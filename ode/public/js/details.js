// details.js --- Client-side code for page showing detailed information about a specific rule.

// Copyright (C) 2013-2015  Tim Krones

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
      var combinations = _.map(cart(parts), function(os) {
        return new Details.Model.OutputString({ content: os });
      });
      outputStrings = outputStrings.concat(combinations);
    }, this);
    this.add(outputStrings);
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
