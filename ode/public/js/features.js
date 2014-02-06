// Models

var Feature = Backbone.Model.extend({});
var Value = Backbone.Model.extend({});

var FeatureList = Backbone.Collection.extend({ model: Feature });
var ValueList = Backbone.Collection.extend({ model: Value });


// Model views

var FeatureView = Backbone.View.extend({
  render: function() {
    this._renderName();
    this._renderDescription();
    this._renderTypeForm();
    this._renderTargetListHeading();
    this._renderTargets();
    this._renderTargetForm();
    return this;
  },
  _renderName: function() {
    var nameTemplate = _.template('<h3><%= name %></h3>');
    var node = $(nameTemplate({ name: this.model.get('name') }));
    this.$el.append(node);
  },
  _renderDescription: function() {
    var descriptionTemplate = _.template('<p><%= description %></p>');
    var node = $(descriptionTemplate({
      description: this.model.get('description')
    }));
    this.$el.append(node);
  },
  _renderTypeForm: function() {
    var typeFormTemplate = _.template(
      '<form role="form">' +
        '<div class="radio"><label>' +
        '<input type="radio" name="type" value="complex" />complex' +
        '</label></div>' +
        '<div class="radio"><label>' +
        '<input type="radio" name="type" value="atomic" />atomic' +
        '</label></div>' +
        '</form>');
    var node = $(typeFormTemplate());
    node.find('input[value="' + this.model.get('type') + '"]').check();
    this.$el.append(node);
  },
  _renderTargetListHeading: function() {
    var targetListHeadingTemplate = _.template('<h4><%= heading %></h4>');
    var heading;
    if (this.model.get('type') === 'complex') {
      heading = 'Features permitted in substructure:';
    } else {
      heading = 'Permitted values:';
    }
    var node = $(targetListHeadingTemplate({ heading: heading }));
    this.$el.append(node);
  },
  _renderTargets: function() {
    var targetListTemplate = _.template(
      '<% _.each(targets, function(target) { %>' +
        '<div class="target"><%= target %></div>' +
        '<% }); %>');
    var node = $(targetListTemplate({ targets: this.model.get('targets') }));
    this.$el.append(node);
  },
  _renderTargetForm: function() {
    var targetFormTemplate = _.template(
      '<form role="form">' +
        '<div class="droppable">Drop <%= targetType %> here ...</div>' +
        '</form>');
    var targetType;
    if (this.model.get('type') === 'complex') {
      targetType = 'feature';
    } else {
      targetType = 'value';
    }
    var node = ($(targetFormTemplate({ targetType: targetType })));
    this.$el.append(node);
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
      targets: item.dataToArray('targets'),
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
