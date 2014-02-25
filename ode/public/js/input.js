// Features

var Feature = Backbone.Model.extend({});

var FeatureList = Backbone.Collection.extend({});

var FeatureItemView = Backbone.View.extend({

  className: 'feature-item draggable',

  attributes: function() {
    return {
      id: this.model.id,
      'data-name': this.model.get('name'),
      'data-description': this.model.get('description'),
      'data-type': this.model.get('type'),
      'data-targets': '[' + this.model.get('targets') + ']',
    }
  },

  initialize: function() {
    this.model.on({
      'hide': function() { this.$el.hide(); },
      'show': function() { this.$el.show(); },
    }, this);
  },

  render: function() {
    this.$el.text(this.model.get('name'));
    this.$el.draggable({
      helper: 'clone',
      revert: 'invalid',
    });
    return this;
  }

});

var FeatureListView = Backbone.View.extend({

  render: function() {
    this.$el.empty();
    this.collection.forEach(this._addFeatureItem, this);
    return this;
  },

  _addFeatureItem: function(featureItem) {
    var featureItemView = new FeatureItemView({ model: featureItem });
    this.$el.append(featureItemView.render().$el);
  },

  events: {
    'mouseenter .feature-item': '_highlight',
    'mouseleave .feature-item': '_unhighlight',
  },

  _highlight: function(e) {
    $(e.currentTarget).addClass('highlighted');
  },

  _unhighlight: function(e) {
    $(e.currentTarget).removeClass('highlighted');
  },

  filterItems: function(input) {
    this.collection.each(function(i) {
      if (!$.matches(i.get('name').toLowerCase(), input)) {
        i.trigger('hide');
      } else {
        i.trigger('show');
      }
    });
  },

});


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
      'change': this.render,
      'change:name': this._updateURL,
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
    this._renderName();
    this._renderDescription();
    return this;
  },

  _renderName: function() {
    var nameTemplate = _.template('<h3 id="rule-name">@<%= name %></h3>');
    var node = $(nameTemplate({ name: this.model.get('name') }));
    this.$el.append(node);
  },

  _renderDescription: function() {
    var descriptionTemplate = _.template(
      '<p id="rule-description"><%= description %></p>');
    var node = $(descriptionTemplate({
      description: this.model.get('description')
    }));
    this.$el.append(node);
  },

  _updateURL: function() {
    window.location.replace(this.model.url() + '/input');
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
        var updateFunction = 'update' + modelField.charAt(0).toUpperCase() +
          modelField.slice(1);
        view.model[updateFunction](inputField.val());
      } else {
        view.render();
      }
    };
  },

});


var Pair = Backbone.Model.extend({}); // A pair consists of an
                                      // attribute/feature (string)
                                      // and a value (string or AVM);
                                      // it can optionally be
                                      // restricted in the types of
                                      // values it allows

var AVM = Backbone.Collection.extend({}); // An AVM is a list of
                                          // attribute-value pairs;
                                          // it can optionally by
                                          // restricted in the types
                                          // of features it allows;
                                          // each rule is associated
                                          // with a single AVM
                                          // representing its LHS

var PairView = Backbone.View.extend({

  className: 'pair',

  render: function() {
    this.$el.text(this.model.get('feature').get('name'));
    return this;
  },

});

var AVMView = Backbone.View.extend({

  className: 'avm',

  initialize: function() {
    this.collection.on({
      'add': this.render,
    }, this);
  },

  render: function() {
    this.$el.empty();
    this._renderBracket('left');
    this._renderContent();
    this._renderBracket('right');
    if (this.collection.isEmpty()) {
      this.$('.bracket').height(this.$('.placeholder').height());
    } else {
      this.$('.bracket').height(this.$('.content').height());
    }
    return this;
  },

  _renderBracket: function(type) {
    this.$el.append($.div('bracket bracket-' + type));
  },

  _renderContent: function() {
    var content = $.div('content');
    // Render pairs:
    this.collection.forEach(function(pair) {
      var pairView = new PairView({ model: pair });
      content.append(pairView.render().$el);
    });
    // Render placeholder:
    var view = this;
    var placeholder = $.placeholder('Drop feature here ...')
      .droppable({
        accept: '.feature-item',
        drop: function(e, ui) {
          var item = $(ui.helper);
          var feature = new Feature({
            name: item.data('name'),
            type: item.data('type'),
            targets: item.dataToArray('targets'),
          });
          view.collection.add(new Pair({ feature: feature }));
        },
      });
    content.append(placeholder);
    this.$el.append(content);
  },

});                                     // This type of view minimally
                                        // consists of an opening
                                        // bracket, a (content block
                                        // with a) single placeholder
                                        // for new *attributes*, and a
                                        // closing bracket;
                                        // it should eventually be
                                        // incorporated into RuleView
                                        // as a subview;
                                        // dropping a feature on the
                                        // placeholder will trigger
                                        // generation of a new pair;
                                        // in case of atomic features,
                                        // the list of possible
                                        // *values* for this pair will
                                        // be set according to the
                                        // list of targets for the
                                        // newly added feature; in
                                        // case of complex features,
                                        // the target list of the
                                        // newly added feature
                                        // determines the list of
                                        // features permitted in the
                                        // value AVM



// Application


$(document).ready(function() {

  // "Features" sidebar

  var featureItems = $('.feature-item');

  var featureList = new FeatureList(
    _.map(featureItems, function(i) {
      var item = $(i);
      var name = item.data('name');
      return new Feature({
        id: name,
        name: name,
        type: item.data('type'),
        description: item.data('description'),
        targets: item.dataToArray('targets'),
      });
    }),
    { comparator: 'name' }
  );

  var featureListView = new FeatureListView({
    el: '#feature-list',
    collection: featureList,
  });
  featureListView.render();

  $('#feature-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    featureListView.filterItems(currentInput);
  });

  // Input Builder

  var name = $('#rule-name').text();
  var description = $('#rule-description').text();
  var rule = new Rule({ id: name, name: name, description: description });

  var ruleView = new RuleView({
    model: rule,
    el: '#interaction-block',
  });
  ruleView.render();

  var avm = new AVM([]);
  var avmView = new AVMView({ collection: avm });
  ruleView.$el.append(avmView.render().$el);

});
