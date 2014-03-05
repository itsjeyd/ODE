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
    this.get('lhs').on({
      'add': this._addFeature,
    }, this);
  },

  _addFeature: function(pair) {
    var featureName = pair.get('feature').get('name');
    var success = function(model, response, options) {};
    this._update('input', { lhs: this.get('lhs'),
                            featureName: featureName, action: 'ADD' },
                 success);
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
        this._renderName();
      },
      'change:description': this._renderDescription,
      'update-error:name': function(msg) {
        this._renderAlert('button.name', msg);
      },
      'update-error:description': function(msg) {
        this._renderAlert('button.description', msg);
      },
    }, this);
  },

  render: function() {
    this.$el.append($.h3('').attr('id', 'rule-name'));
    this.$el.append($.p('').attr('id', 'rule-description'));
    this._renderName();
    this._renderDescription();
    this._renderLHS();
    return this;
  },

  _renderName: function() {
    this.$('#rule-name').text('@' + this.model.get('name'));
  },

  _renderDescription: function() {
    this.$('#rule-description').text(this.model.get('description'));
  },

  _renderLHS: function() {
    var lhsView = new AVMView({ collection: this.model.get('lhs') });
    this.$el.append(lhsView.render().$el);
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


// AVMs: models

var AVM = Backbone.Collection.extend({

  initialize: function(models, options) {
    this.accept = options.accept;
  },

});


var Pair = Backbone.Model.extend({

  initialize: function(options) {
    if (options.feature.get('type') === 'complex') {
      var accept = '#' + options.feature.get('targets').join(', #');
      this.set('value', new AVM([], { accept: accept }));
    } else {
      this.set('value', 'underspecified');
    }
  },

});


// AVMs: views

var AVMView = Backbone.View.extend({

  className: 'avm',

  initialize: function() {
    this.collection.on({
      'add': function(newPair) {
        this._renderPair(newPair);
        this.trigger('update');
      },
    }, this);
    this.on({
      'update': function() {
        this._adjustBracketHeight();
        this.trigger('re-rendered');
      },
      'remove:pair': function(pair) {
        this.collection.remove(pair);
        this.trigger('update');
      },
    }, this);
  },

  _renderPair: function(pair) {
    var pairView = new PairView({ model: pair, parentView: this });
    pairView.render();
    pairView.$el
      .insertBefore(this.$el.children('.content').children('.placeholder'));
  },

  render: function() {
    this.$el.empty();
    this._renderBracket('left');
    this._renderContent();
    this._renderBracket('right');
    this._adjustBracketHeight();
    this.$el.append($.emptyButton().css('visibility', 'hidden'));
    return this;
  },

  _renderBracket: function(type) {
    this.$el.append($.div('bracket bracket-' + type));
  },

  _renderContent: function() {
    var content = $.div('content');
    var view = this;
    var placeholder = $.placeholder('Drop feature here ...')
      .droppable({
        accept: view.collection.accept,
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

  _adjustBracketHeight: function() {
    var content = this.$el.children('.content');
    var height = content.children('.placeholder').height();
    if (!this.collection.isEmpty()) {
      height = content.height();
    }
    this.$el.children('.bracket').height(height);
  },

  events: {
    'mouseenter': '_showEmptyButton',
    'mouseleave': '_hideEmptyButton',
    'click .empty-button': function() {
      this.collection.reset();
      this.render();
      this.trigger('re-rendered');
    },
  },

  _showEmptyButton: function() {
    this.$('.empty-button').css('visibility', 'visible');
  },

  _hideEmptyButton: function() {
    this.$('.empty-button').css('visibility', 'hidden');
  },

});

var PairView = Backbone.View.extend({

  className: 'pair',

  initialize: function(options) {
    this.parentView = options.parentView;
  },

  render: function() {
    var feature = this.model.get('feature');
    var name = feature.get('name');
    this.$el.append($.span('attribute').text(name));
    this.$('.attribute')
      .append($.removeButton(name).css('visibility', 'hidden'));
    if (feature.get('type') === 'complex') {
      this._renderSubstructure();
    } else {
      this._renderValue();
    }
    return this;
  },

  _renderSubstructure: function() {
    var avmView = new AVMView({ collection: this.model.get('value') });
    var value = $.div('value');
    value.append(avmView.render().$el);
    this.$el.append(value);
    this.listenTo(
      avmView, 're-rendered',
      function() { this.parentView.trigger('update') });
  },

  _renderValue: function() {
    var selectMenu = $.selectMenu();
    var options = ['underspecified']
      .concat(this.model.get('feature').get('targets'));
    _.each(options, function(o) {
      selectMenu.append($.option(o));
    });
    selectMenu.val(this.model.get('value'));
    var value = $.span('value');
    value.append(selectMenu);
    this.$el.append(value);
  },

  events: {
    'mouseenter .attribute': '_showRemoveButton',
    'mouseleave .attribute': '_hideRemoveButton',
    'click .remove-button': function() {
      this.remove();
      this.parentView.trigger('remove:pair', this.model);
    },
  },

  _showRemoveButton: function() {
    this.$('.remove-button').css('visibility', 'visible');
  },

  _hideRemoveButton: function() {
    this.$('.remove-button').css('visibility', 'hidden');
  },

});



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

  var avm = new AVM([], { accept: '.feature-item' });

  var rule = new Rule({
    id: name,
    name: name,
    description: description,
    lhs: avm,
  });

  var ruleView = new RuleView({
    model: rule,
    el: '#interaction-block',
  });
  ruleView.render();

});
