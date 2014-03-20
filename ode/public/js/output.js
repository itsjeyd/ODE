// Rules

var Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
  },

  _update: function(field, attrs, success) {
    this.save(attrs,
              { url: this.url() + '/' + field,
                wait: true,
                success: success,
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:' + field, response.message);
                },
              });
  },

  updateName: function(newName) {
    var success = function(model, response, options) {
      model.id = newName;
    };
    this._update('name', { name: newName }, success);
  },

  updateDescription: function(newDescription) {
    var success = function(model, response, options) {};
    this._update('description', { description: newDescription }, success);
  },

});

var RuleView = Backbone.View.extend({

  initialize: function() {
    this.model.on({
      'change:name': function() {
        this._updateURL();
      },
      'change:description': function() {
        this._removeEditControls('description');
        this._renderDescription();
      },
      'update-error:name': function(msg) {
        this._renderAlert('button.name', msg);
      },
      'update-error:description': function(msg) {
        this._renderAlert('button.description', msg);
      },
    }, this);
  },

  render: function() {
    this.$el.empty();
    this.$el.append($.h3('').attr('id', 'rule-name'));
    this.$el.append($.p('').attr('id', 'rule-description'));
    this._renderName();
    this._renderDescription();
    return this;
  },

  _renderName: function() {
    this.$('#rule-name').text('@' + this.model.get('name'));
  },

  _renderDescription: function() {
    this.$('#rule-description').text(this.model.get('description'));
  },

  _updateURL: function() {
    window.location.replace(this.model.url() + '/output');
  },

  _renderAlert: function(button, msg) {
    var updateButton = this.$(button);
    updateButton.next('.alert-msg').remove();
    $.alertMsg(msg).insertAfter(updateButton);
  },

  events: {
    'dblclick #rule-name': function(e) {
      this._renderEditControls('name')(e)
    },
    'click button.name': function() {
      this._saveEdits('name')(this)
    },
    'dblclick #rule-description': function(e) {
      this._renderEditControls('description')(e)
    },
    'click button.description': function() {
      this._saveEdits('description')(this)
    },
  },

  _renderEditControls: function(modelField) {
    return function(e) {
      var fieldToEdit = $(e.currentTarget);
      var currentValue = fieldToEdit.text();
      if (modelField === 'name') {
        currentValue = currentValue.substring(1);
      }
      var inputField = $.textInput().addClass(modelField)
        .val(currentValue);
      var okButton = $.okButton().addClass(modelField);
      fieldToEdit.hide();
      inputField.insertAfter(fieldToEdit);
      okButton.insertAfter(inputField);
      inputField.focus();
    };
  },

  _saveEdits: function(modelField) {
    return function(view) {
      var inputField = view.$('input.' + modelField);
      if (!inputField.isEmpty() &&
          inputField.val() !== view.model.get(modelField)) {
        var updateFunction = 'update' + $.cap(modelField);
        view.model[updateFunction](inputField.val());
      } else {
        view._removeEditControls(modelField);
        var renderFunction = '_render' + $.cap(modelField);
        view[renderFunction]();
      }
    };
  },

  _removeEditControls: function(modelField) {
    this.$('.' + modelField).remove();
    this.$('.alert-msg').remove();
    this.$('#rule-' + modelField).show();
  },

});


// Output: Models

var Part = Backbone.Model.extend({});

var PartsInventory = Backbone.Collection.extend({

  model: Part,

});

var OutputString = Backbone.Model.extend({

  defaults: { tokens: [] },

  initialize: function() {
    this.on({
      'split': function(splitPoint) {
        this.set('splitPoint', splitPoint);
      },
    }, this);
  },

});

var CombinationGroup = Backbone.Model.extend({

  initialize: function(attrs, options) {
    var outputStrings = _.map(options.json.outputStrings, function(os) {
      return new OutputString({ tokens: os.tokens });
    });
    this.set('outputStrings', new Backbone.Collection(outputStrings));
    var slotID = 1;
    var slots = _.map(options.json.partsTable.slots, function(s) {
      var parts = _.map(s.parts, function(p) {
        return new Part({ content: p });
      });
      return new Slot({ id: slotID++,
                        parts: new Backbone.Collection(parts) });
    });
    alert(JSON.stringify(this));
    this.set('partsTable', new PartsTable({
      slots: new Backbone.Collection(slots)
    }));
    this.get('outputStrings').on({
      'change:splitPoint': function(model) {
        this.get('outputStrings').remove(model);
        this.get('partsTable').add(model);
      },
    }, this);
  },

  addOutputString: function(outputString) {
    this.get('outputStrings').push(outputString);
  },

});

var PartsTable = Backbone.Model.extend({

  add: function(outputString) {
    var tokens = outputString.get('tokens');
    var splitPoint = outputString.get('splitPoint');
    var leftPart = new Part({
      content: tokens.slice(0, splitPoint).join(' '),
    });
    var rightPart = new Part({
      content: tokens.slice(splitPoint).join(' '),
    });
    this.get('slots').at(0).add(leftPart);
    this.get('slots').at(1).add(rightPart);
  },

});

var Slot = Backbone.Model.extend({

  add: function(part) {
    this.get('parts').add(part);
  },

});


// Output: Views

var OutputStringView = Backbone.View.extend({

  className: 'output-string',

  initialize: function() {
    this.model.on({
      'remove': this.remove,
    }, this);
  },

  render: function() {
    var sepID = 1;
    var tokens = this.model.get('tokens');
    _.each(_.initial(tokens), function(t) {
      this.$el.append($.span('token').text(t));
      this.$el.append($.span('sep').data('ID', sepID++));
    }, this);
    this.$el.append($.span('token').text(_.last(tokens)));
    this.$el.append($.removeButton().css('visibility', 'hidden'));
    return this;
  },

  events: {
    'mouseenter': function() {
      this.$('.remove-button').css('visibility', 'visible');
    },
    'mouseleave': function() {
      this.$('.remove-button').css('visibility', 'hidden');
    },
    'click .sep': function(e) {
      this.model.trigger('split', $(e.currentTarget).data('ID'));
    },
    'click .remove-button': function() {
      this.model.destroy();
      this.remove();
    },
    'dblclick': '_renderEditControls',
    'click button': function() {
      this._updateModel();
      this.$el.empty();
      this.render();
    },
  },

  _renderEditControls: function(e) {
    if (!this.$('input').exists()) {
      var tokens = _.map(this.$('.token'), function(t) {
        return $(t).text();
      });
      var inputField = $.textInput().val(tokens.join(' '));
      var okButton = $.okButton();
      this.$('.token').remove();
      this.$('.sep').remove();
      this.$('.remove-button').remove();
      this.$el.append(inputField);
      this.$el.append(okButton);
      inputField.focus();
    }
  },

  _updateModel: function() {
    var inputField = this.$('input');
    if (!inputField.isEmpty()) {
      this.model.set('tokens', inputField.val().split(' '));
    }
  },

});

var CombinationGroupView = Backbone.View.extend({

  className: 'combination-group',

  initialize: function() {
    this.model.get('outputStrings').on({
      'remove': function(outputString) {
        outputString.trigger('remove');
      },
    }, this);
  },

  render: function() {
    this._renderHeader();
    this._renderPartsTable();
    this._renderOutputStrings();
    this._renderPlaceholder();
    return this;
  },

  _renderHeader: function() {
    this.$el.append($.h4('Group ' + this.model.id));
  },

  _renderPartsTable: function() {
    this.$el.append(new PartsTableView({
      model: this.model.get('partsTable')
    }).render().$el);
  },

  _renderOutputStrings: function() {
    var list = $.div('output-strings');
    this.model.get('outputStrings').each(function(os) {
      list.append(new OutputStringView({ model: os }).render().$el);
    });
    this.$el.append(list);
  },

  _renderPlaceholder: function() {
    var placeholder = $.div('placeholder').text('Add more content ...');
    this.$el.append(placeholder);
    placeholder.makeEditable();
    var view = this;
    placeholder.droppable({
      accept: '.part',
      drop: function(e, ui) {
        var tokens = $(ui.helper).text().split(' ');
        var outputString = new OutputString({ tokens: tokens });
        view.model.addOutputString(outputString);
        var outputStringView = new OutputStringView({ model: outputString });
        outputStringView.render().$el.insertBefore(placeholder);
        view._resetPlaceholder();
      },
    });
    this.$el.append($.addButton().css('visibility', 'hidden'));
  },

  events: {
    'click .placeholder': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() === 'Add more content ...') {
        inputField.empty();
      }
    },
    'keyup .placeholder': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text()) {
        inputField.next('button').css('visibility', 'visible');
      } else {
        inputField.next('button').css('visibility', 'hidden');
      }
    },
    'click .placeholder + button': function(e) {
      var placeholder = this.$('.placeholder');
      var tokens = placeholder.text().split(' ');
      var outputString = new OutputString({ tokens: tokens });
      this.model.addOutputString(outputString);
      var outputStringView = new OutputStringView({ model: outputString });
      outputStringView.render().$el.insertBefore(placeholder);
      this._resetPlaceholder();
    },
  },

  _resetPlaceholder: function() {
    var placeholder = this.$('.placeholder');
    placeholder.text('Add more content ...');
    placeholder.next('button').css('visibility', 'hidden');
  },

});

var PartsTableView = Backbone.View.extend({

  className: 'parts-table',

  render: function() {
    this._renderSlots();
    return this;
  },

  _renderSlots: function() {
    var slots = $.div('slots');
    this.model.get('slots').each(function(slot) {
      slots.append(new SlotView({ model: slot }).render().$el);
    });
    this.$el.append(slots);
  },

});

var SlotView = Backbone.View.extend({

  className: 'slot',

  initialize: function() {
    this.model.get('parts').on({
      'add': function() {
        this.$el.empty();
        this.render();
      },
    }, this);
  },

  render: function() {
    this._renderHeader();
    this._renderLine();
    this._renderParts();
    return this;
  },

  _renderHeader: function() {
    this.$el.append($.h5('Slot ' + this.model.id));
  },

  _renderLine: function() {
    this.$el.append($.hr());
  },

  _renderParts: function() {
    this.model.get('parts').each(function(part) {
      this.$el.append($.div('part').text(part.get("content")));
    }, this);
  },

});

var PartsInventoryView = Backbone.View.extend({

  render: function() {
    this.$el.empty();
    this.collection.each(this._renderPart, this);
    return this;
  },

  _renderPart: function(part) {
    var partItemView = new PartItemView({ model: part });
    this.$el.append(partItemView.render().$el);
  },

  filterItems: function(input) {
    this.collection.each(function(i) {
      if (!$.matches(i.get('content').toLowerCase(), input)) {
        i.trigger('hide');
      } else {
        i.trigger('show');
      }
    });
  },

});

var PartItemView = Backbone.View.extend({

  className: 'part draggable',

  initialize: function() {
    this.model.on({
      'hide': function() { this.$el.hide(); },
      'show': function() { this.$el.show(); },
    }, this);
  },

  render: function() {
    this.$el.text(this.model.get('content'));
    this.$el.draggable({
      helper: 'clone',
      revert: 'invalid',
    });
    return this;
  }

});


// Application

$(document).ready(function() {

  // "Parts" sidebar

  var parts = $('.part');

  var partsInventory = new PartsInventory(
    _.map(parts, function(p) {
      return new Part({ content: $(p).data('content') });
    }),
    { comparator: 'content' }
  );

  var partsInventoryView = new PartsInventoryView({
    el: '#parts-list',
    collection: partsInventory,
  });
  partsInventoryView.render();

  $('#parts-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    partsInventoryView.filterItems(currentInput);
  });

  // Output Builder

  var name = $('#rule-name').text();
  var description = $('#rule-description').text();

  var rule = new Rule({
    id: name,
    name: name,
    description: description,
  });

  var ruleView = new RuleView({
    model: rule,
    el: '#interaction-block',
  });
  ruleView.render();

  var cgJSON = { outputStrings: [ { tokens: ['a', 'b', 'c'] },
                                  { tokens: ['d', 'e', 'f'] }, ],
                 partsTable: { slots: [ { parts:
                                          ['parts', 'in', 'slot', '1'] },
                                        { parts:
                                          ['parts', 'in', 'slot', '2'] },
                                      ], }, }


  var cg = new CombinationGroup({ id: 1 }, { json: cgJSON });

  var cgView = new CombinationGroupView({ model: cg });
  $('#rule-rhs').append(cgView.render().$el);

  // Header

  var navbarButton = $('<li>');
  var navbarLink = $('<a>').attr('href', rule.url() + '/input')
    .text('InputBuilder');
  navbarButton.append(navbarLink);
  var browseButton = $('.navbar-right').children('li').first();
  navbarButton.insertBefore(browseButton);

});
