var Details = {};

Details.Model = {};
Details.Collection = {};
Details.View = {};


// Models

Details.Model.Pair = Backbone.Model.extend({

  initialize: function(attrs, options) {
    this.parent = options.parent;
    this.set('attribute', new Details.Model.Feature({
      name: options.attribute.name,
      type: options.attribute.type
    }));
    this._setValue(options.value);
  },

  _setValue: function(value) {
    if (typeof value === 'string') {
      this.set('value', value);
    } else {
      this.set('value', new Details.Collection.AVM(null, { json: value }));
    }
  },

});

Details.Model.Feature = Backbone.Model.extend({});

Details.Model.OutputString = Backbone.Model.extend({});



// Collections

Details.Collection.AVM = Backbone.Collection.extend({

  initialize: function(models, options) {
    _.each(options.json.pairs, function(pair) {
      this.add(new Details.Model.Pair(null, { parent: this,
                                              attribute: pair.attribute,
                                              value: pair.value }));
    }, this);
  },

});


Details.Collection.RHS = Backbone.Collection.extend({

  initialize: function(models, options) {
    var outputStrings = [];
    _.each(options.json.groups, function(g) {
      _.each(g.outputStrings, function(os) {
        outputStrings.push(new Details.Model.OutputString({
          content: os.content
        }));
      });
      var parts = _.map(
        _.sortBy(g.partsTable.slots, function(s) {
          return s.position;
        }),
        function(s) {
          return _.map(s.parts, function(p) {
            return p.content;
          });
        });
      var combinations = _.map(this._cart(parts), function(os) {
        return new Details.Model.OutputString({ content: os });
      });
      outputStrings = outputStrings.concat(combinations);
    }, this);
    this.add(outputStrings);
  },

  _cart: function(parts) {
    if (parts.length === 0) {
      return [];
    } else if (parts.length === 1) {
      return parts.pop();
    } else if (parts.length === 2) {
      return this._combineSlots(parts[0], parts[1]);
    } else {
      var intermediateResult = this._combineSlots(parts[0], parts[1]);
      var remainingSlots = parts.slice(2);
      return this._cart([intermediateResult].concat(remainingSlots));
    }
  },

  _combineSlots: function(slot1, slot2) {
    var partsTable = this;
    var acc = function(a, b, result) {
      if (a.length === 0) {
        return result
      } else {
        var intermediateResult = partsTable._combineStrings(a[0], b);
        return acc(a.slice(1), b, result.concat(intermediateResult));
      }
    };
    if (slot1.length === 0 && slot2.length === 0) {
      return [];
    } else {
      return acc(slot1, slot2, []);
    }
  },

  _combineStrings: function(string, slot) {
    var acc = function(str, slt, result) {
      if (slt.length === 0) {
        return result
      } else {
        var concatenatedString = str + ' ' + slt[0];
        return acc(str, slt.slice(1), result.concat([concatenatedString]));
      }
    }
    if (slot.length === 0) {
      return [str];
    } else {
      return acc(string, slot, []);
    }
  },

});



// Views

Details.View.AVMView = Backbone.View.extend({

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
    var h = this.$el.children('.content').height() ||
      this.$el.children('.content').css('font-size');
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
    return new Details.View.PairView({
      model: pair, parentView: this
    }).render().$el;
  },

});


Details.View.PairView = Backbone.View.extend({

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
    var avmView = new Details.View.AVMView({
      collection: this.model.get('value'),
      parentView: this
    });
    value.append(avmView.render().$el);
    this.$el.append(value);
  },

  _renderValue: function() {
    var value = $.span('value').text(this.model.get('value'));
    this.$el.append(value);
  },

});


Details.View.RHSView = Backbone.View.extend({

  render: function() {
    this.collection.each(function(os) {
      var item = $('<p>').text(os.get('content'));
      this.$el.append(item);
    }, this);
    return this;
  },

});



// Application

$(document).ready(function() {

  var lhsJSON = $('#lhs').data('json');
  var lhs = new Details.Collection.AVM(null, { json: lhsJSON });

  var lhsView = new Details.View.AVMView({
    collection: lhs,
    el: '#lhs'
  });
  lhsView.render();
  lhsView.trigger('inserted');

  var rhsJSON = $('#rhs').data('json');
  var rhs = new Details.Collection.RHS(null, { json: rhsJSON });

  var rhsView = new Details.View.RHSView({
    collection: rhs,
    el: '#rhs'
  });
  rhsView.render();

});
