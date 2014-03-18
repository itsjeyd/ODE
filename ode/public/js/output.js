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

var OutputString = Backbone.Model.extend({

  defaults: { tokens: [] },

});

var CombinationGroup = Backbone.Model.extend({

  initialize: function(attrs, options) {
    var outputStrings = _.map(options.json.outputStrings, function(os) {
      return new OutputString({ tokens: os.tokens });
    });
    this.set('outputStrings', outputStrings);
    var slots = _.map(options.json.partsTable.slots, function(s) {
      var parts = _.map(s.parts, function(p) {
        return new Part({ content: p });
      });
      return new Slot({ parts: parts });
    });
    this.set('partsTable', new PartsTable({ slots: slots }));
    alert(JSON.stringify(this));
  },

});

var PartsTable = Backbone.Model.extend({});

var Slot = Backbone.Model.extend({});


// Output: Views

var OutputStringView = Backbone.View.extend({

  className: 'output-string',

  render: function() {
    var tokens = this.model.get('tokens');
    _.each(_.initial(tokens), function(t) {
      this.$el.append($.span('token').text(t));
      this.$el.append($.span('sep'));
    }, this);
    this.$el.append($.span('token').text(_.last(tokens)));
    return this;
  },

});

var CombinationGroupView = Backbone.View.extend({

  className: 'combination-group',

  render: function() {
    this._renderHeader();
    this._renderPartsTable();
    this._renderOutputStrings();
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
    _.each(this.model.get('outputStrings'), function(os) {
      list.append(new OutputStringView({ model: os }).render().$el);
    });
    this.$el.append(list);
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
    _.each(this.model.get('slots'), function(slot) {
      slots.append(new SlotView({ model: slot }).render().$el);
    });
    this.$el.append(slots);
  },

});

var SlotView = Backbone.View.extend({

  className: 'slot',

  render: function() {
    this._renderParts();
    return this;
  },

  _renderParts: function() {
    _.each(this.model.get('parts'), function(part) {
      this.$el.append($.div('part').text(part.get("content")));
    }, this);
  },

});


// Application

$(document).ready(function() {

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
