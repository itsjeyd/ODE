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

var AVM = Backbone.Collection.extend({

  initialize: function(models, options) {
    this.accept = options.accept;
  },

}); // Each rule is associated
    // with a single AVM
    // representing its LHS

var PairView = Backbone.View.extend({

  className: 'pair',

  initialize: function(options) {
    this.parentView = options.parentView;
  },

  render: function() {
    var attribute = this.model.get('feature');
    this.$el.append(
      $.span('attribute').text(attribute.get('name')));
    if (attribute.get('type') === 'complex') {
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

});

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

}); // This view should eventually
    // be incorporated into
    // RuleView as a subview



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

  var avm = new AVM([], { accept: '.feature-item' });
  var avmView = new AVMView({ collection: avm });
  ruleView.$el.append(avmView.render().$el);

});
