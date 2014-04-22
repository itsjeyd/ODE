// Models

var SearchTarget = Backbone.Model.extend({

  defaults: {
    features: new Backbone.Collection([]),
    strings: new Backbone.Collection([]),
  },

});

var Feature = Backbone.Model.extend({

  defaults: { value: '' }

});

// Views

var SearchTargetView = Backbone.View.extend({

  render: function() {
    this._addFeatureField();
    this._addStringField();
    return this;
  },

  _addFeatureField: function() {
    this._addSearchField('#features');
  },

  _addStringField: function() {
    this._addSearchField('#strings');
  },

  _addSearchField: function(blockID) {
    var searchField = $.div('search-field col-md-11');
    searchField.append($.textInput());
    this.$(blockID).append(searchField);
    var controls = $.div('controls col-md-1');
    controls.append($.plusButton());
    this.$(blockID).append(controls);
  },

});

// Application

$(document).ready(function() {

  var searchTarget = new SearchTarget();

  var searchTargetView = new SearchTargetView({
    model: searchTarget,
    el: '.container-full',
  });
  searchTargetView.render();

});
