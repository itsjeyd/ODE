// Models

var Feature = Backbone.Model.extend({
  initialize: function() {
    this.urlRoot = '/features'
  },
  validate: function(attrs, options) {
    if (attrs.name.split(/ +/).length > 1) {
      return 'Feature names can not contain whitespace.';
    }
  },
  updateName: function(newName) {
    this.save({ name: newName },
              { url: this.url() + '/name',
                wait: true,
                success: function(model, response, options) {
                  model.id = newName;
                },
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:name', response.message);
                },
              });
  },
  updateDescription: function(newDescription) {
    this.save({ description: newDescription },
              { url: this.url() + '/description',
                wait: true,
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:description', response.message);
                },
              });
  },
});
var Value = Backbone.Model.extend({});

var FeatureList = Backbone.Collection.extend({ model: Feature });
var ValueList = Backbone.Collection.extend({ model: Value });


// Model views

var FeatureView = Backbone.View.extend({
  events: {
    'dblclick h3': function(e) {
      this._renderEditControls('name')(e)
    },
    'click button.fname': function() {
      this._saveEdits('name')(this)
    },
    'dblclick p': function(e) {
      this._renderEditControls('description')(e)
    },
    'click button.fdescription': function() {
      this._saveEdits('description')(this)
    },
  },
  _renderEditControls: function(modelField) {
    return function(e) {
      var fieldToEdit = $(e.currentTarget);
      var inputField = $('<input>').addClass('form-control f' + modelField)
        .attr('type', 'text').val(fieldToEdit.text());
      var okButton = $('<button>').addClass('btn btn-info f' + modelField)
        .text('OK');
      fieldToEdit.hide();
      inputField.insertAfter(fieldToEdit);
      okButton.insertAfter(inputField);
      inputField.focus();
    };
  },
  _saveEdits: function(modelField) {
    return function(view) {
      var inputField = view.$el.find('input.f' + modelField);
      var okButton = view.$el.find('button.f' + modelField);
      if (!inputField.isEmpty() &&
          inputField.val() !== view.model.get(modelField)) {
        var updateFunction = 'update' + modelField.charAt(0).toUpperCase() +
          modelField.slice(1);
        view.model[updateFunction](inputField.val());
      } else {
        view.render();
      }
    };
  },
  initialize: function() {
    this.model.on('change', this.render, this);
    this.model.on('invalid', function(model, error) {
      this._renderAlert('button.fname', error);
    }, this);
    this.model.on('update-error:name', function(msg) {
      this._renderAlert('button.fname', msg);
    }, this);
    this.model.on('update-error:description', function(msg) {
      this._renderAlert('button.fdescription', msg);
    }, this);
  },
  _renderAlert: function(button, msg) {
    var updateButton = this.$el.find(button);
    updateButton.next('.alert-msg').remove();
    var alertMsg = $('<span>').addClass('alert-msg text-danger')
      .text(msg);
    alertMsg.insertAfter(updateButton);
  },
  render: function() {
    this.$el.empty();
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
  attributes: function() {
    return {
      id: this.model.id,
      'data-name': this.model.get('name'),
      'data-type': this.model.get('type'),
      'data-description': this.model.get('description'),
      'data-targets': this.model.get('targets'),
    }
  },
  initialize: function() {
    this.model.on('change:name', this.render, this);
  },
  render: function() {
    this.$el.text(this.model.get('name'));
    return this;
  },
});

var ValueItemView = Backbone.View.extend({
  className: 'value-item',
  attributes: function() {
    return {
      id: this.model.id,
      'data-name': this.model.get('name'),
    }
  },
  render: function() {
    this.$el.text(this.model.get('name'));
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
    $('#interaction-block').html(featureView.$el);
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
