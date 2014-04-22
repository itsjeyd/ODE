// Models

var SearchTarget = Backbone.Model.extend({

  defaults: {
    features: new Backbone.Collection([]),
    strings: new Backbone.Collection([]),
  },

  addFeatures: function(names) {
    _.each(names, function(n) {
      if (n.match(/^<.+>.*/i)) {
        var name = n.slice(1, n.indexOf('>'));
        var value = n.slice(n.indexOf('>') + 1);
        this.get('features').add(new Feature({ name: name, value: value }));
      } else {
        this.get('features').add(new Feature({ name: n }));
      }
    }, this);
  },

  addStrings: function(strings) {
    _.each(strings, function(s) {
      this.get('strings').add(new String({ content: s }));
    }, this);
  },

  search: function() {
    alert('Pretending to do some serious searching ...');
  },

});

var Feature = Backbone.Model.extend({

  defaults: { value: '' }

});

var String = Backbone.Model.extend({});


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

  events: {
    'click #search-button': '_performSearch',
  },

  _performSearch: function() {
    var searchFeatures = this._getSearchParams('#features');
    this.model.addFeatures(searchFeatures);
    var searchStrings = this._getSearchParams('#strings');
    this.model.addStrings(searchStrings);
    this.model.search();
  },

  _getSearchParams: function(blockID) {
    return _.map(this.$(blockID).find('input'), function(field) {
      return $(field).val();
    });
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
