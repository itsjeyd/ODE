var Search = {};

Search.Model = {};
Search.Collection = {};
Search.View = {};


// Models

Search.Model.SearchTarget = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/search';
  },

  defaults: {
    features: new Backbone.Collection([]),
    strings: new Backbone.Collection([]),
  },

  reset: function() {
    this.get('features').reset();
    this.get('strings').reset();
    if (this.has('matchingRules')) {
      this.unset('matchingRules');
    }
  },

  addFeatures: function(names) {
    _.each(names, function(n) {
      if (n.match(/^<.+>.*/i)) {
        var name = n.slice(1, n.indexOf('>'));
        var value = n.slice(n.indexOf('>') + 1);
        this.get('features')
          .add(new Search.Model.Feature({ name: name, value: value }));
      } else {
        this.get('features')
          .add(new Search.Model.Feature({ name: n }));
      }
    }, this);
  },

  addStrings: function(strings) {
    _.each(strings, function(s) {
      this.get('strings').add(new Search.Model.String({ content: s }));
    }, this);
  },

  search: function() {
    this.save(null, {
      success: function(model, response, options) {
        var matchingRules = _.map(model.get('matchingRules'), function(r) {
          return new Search.Model.Rule({
            name: r.name, description: r.description
          });
        });
        model.set('matchingRules',
                  new Search.Collection.Results(matchingRules,
                                                { comparator: 'name' }));
        model.trigger('found');
      },
      error: function(model, response, options) {
        model.trigger('notfound');
      },
    });
  },

});


Search.Model.Feature = Backbone.Model.extend({

  defaults: { value: '' }

});


Search.Model.String = Backbone.Model.extend({});


Search.Model.Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
  },

  url: function() {
    return this.urlRoot + '/' + this.get('name');
  },

});



// Collections

Search.Collection.Results = Backbone.Collection.extend({

  initialize: function() {
    this.sortState = { name: 'ASC' };
  },

  setComparator: function(attr, direction) {
    if (direction === 'ASC') {
      this.comparator =
        function(r1, r2) { return r1.get(attr) >= r2.get(attr) };
    } else if (direction === 'DESC') {
      this.comparator =
        function(r1, r2) { return r1.get(attr) <= r2.get(attr) };
    }
    return this;
  },

});



// Views

Search.View.SearchTargetView = Backbone.View.extend({

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
    var resultView = new Search.View.ResultView({
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


Search.View.ResultView = Backbone.View.extend({

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
    var sortState = this.collection.sortState;
    if (sortState.name && sortState.name === 'ASC') {
      sortState.name = 'DESC';
      this.collection.setComparator('name', sortState.name).sort();
    } else {
      delete sortState['description'];
      sortState.name = 'ASC';
      this.collection.setComparator('name', sortState.name).sort();
    }
  },

  _sortByDescription: function() {
    var sortState = this.collection.sortState;
    if (sortState.description && sortState.description === 'ASC') {
      sortState.description = 'DESC';
      this.collection.setComparator('description', sortState.description)
        .sort();
    } else {
      delete sortState['name'];
      sortState.description = 'ASC';
      this.collection.setComparator('description', sortState.description)
        .sort();
    }
  },

});



// Application

$(document).ready(function() {

  $('a#search').parent("li").addClass('active');

  var searchTarget = new Search.Model.SearchTarget();

  var searchTargetView = new Search.View.SearchTargetView({
    model: searchTarget,
    el: '.container-full',
  });
  searchTargetView.render();

});
