// Features

var Feature = Backbone.Model.extend({});

var FeatureList = Backbone.Collection.extend({});

var FeatureItemView = Backbone.View.extend({

  className: 'feature-item draggable',

  attributes: function() {
    return {
      id: this.model.id,
      'data-name': this.model.get('name'),
      'data-description': this.model.get('description'),
      'data-type': this.model.get('type'),
      'data-targets': this.model.get('targets'),
    }
  },

  initialize: function() {
    this.model.on({
      'hide': function() { this.$el.hide(); },
      'show': function() { this.$el.show(); },
    }, this);
  },

  render: function() {
    this.$el.text(this.model.get('name'));
    this.$el.draggable({
      helper: 'clone',
      revert: 'invalid',
    });
    return this;
  }

});

var FeatureListView = Backbone.View.extend({

  render: function() {
    this.$el.empty();
    this.collection.forEach(this._addFeatureItem, this);
    return this;
  },

  _addFeatureItem: function(featureItem) {
    var featureItemView = new FeatureItemView({ model: featureItem });
    this.$el.append(featureItemView.render().$el);
  },

  events: {
    'mouseenter .feature-item': '_highlight',
    'mouseleave .feature-item': '_unhighlight',
  },

  _highlight: function(e) {
    $(e.currentTarget).addClass('highlighted');
  },

  _unhighlight: function(e) {
    $(e.currentTarget).removeClass('highlighted');
  },

  filterItems: function(input) {
    this.collection.each(function(i) {
      if (!$.matches(i.get('name').toLowerCase(), input)) {
        i.trigger('hide');
      } else {
        i.trigger('show');
      }
    });
  },

});


// Rules

var Rule = Backbone.Model.extend({});

var RuleView = Backbone.View.extend({});


// Application


$(document).ready(function() {

  // "Features" sidebar

  var featureItems = $('.feature-item');

  var featureList = new FeatureList(
    _.map(featureItems, function(i) {
      var item = $(i);
      var name = item.data('name');
      return new Feature({
        id: name,
        name: name,
        type: item.data('type'),
        description: item.data('description'),
        targets: item.dataToArray('targets'),
      });
    }),
    { comparator: 'name' }
  );

  var featureListView = new FeatureListView({
    el: '#feature-list',
    collection: featureList,
  });
  featureListView.render();

  $('#feature-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    featureListView.filterItems(currentInput);
  });

  // Input Builder

  var name = $('#rule-name').text();
  var description = $('#rule-description').text();
  var rule = new Rule({ id: name, name: name, description: description });

  var ruleView = new RuleView({ model: rule });

});
