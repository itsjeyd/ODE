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
    lhsView.trigger('inserted');
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

// ...


// AVMs: views

// ...




var AVM = Backbone.Collection.extend({

  initialize: function(models, options) {
    this.ruleName = options.ruleName;
    this.uuid = options.json.uuid;
    this.accept = options.accept || '.feature-item';
    var avm = this;
    _.each(options.json.pairs, (function(pair) {
      avm.add(new Pair(null, { parent: avm,
                               attribute: pair.attribute,
                               value: pair.value }));
    }));
  },

  empty: function() {
    this.each(function(pair) {
      pair.remove();
    });
  },

});

var Pair = Backbone.Model.extend({

  initialize: function(attrs, options) {
    this.parent = options.parent;
    this.set('attribute', new Feature({
      name: options.attribute.name,
      type: options.attribute.type,
      targets: options.attribute.targets
    }));
    if (options.value) {
      this._setValue(options.value);
    }
  },

  _setValue: function(value) {
    if (typeof value === 'string') {
      this.set('value', value);
    } else {
      var accept = '#' + this.get('attribute').get('targets').join(', #');
      this.set('value', new AVM(null, { ruleName: this.parent.ruleName,
                                        accept: accept,
                                        json: value }));
    }
  },

  url: function() {
    return '/rules/' + this.parent.ruleName + '/input';
  },

  create: function() {
    var pair = this;
    var success = function(model, response, options) {
      pair._setValue(model.get('value'));
      pair.parent.add(pair);
    };
    this.save({ uuid: this.parent.uuid },
              { url: this.url(),
                wait: true,
                success: success });
  },

  updateValue: function(newValue) {
    this.save({ uuid: this.parent.uuid,
                newValue: newValue },
              { url: this.url(),
                type: 'PUT',
                wait: true });
  },

  remove: function() {
    this.save({ uuid: this.parent.uuid, },
              { url: this.url(),
                type: 'DELETE',
                wait: true,
                success: function(model, response, options) {
                  model.trigger('remove');
                },
              });
  },

});

var AVMView = Backbone.View.extend({

  className: 'avm',

  attributes: function() {
    return {
      id: this.collection.uuid,
    }
  },

  initialize: function(options) {
    this.parentView = options.parentView;
    if (this.parentView) {
      this.parentView.on({
        'inserted': function() {
          this.trigger('inserted');
        },
      }, this);
    }
    this.on({
      'inserted': function() {
        this.trigger('rendered');
        this._removeBrackets();
        this._renderBrackets();
      },
      'update': function() {
        this._removeBrackets();
        this._renderBrackets();
        this.trigger('re-rendered');
      },
    }, this);
    this.collection.on({
      'add': function(pair) {
        this._renderPair(pair);
        this.trigger('update');
      },
    }, this);
  },

  render: function() {
    this._renderContent();
    this._renderEmptyButton();
    return this;
  },

  _renderBrackets: function() {
    var leftBracket = this._makeBracket('left');
    var rightBracket = this._makeBracket('right');
    var h = this.$el.children('.content').height();
    leftBracket.insertBefore(this.$el.children('.content'));
    rightBracket.insertAfter(this.$el.children('.content'));
    leftBracket.height(h);
    rightBracket.height(h);
  },

  _makeBracket: function(type) {
    return $.div('bracket bracket-' + type);
  },

  _removeBrackets: function() {
    this.$el.children('.bracket').remove();
  },

  _renderPair: function(pair) {
    var placeholder = this.$el.children('.content').children('.placeholder');
    this._makePair(pair).insertBefore(placeholder);
    this.trigger('rendered');
  },

  _renderContent: function() {
    var content = $.div('content');
    this.collection.each(function(pair) {
      content.append(this._makePair(pair));
    }, this);
    content.append(this._makePlaceholder());
    this.$el.append(content);
  },

  _makePair: function(pair) {
    return new PairView({ model: pair, parentView: this }).render().$el;
  },

  _makePlaceholder: function() {
    var view = this;
    return $.placeholder('Drop feature here ...')
      .droppable({
        accept: this.collection.accept,
        drop: function(e, ui) {
          var item = $(ui.helper);
          var parent = view.collection;
          var attribute = {
            name: item.data('name'),
            type: item.data('type'),
            targets: item.dataToArray('targets'),
          };
          new Pair(null, { parent: parent, attribute: attribute }).create();
        },
      });
  },

  _renderEmptyButton: function() {
    this.$el.append($.emptyButton().css('visibility', 'hidden'));
  },

  events: {
    'mouseenter': '_showEmptyButton',
    'mouseleave': '_hideEmptyButton',
    'click .empty-button': '_empty',
  },

  _showEmptyButton: function() {
    this.$('.empty-button').css('visibility', 'visible');
  },

  _hideEmptyButton: function() {
    this.$('.empty-button').css('visibility', 'hidden');
  },

  _empty: function(e) {
    e.stopPropagation();
    this.collection.empty();
  }

});

var PairView = Backbone.View.extend({

  className: 'pair',

  initialize: function(options) {
    this.parentView = options.parentView;
    this.parentView.on({
      'rendered': function() {
        this.trigger('inserted');
      },
    }, this);
    this.model.on({
      'remove': function() {
        this.remove();
        this.parentView.trigger('update');
      }
    }, this);
  },

  render: function() {
    this._renderAttr();
    this._renderVal();
    return this;
  },

  _renderAttr: function() {
    var name = this.model.get('attribute').get('name');
    var attr = $.span('attribute').text(name);
    attr.append($.removeButton(name).css('visibility', 'hidden'));
    this.$el.append(attr);
  },

  _renderVal: function() {
    if (this.model.get('attribute').get('type') === 'complex') {
      this._renderSubstructure();
    } else {
      this._renderValue();
    }
  },

  _renderSubstructure: function() {
    var value = $.div('value');
    var avmView = new AVMView({ collection: this.model.get('value'),
                                parentView: this });
    value.append(avmView.render().$el);
    this.$el.append(value);
    this.listenTo(avmView,
                  're-rendered',
                  function() { this.parentView.trigger('update') });
  },

  _renderValue: function() {
    var value = $.span('value');
    var selectMenu = $.selectMenu();
    var options = this.model.get('attribute').get('targets');
    _.each(options, function(opt) { selectMenu.append($.option(opt)) });
    selectMenu.val(this.model.get('value'));
    value.append(selectMenu);
    this.$el.append(value);
  },

  events: {
    'mouseenter .attribute': '_showRemoveButton',
    'mouseleave .attribute': '_hideRemoveButton',
    'change .value > select': '_changeValue',
    'click .remove-button': '_remove',
  },

  _showRemoveButton: function(e) {
    e.stopPropagation();
    this.$('.remove-button').css('visibility', 'visible');
  },

  _hideRemoveButton: function() {
    this.$('.remove-button').css('visibility', 'hidden');
  },

  _changeValue: function(e) {
    var newValue = $(e.currentTarget).val();
    this.model.updateValue(newValue);
  },

  _remove: function(e) {
    e.stopPropagation();
    this.model.remove();
  }

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
  var lhsJSON = $('#rule-lhs').data('json');

  var lhs = new AVM(null, { ruleName: name, json: lhsJSON });

  var rule = new Rule({
    id: name,
    name: name,
    description: description,
    lhs: lhs,
  });

  var ruleView = new RuleView({
    model: rule,
    el: '#interaction-block',
  });
  ruleView.render();

});
