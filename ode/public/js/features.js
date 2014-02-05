// Models

var Feature = Backbone.Model.extend({});
var Value = Backbone.Model.extend({});

var FeatureList = Backbone.Collection.extend({ model: Feature });
var ValueList = Backbone.Collection.extend({ model: Value });


// Model views

var FeatureView = Backbone.View.extend({
  renderListEntry: function() {
    var listEntryTemplate = _.template(
      '<div id="<%= name %>" class="feature-item" ' +
        'data-name="<%= name %>" data-type="<%= type %>" ' +
        'data-description="<%= description %>" ' +
        'data-targets="<%= targets %>">' +
        '<%= name %>' +
        '</div>');
    var attributes = this.model.toJSON();
    return $(listEntryTemplate(attributes));
  },
});

var ValueView = Backbone.View.extend({
  renderListEntry: function() {
    var listEntryTemplate = _.template(
      '<div id="<%= name %>" class="value-item" ' +
        'data-name="<%= name %>">' +
        '<%= name %>' +
        '</div>');
    var attributes = this.model.toJSON();
    return $(listEntryTemplate(attributes));
  }
});


// Collection views

var FeatureListView = Backbone.View.extend({
  render: function() {
    this.collection.forEach(this.addFeature, this);
  },
  addFeature: function(featureItem) {
    var featureView = new FeatureView({ model: featureItem });
    this.$el.append(featureView.renderListEntry());
  }
});

var ValueListView = Backbone.View.extend({
  render: function() {
    this.collection.forEach(this.addValue, this);
  },
  addValue: function(valueItem) {
    var valueView = new ValueView({ model: valueItem });
    this.$el.append(valueView.renderListEntry());
  }
});



$(document).ready(function() {

  var interactionBlock = $('#interaction-block');
  var newFeatureBlock = $('#new-feature-block');
  var newFeatureButton = $('#new-feature-button');

  newFeatureBlock.hide();

  newFeatureButton.on('click', function() {
    interactionBlock.html(newFeatureBlock.html());
  });

  var featureItems = $('.feature-item');
  var valueItems = $('.value-item');

  var featureList = new FeatureList();
  var valueList = new ValueList();


  featureItems.each(function() {
    var item = $(this);
    var feature = new Feature({
      name: item.data('name'),
      type: item.data('type'),
      description: item.data('description'),
      targets: item.data('targets'),
    });
    featureList.add(feature);
  });

  valueItems.each(function() {
    var item = $(this);
    var value = new Value({
      name: item.data('name'),
    });
    valueList.add(value);
  });

  var featureListView = new FeatureListView({
    collection: featureList,
    $el: $('#feature-list'),
  });

  var valueListView = new ValueListView({
    collection: valueList,
    $el: $('#value-list'),
  });

});
