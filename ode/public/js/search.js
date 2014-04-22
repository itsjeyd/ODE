// Models

var SearchTarget = Backbone.Model.extend({

  defaults: {
    features: new Backbone.Collection([]),
    strings: new Backbone.Collection([]),
  },

  reset: function() {
    this.get('features').reset();
    this.get('strings').reset();
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
    this.fetch({
      url: '/search',
      method: 'POST',
      success: function(model, response, options) {
        model.trigger('found');
      },
      error: function(model, response, options) {
        model.trigger('notfound');
      },
    });
  },

});

var Feature = Backbone.Model.extend({

  defaults: { value: '' }

});

var String = Backbone.Model.extend({});


// Views

var SearchTargetView = Backbone.View.extend({

  initialize: function() {
    this.model.on({
      'found': this._showResults,
      'notfound': this._showErrorMsg,
    }, this);
  },

  _showResults: function() {
    var results = this.$('#results');
    results.empty();
    var resultView = new ResultView({
      model: this.model,
      el: results,
    });
    resultView.render();
  },

  _showErrorMsg: function() {
    var results = this.$('#results');
    results.empty();
    results.append(
      $.span('text-danger error-msg')
        .text('There are no rules matching the search terms you entered.'));
  },

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
    this.model.reset();
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

var ResultView = Backbone.View.extend({

  render: function() {
    var table = $.table();
    table.append(this._makeTableHeader());
    table.append(this._makeTableBody());
    this.$el.append(table);
    return this;
  },

  _makeTableHeader: function() {
    var thead = $.thead();
    thead.append($.th().text('Rule'));
    thead.append($.th().text('Description'));
    return thead;
  },

  _makeTableBody: function() {
    var tbody = $.tbody();
    _.each(this.model.get('matchingRules'), function(r) {
      var tr = $.tr();
      tr.append($.td().html($.a('rules/' + r.name, r.name)));
      tr.append($.td().text(r.description));
      tbody.append(tr);
    });
    return tbody;
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
