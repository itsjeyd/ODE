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
        var matchingRules = _.map(model.get('matchingRules'), function(r) {
          return new Rule({ name: r.name, description: r.description });
        });
        model.set('matchingRules', new Results(matchingRules));
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


var Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
  },

  url: function() {
    return this.urlRoot + '/' + this.get('name');
  },

});


// Collections

var Results = Backbone.Collection.extend({

  setComparator: function(attr) {
    this.comparator = function(r) { return r.get(attr) };
    return this;
  },

});


// Views

var SearchTargetView = Backbone.View.extend({

  initialize: function() {
    this.on({
      'emptysearch': this._showWarningMsg,
    });
    this.model.on({
      'found': this._showResults,
      'notfound': this._showErrorMsg,
    }, this);
  },

  _showWarningMsg: function() {
    var results = this.$('#results');
    results.empty();
    results.append(
      $.span('text-warning warning-msg')
        .text('Please enter at least one search term.'));
  },

  _showResults: function() {
    var results = this.$('#results');
    results.empty();
    var resultView = new ResultView({
      collection: this.model.get('matchingRules'),
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
    this._renderFeatureField();
    this._renderStringField();
    return this;
  },

  _renderFeatureField: function(optional) {
    this._addSearchField('#features', optional);
  },

  _renderStringField: function(optional) {
    this._addSearchField('#strings', optional);
  },

  _addSearchField: function(blockID, optional) {
    var searchField = $.div('search-field col-md-11');
    var textInput = $.textInput();
    if (blockID === '#features') {
      textInput.attr( 'placeholder', 'Enter feature ...');
    } else {
      textInput.attr('placeholder', 'Enter output string ...');
    }
    searchField.append(textInput);
    this.$(blockID).append(searchField);
    var controls = $.div('controls col-md-1');
    controls.append($.plusButton());
    var minusButton = $.minusButton();
    if (!optional) {
      minusButton.hide();
    }
    controls.append(minusButton);
    this.$(blockID).append(controls);
  },

  events: {
    'click #search-button': '_performSearch',
    'click #features > .controls > .plus-button': '_addFeatureField',
    'click #strings > .controls > .plus-button': '_addStringField',
    'click #features > .controls > .minus-button': '_removeFeatureField',
    'click #strings > .controls > .minus-button': '_removeStringField',
  },

  _addFeatureField: function(e) {
    $(e.currentTarget).hide();
    this.$('#features').find('.minus-button').show();
    this._renderFeatureField(true);
  },

  _addStringField: function(e) {
    $(e.currentTarget).hide();
    this.$('#strings').find('.minus-button').show();
    this._renderStringField(true);
  },

  _removeFeatureField: function(e) {
    this._removeSearchField(e);
    this._adjustControls('#features');
  },

  _removeStringField: function(e) {
    this._removeSearchField(e);
    this._adjustControls('#strings');
  },

  _adjustControls: function(blockID) {
    var block = this.$(blockID);
    block.find('.plus-button').last().show();
    if (block.find('.search-field').length === 1) {
      block.find('.minus-button').last().hide();
    }
  },

  _removeSearchField: function(e) {
    var controls = $(e.currentTarget).parent('.controls');
    controls.prev('.search-field').remove();
    controls.remove();
  },

  _performSearch: function() {
    var searchFeatures = this._getSearchParams('#features');
    var searchStrings = this._getSearchParams('#strings');
    if (searchFeatures.length > 0 || searchStrings.length > 0) {
      this.model.reset();
      this.model.addFeatures(searchFeatures);
      this.model.addStrings(searchStrings);
      this.model.search();
    } else {
      this.trigger('emptysearch');
    }
  },

  _getSearchParams: function(blockID) {
    var searchParams = _.map(this.$(blockID).find('input'), function(field) {
      return $(field).val();
    });
    return _.filter(searchParams, function(p) {
      return p.length > 0;
    });
  },

});

var ResultView = Backbone.View.extend({

  initialize: function() {
    this.collection.on({
      'sort': function() {
        this.$el.empty();
        this.render();
      },
    }, this);
  },

  render: function() {
    var table = $.table();
    table.append(this._makeTableHeader());
    table.append(this._makeTableBody());
    this.$el.append(table);
    return this;
  },

  _makeTableHeader: function() {
    var thead = $.thead();
    thead.append($.th().attr('id', 'name-col').text('Rule'));
    thead.append($.th().attr('id', 'desc-col').text('Description'));
    return thead;
  },

  _makeTableBody: function() {
    var tbody = $.tbody();
    this.collection.each(function(r) {
      var tr = $.tr();
      tr.append($.td().html($.a(r.url(), r.get('name'))));
      tr.append($.td().text(r.get('description')));
      tbody.append(tr);
    });
    return tbody;
  },

  events: {
    'click #name-col': '_sortByName',
    'click #desc-col': '_sortByDescription',
  },

  _sortByName: function() {
    this.collection.setComparator('name').sort();
  },

  _sortByDescription: function() {
    this.collection.setComparator('description').sort();
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
