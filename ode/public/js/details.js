// Models

var Pair = Backbone.Model.extend({

  initialize: function(attrs, options) {
    this.parent = options.parent;
    this.set('attribute', new Feature({
      name: options.attribute.name,
      type: options.attribute.type
    }));
    this._setValue(options.value);
  },

  _setValue: function(value) {
    if (typeof value === 'string') {
      this.set('value', value);
    } else {
      this.set('value', new AVM(null, { json: value }));
    }
  },

});

var Feature = Backbone.Model.extend({});

// Collections

var AVM = Backbone.Collection.extend({

  initialize: function(models, options) {
    _.each(options.json.pairs, function(pair) {
      this.add(new Pair(null, { parent: this,
                                attribute: pair.attribute,
                                value: pair.value }));
    }, this);
  },

});

var RHS = Backbone.Collection.extend({});


// Views

var AVMView = Backbone.View.extend({

  className: 'avm',

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
        this._renderBrackets();
      },
    }, this);
  },

  _renderBrackets: function() {
    var leftBracket = this._makeBracket('left');
    var rightBracket = this._makeBracket('right');
    var h = this.$el.children('.content').height() || 14;
    leftBracket.insertBefore(this.$el.children('.content'));
    rightBracket.insertAfter(this.$el.children('.content'));
    leftBracket.height(h);
    rightBracket.height(h);
  },

  _makeBracket: function(type) {
    return $.div('bracket bracket-' + type);
  },

  render: function() {
    this._renderContent();
    return this;
  },

  _renderContent: function() {
    var content = $.div('content');
    this.collection.each(function(pair) {
      content.append(this._makePair(pair));
    }, this);
    this.$el.append(content);
  },

  _makePair: function(pair) {
    return new PairView({ model: pair, parentView: this }).render().$el;
  },

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
  },

  render: function() {
    this._renderAttr();
    this._renderVal();
    return this;
  },

  _renderAttr: function() {
    var name = this.model.get('attribute').get('name');
    var attr = $.span('attribute').text(name);
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
  },

  _renderValue: function() {
    var value = $.span('value').text(this.model.get('value'));
    this.$el.append(value);
  },

});

var RHSView = Backbone.View.extend({});


// Application

$(document).ready(function() {

  var lhsJSON = $('#lhs').data('json');
  var lhs = new AVM(null, { json: lhsJSON });

  var lhsView = new AVMView({
    collection: lhs,
    el: '#lhs'
  });
  lhsView.render();
  lhsView.trigger('inserted');

  var rhsJSON = $('#rhs').data('json');
  var rhs = new RHS(null, { json: rhsJSON });

  var rhsView = new RHSView({
    collection: rhs,
    el: '#rhs'
  });
  rhsView.render();

});
