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
    var removeButton = $.removeButton();
    if (!optional) {
      removeButton.hide();
    }
    controls.append(removeButton);
    this.$(blockID).append(controls);
  },

  events: {
    'click #search-button': '_performSearch',
    'click #features > .controls > .plus-button': '_addFeatureField',
    'click #strings > .controls > .plus-button': '_addStringField',
    'click #features > .controls > .remove-button': '_removeFeatureField',
    'click #strings > .controls > .remove-button': '_removeStringField',
  },

  _addFeatureField: function(e) {
    $(e.currentTarget).hide();
    this.$('#features').find('.remove-button').show();
    this._renderFeatureField(true);
  },

  _addStringField: function(e) {
    $(e.currentTarget).hide();
    this.$('#strings').find('.remove-button').show();
    this._renderStringField(true);
  },

  _removeFeatureField: function(e) {
    this._removeSearchField(e);
    this.$('#features').find('.plus-button').last().show();
    if (this.$('#features').find('.search-field').length === 1) {
      this.$('#features').find('.remove-button').last().hide();
    }
  },

  _removeStringField: function(e) {
    this._removeSearchField(e);
    this.$('#strings').find('.plus-button').last().show();
    if (this.$('#strings').find('.search-field').length === 1) {
      this.$('#strings').find('.remove-button').last().hide();
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
