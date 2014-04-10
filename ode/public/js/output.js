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
    this.$el.append($.div().attr('id', 'rule-rhs'));
    this._renderName();
    this._renderDescription();
    this._renderRHS();
    return this;
  },

  _renderName: function() {
    this.$('#rule-name').text('@' + this.model.get('name'));
  },

  _renderDescription: function() {
    this.$('#rule-description').text(this.model.get('description'));
  },

  _renderRHS: function() {
    var rhsView = new RHSView({ model: this.model.get('rhs'),
                                el: this.$('#rule-rhs') });
    rhsView.render();
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

var RHS = Backbone.Model.extend({

  initialize: function(attrs, options) {
    var groups = [];
    var pos = 1;
    _.each(options.json.groups, function(g) {
      var group = new CombinationGroup({ id: g.uuid,
                                         position: pos++,
                                         ruleID: this.get('ruleID') },
                                       { json: g });
      groups.push(group);
    }, this);
    this.set('groups', new Backbone.Collection(groups));
  },

});

var Part = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups/' +
      this.get('groupID') + '/slots/' + this.get('slotID') + '/parts';
  },

});

var PartsInventory = Backbone.Collection.extend({

  model: Part,

});

var OutputString = Backbone.Model.extend({

  defaults: { tokens: [] },

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') +
      '/groups/' + this.get('groupID') + '/strings';
    this.on({
      'split': function(splitPoint) {
        this.set('splitPoint', splitPoint);
      },
    }, this);
  },

});

var CombinationGroup = Backbone.Model.extend({

  initialize: function(attrs, options) {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups';
    if (options.json) {
      var outputStrings = _.map(options.json.outputStrings, function(os) {
        return new OutputString({ id: os.uuid,
                                  tokens: os.tokens,
                                  ruleID: this.get('ruleID'),
                                  groupID: this.id });
      }, this);
      this.set('outputStrings', new Backbone.Collection(outputStrings));
      var position = 1;
      var slots = _.map(options.json.partsTable.slots, function(s) {
        var parts = _.map(s.parts, function(p) {
          return new Part({ id: p.uuid,
                            content: p,
                            ruleID: this.get('ruleID'),
                            groupID: this.id,
                            slotID: s.uuid });
        }, this);
        return new Slot({ id: s.uuid,
                          position: position++,
                          parts: new Backbone.Collection(parts),
                          ruleID: this.get('ruleID'),
                          groupID: this.id });
      }, this);
      this.set('partsTable', new PartsTable({
        slots: new Backbone.Collection(slots)
      }));
    }
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

  create: function(id) {
    var json = {
      'outputStrings': [],
      'partsTable': { 'slots': [ { 'parts': [] },
                                 { 'parts': [] } ] },
    };
    return new CombinationGroup({ id: id }, { json: json });
  },

  copy: function() {
    var outputStrings = this.get('outputStrings').map(function(os) {
      return new OutputString({ tokens: os.get('tokens') });
    });
    var position = 1;
    var slots = this.get('partsTable').get('slots').map(function(s) {
      var parts = s.get('parts').map(function(p) {
          return new Part({ content: p.get('content') });
      });
      return new Slot({ position: position++,
                        parts: new Backbone.Collection(parts) });
    });
    var partsTable = new PartsTable({
      slots: new Backbone.Collection(slots)
    });
    return new CombinationGroup({
      id: this.id + 1,
      outputStrings: new Backbone.Collection(outputStrings),
      partsTable: partsTable
    }, {});
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

  hasOptionalSlots: function() {
    return this.get('slots').size() > 2;
  },

});

var Slot = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups/' +
      this.get('groupID') + '/slots';
  },

  add: function(part) {
    var parts = this.get('parts');
    if (!parts.findWhere({ content: part.get('content') })) {
      parts.add(part);
    }
  },

});


// Output: Views

var RHSView = Backbone.View.extend({

  render: function() {
    this._renderGroups();
    return this;
  },

  _renderGroups: function() {
    this.model.get('groups').each(function(group) {
      var groupView = new CombinationGroupView({ model: group });
      this.$el.append(groupView.render().$el);
    }, this);
  },

});

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
    this._renderPlaceholder();
    this.$el.append($.removeButton().css('visibility', 'hidden'));
    return this;
  },

  _renderPlaceholder: function() {
    var placeholder = $.span('placeholder').text('...');
    this.$el.append(placeholder);
    var view = this;
    placeholder.droppable({
      accept: '.part',
      drop: function(e, ui) {
        var tokens = view.model.get('tokens');
        var partTokens = $(ui.helper).text().split(' ');
        _.each(partTokens, function(t) {
          tokens.push(t);
        });
        view.$el.empty();
        view.render();
      },
    });
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
      this.$('.placeholder').remove();
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
    var groupHeader = $.h4('Group ' + this.model.id);
    var small = $.small();
    small.append($.plusButton());
    small.append($.copyButton());
    groupHeader.append(small);
    this.$el.append(groupHeader);
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
    'click .plus-button': function(e) {
      $(e.currentTarget).remove();
      this.$('.copy-button').remove();
      var emptyGroup = this.model.create(this.model.id + 1);
      var groupView = new CombinationGroupView({ model: emptyGroup });
      this.$el.parent().append(groupView.render().$el);
    },
    'click .copy-button': function(e) {
      this.$('.plus-button').remove();
      $(e.currentTarget).remove();
      var groupCopy = this.model.copy();
      var groupView = new CombinationGroupView({ model: groupCopy });
      groupView.render().$el.insertAfter(this.$el);
    },
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
      var placeholder = $(e.currentTarget).prev('.placeholder');
      var tokens = placeholder.text().split(' ');
      var outputString = new OutputString({ tokens: tokens });
      this.model.addOutputString(outputString);
      var outputStringView = new OutputStringView({ model: outputString });
      outputStringView.render().$el.insertBefore(placeholder);
      this._resetPlaceholder();
    },
  },

  _resetPlaceholder: function() {
    var placeholder = this.$el.children('.placeholder');
    placeholder.text('Add more content ...');
    placeholder.next('button').css('visibility', 'hidden');
  },

});

var PartsTableView = Backbone.View.extend({

  className: 'parts-table',

  initialize: function() {
    this.model.get('slots').on({
      'delete': function(slot) {
        this.model.get('slots').remove(slot);
        var slotsToUpdate = this.model.get('slots').filter(function(s) {
          return s.get('position') > slot.get('position');
        });
        _.each(slotsToUpdate, function(s) {
          s.set('position', s.get('position')-1);
        });
      },
    }, this);
  },

  render: function() {
    this._renderSlots();
    this._renderControls();
    return this;
  },

  _renderSlots: function() {
    var slots = $.div('slots');
    this.model.get('slots').each(function(slot) {
      slots.append(new SlotView({ model: slot }).render().$el);
    });
    this.$el.append(slots);
  },

  _renderControls: function() {
    var controls = $.div('controls');
    controls.append($.infoButton('Add slot'));
    this.$('.slots').append(controls);
  },

  events: {
    'click .btn': '_addSlot',
    'mouseenter h5': function(e) {
      if (this.model.hasOptionalSlots()) {
        $(e.currentTarget).find('.remove-button')
          .css('visibility', 'visible');
      }
    },
    'mouseleave h5': function(e) {
      if (this.model.hasOptionalSlots()) {
        $(e.currentTarget).find('.remove-button')
          .css('visibility', 'hidden');
      }
    },
  },

  _addSlot: function() {
    this.$('.controls').remove();
    var position = this.model.get('slots').size() + 1;
    var slot = new Slot({ position: position,
                          parts: new Backbone.Collection([]) });
    this.model.get('slots').add(slot);
    var slotView = new SlotView({ model: slot });
    this.$('.slots').append(slotView.render().$el);
    this._renderControls();
  },

});

var SlotView = Backbone.View.extend({

  className: 'slot',

  initialize: function() {
    this.model.get('parts').on({
      'add remove': function() {
        this.$el.empty();
        this.render();
      },
    }, this);
    this.model.on({
      'change:position': this._updateHeader,
    }, this);
  },

  _updateHeader: function() {
    this.$('h5').text('Slot ' + this.model.get('position'));
  },

  render: function() {
    this._renderHeader();
    this._renderLine();
    this._renderParts();
    this._renderPlaceholder();
    return this;
  },

  _renderHeader: function() {
    var slotHeader = $.h5('Slot ' + this.model.get('position'));
    slotHeader.append($.removeButton().css('visibility', 'hidden'));
    this.$el.append(slotHeader);
  },

  _renderLine: function() {
    this.$el.append($.hr());
  },

  _renderParts: function() {
    this.model.get('parts').each(function(p) {
      var part = $.div('part').text(p.get("content"));
      part.append($.removeButton().css('visibility', 'hidden'));
      this.$el.append(part);
    }, this);
  },

  _renderPlaceholder: function() {
    var placeholder = $.div('placeholder').text('...');
    this.$el.append(placeholder);
    placeholder.makeEditable();
    var view = this;
    placeholder.droppable({
      accept: '.part',
      drop: function(e, ui) {
        var part = new Part({ content: $(ui.helper).text() });
        view.model.add(part);
      },
    });
  },

  events: {
    'click h5 > .remove-button': function() {
      this.model.trigger('delete', this.model);
      this.remove();
    },
    'mouseenter .part': function(e) {
      $(e.currentTarget).find('.remove-button').css('visibility', 'visible');
    },
    'mouseleave .part': function(e) {
      $(e.currentTarget).find('.remove-button').css('visibility', 'hidden');
    },
    'click .part > .remove-button': function(e) {
      var parts = this.model.get('parts');
      var part = parts.findWhere({
        content: $(e.currentTarget).parent().text()
      });
      parts.remove(part);
    },
    'dblclick .part': '_renderEditControls',
    'click .placeholder': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() === '...') {
        inputField.empty();
      }
    },
    'keydown .placeholder': function(e) {
      if (e.which === 13) {
        e.preventDefault();
      }
    },
    'keyup .placeholder': function(e) {
      if (e.which === 13) {
        var part = new Part({ content: $(e.currentTarget).text() });
        this.model.add(part);
      }
    },
    'keyup input': function(e) {
      if (e.which === 13) {
        var inputField = $(e.currentTarget);
        var newContent = inputField.val();
        var hiddenPart = inputField.prev('.part');
        var oldContent = hiddenPart.text();
        if (newContent && !(newContent === oldContent)) {
          var part = this.model.get('parts')
            .findWhere({ content: hiddenPart.text() });
          part.set('content', newContent);
          hiddenPart.text(newContent);
          hiddenPart.append($.removeButton().css('visibility', 'hidden'));
        }
        hiddenPart.show();
        inputField.remove();
      }
    },
  },

  _renderEditControls: function(e) {
    if (!this.$('input').exists()) {
      var part = $(e.currentTarget);
      var inputField = $.textInput().val(part.text());
      part.hide();
      inputField.insertAfter(part);
      inputField.focus();
    }
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
  var rhsJSON = $('#rule-rhs').data('json');

  var rhs = new RHS({ ruleID: name }, { json: rhsJSON });

  var rule = new Rule({
    id: name,
    name: name,
    description: description,
    rhs: rhs,
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
