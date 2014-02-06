// Models

var Feature = Backbone.Model.extend({});
var Value = Backbone.Model.extend({});

var FeatureList = Backbone.Collection.extend({ model: Feature });
var ValueList = Backbone.Collection.extend({ model: Value });


// Model views

var FeatureView = Backbone.View.extend({
  render: function() {
    this.$el.text(
      'I want to be an edit block for ' + this.model.get('name') +
        ' when I grow up!');
    return this;
  },
});

var FeatureItemView = Backbone.View.extend({
  className: 'feature-item',
  render: function() {
    var attributes = this.model.toJSON();
    this.$el.attr('id', this.model.id);
    this.$el.attr('data-name', attributes['name']);
    this.$el.attr('data-type', attributes['type']);
    this.$el.attr('data-description', attributes['description']);
    this.$el.attr('data-targets', attributes['targets']);
    this.$el.text(attributes['name']);
    return this;
  },
});

var ValueItemView = Backbone.View.extend({
  className: 'value-item',
  render: function() {
    var name = this.model.get('name');
    this.$el.attr('id', this.model.id);
    this.$el.attr('data-name', name);
    this.$el.text(name);
    return this;
  },
});


// Collection views

var FeatureListView = Backbone.View.extend({
  events: {
    'click .feature-item': 'dispatcher',
    'mouseenter .feature-item': 'highlight',
    'mouseleave .feature-item': 'unhighlight',
    'unselect-all': 'unselectAll',
  },
  dispatcher: function(e) { this.select(e); this.showEditBlock(e); },
  select: function(e) {
    this.$el.trigger('unselect-all');
    $(e.currentTarget).addClass('selected');
  },
  unselectAll: function() {
    this.$el.find('.selected').removeClass('selected');
  },
  showEditBlock: function(e) {
    var featureID = e.currentTarget.id;
    var feature = this.collection.get(featureID);
    var featureView = new FeatureView({ model: feature });
    featureView.render();
    $('#interaction-block').html(featureView.$el.html());
  },
  highlight: function(e) { $(e.currentTarget).addClass('highlighted'); },
  unhighlight: function(e) { $(e.currentTarget).removeClass('highlighted'); },
  render: function() {
    this.collection.forEach(this.addFeatureItem, this);
  },
  addFeatureItem: function(featureItem) {
    var featureItemView = new FeatureItemView({ model: featureItem });
    this.$el.append(featureItemView.render().$el);
  }
});

var ValueListView = Backbone.View.extend({
  render: function() {
    this.collection.forEach(this.addValue, this);
  },
  addValue: function(valueItem) {
    var valueView = new ValueItemView({ model: valueItem }); // Should be a ValueItemView!
    this.$el.append(valueView.render().$el);
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
    var name = item.data('name');
    var feature = new Feature({
      id: name,
      name: name,
      type: item.data('type'),
      description: item.data('description'),
      targets: item.data('targets'),
    });
    featureList.add(feature);
  });

  valueItems.each(function() {
    var item = $(this);
    var name = item.data('name');
    var value = new Value({
      id: name,
      name: name,
    });
    valueList.add(value);
  });

  var featureListView = new FeatureListView({
    id: 'feature-list',
    collection: featureList,
  });

  featureListView.render();
  $('#feature-list').replaceWith(featureListView.$el);

  var valueListView = new ValueListView({
    id: 'value-list',
    collection: valueList,
  });

  valueListView.render();
  $('#value-list').replaceWith(valueListView.$el);

});
