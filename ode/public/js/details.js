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

var AVMView = Backbone.View.extend({});

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

  var rhsJSON = $('#rhs').data('json');
  var rhs = new RHS(null, { json: rhsJSON });

  var rhsView = new RHSView({
    collection: rhs,
    el: '#rhs'
  });
  rhsView.render();

});
