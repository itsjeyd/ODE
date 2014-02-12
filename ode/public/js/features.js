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
  updateType: function(newType) {
    this.save({ type: newType },
              { url: this.url() + '/type',
                wait: true,
                success: function(model, response, options) {
                  model.set({ targets: [] });
                },
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:type', response.message);
                },
              });
  },
  removeTarget: function(targetName) {
    var targets = _.without(this.get('targets'), targetName);
    this.save({},
              { url: this.url() + '/targets/' + targetName,
                wait: true,
                success: function(model, response, options) {
                  model.set({ targets: targets });
                },
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:remove-target',
                                response.message);
                },
              });
  },
  addTarget: function(targetName) {
    if (this.get('targets').indexOf(targetName) !== -1) {
      this.trigger('update-error:add-target', 'Target already in list.');
    } else {
      var targets = this.get('targets');
      targets.push(targetName);
      this.save({ target: targetName },
                { url: this.url() + '/targets',
                  wait: true,
                  success: function(model, response, options) {
                    model.set({ targets: targets });
                    model.trigger('update-success:add-target',
                                  targetName, model.get('type'));
                  },
                  error: function(model, xhr, options) {
                    var response = $.parseJSON(xhr.responseText);
                    model.trigger('update-error:add-target',
                                  response.message);
                },
                });
    }
  },
  del: function() {
    this.destroy({
      wait: true,
      success: function(model, response, options) {
        model.trigger('destroy');
      },
      error: function(model, xhr, options) {
        var response = $.parseJSON(xhr.responseText);
        alert(response.message);
      },
    });
  },
});
var Value = Backbone.Model.extend({
  initialize: function() {
    this.urlRoot = '/values'
  },
  updateName: function(newName) {
    this.save({ name: newName },
              { wait: true,
                success: function(model, response, options) {
                  model.id = newName;
                },
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:name', response.message);
                },
              });
  },
});

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
    'change :radio': function(e) {
      var selectedType = $(e.currentTarget).val();
      if (selectedType === this.model.get('type')) {
        this.$el.find('button.ftype').remove();
        this.$el.find('h4').show();
        this.$el.find('.target').show();
        this.$el.find('.droppable').show();
        this.$el.find('button.ftarget').show();
        this.$el.find('.alert-msg').show();
      } else {
        var okButton = $.okButton('ftype');
        okButton.insertAfter(this.$el.find('.radio').last());
        this.$el.find('h4').hide();
        this.$el.find('.target').hide();
        this.$el.find('.droppable').hide();
        this.$el.find('button.ftarget').hide();
        this.$el.find('.alert-msg').hide();
      }
    },
    'click button.ftype': function(e) {
      e.preventDefault();
      this.model.updateType(this.$el.find('input:checked').val());
    },
    'mouseenter .target': function(e) {
      var currentTarget = $(e.currentTarget);
      currentTarget.append($.removeButton(currentTarget.text()));
    },
    'mouseleave .target': function(e) {
      $(e.currentTarget).find('.remove-button').remove();
    },
    'click .remove-button': function(e) {
      var targetName = $(e.currentTarget).data('target');
      this.model.removeTarget(targetName);
    },
    'click button.ftarget': function(e) {
      var targetName = this.$el.find('.droppable').text();
      this.model.addTarget(targetName);
    },
    'click .editable': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() === 'Drop value here ...') {
        inputField.empty();
      }
      inputField.next('button.ftarget').remove();
      var addButton = $('<button>').addClass('btn btn-info ftarget')
        .text('Add').disable();
      addButton.insertAfter(inputField);
    },
    'keyup .editable': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() !== '') {
        this.$el.find('button.ftarget').enable();
      } else {
        this.$el.find('button.ftarget').disable();
      }
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
    this.model.on('update-error:add-target', function(msg) {
      this._renderAlert('button.ftarget', msg);
    }, this);
    this.model.on('destroy', function() { this.remove() }, this);
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
    this._renderTargetField();
    this._activateTargetField();
    return this;
  },
  _activateTargetField: function() {
    var targetType;
    if (this.model.get('type') === 'complex') {
      targetType = '.feature-item';
    } else {
      targetType = '.value-item';
    }
    var viewModel = this.model;
    this.$el.find('.droppable').droppable({
      accept: targetType,
      drop: function(e, ui) {
        var targetName = $(ui.helper).text();
        var targetField = $(this);
        targetField.next('button.ftarget').remove();
        targetField.text(targetName);
        var addButton = $('<button>').addClass('btn btn-info ftarget')
          .text('Add');
        addButton.insertAfter(targetField);
      },
    });
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
  _renderTargetField: function() {
    var targetFormTemplate = _.template(
      '<div class="droppable">Drop <%= targetType %> here ...</div>');
    var node;
    if (this.model.get('type') === 'complex') {
      node = ($(targetFormTemplate({ targetType: 'feature' })));
    } else {
      node = ($(targetFormTemplate({ targetType: 'value' })));
      node.makeEditable();
    }
    this.$el.append(node);
  },
});

var FeatureItemView = Backbone.View.extend({
  className: 'feature-item draggable',
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
    this.model.on('hide', function() { this.$el.hide(); }, this);
    this.model.on('show', function() { this.$el.show(); }, this);
  },
  render: function() {
    this.$el.text(this.model.get('name'));
    this.$el.draggable({
      cursor: 'crosshair',
      helper: 'clone',
      revert: 'invalid',
    });
    return this;
  },
});

var ValueItemView = Backbone.View.extend({
  className: 'value-item draggable',
  attributes: function() {
    return {
      id: this.model.id,
      'data-name': this.model.get('name'),
    }
  },
  initialize: function() {
    this.model.on('change', this.render, this);
    this.model.on('hide', function() { this.$el.hide(); }, this);
    this.model.on('show', function() { this.$el.show(); }, this);
    this.model.on('update-error:name', function(msg) {
      this._renderAlert(msg);
    }, this);
  },
  _renderAlert: function(msg) {
    var updateButton = this.$el.next().next();
    updateButton.next('.alert-msg').remove();
    var alertMsg = $('<span>').addClass('alert-msg text-danger')
      .text(msg);
    alertMsg.insertAfter(updateButton);
  },
  render: function() {
    this.$el.text(this.model.get('name'));
    this.$el.draggable({
      cursor: 'crosshair',
      helper: 'clone',
      revert: 'invalid',
    });
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
    'click .remove-button': 'removeFeature',
  },
  dispatcher: function(e) {
    this.select(e); this._renderDeleteButton(e); this.showEditBlock(e); },
  select: function(e) {
    this.$el.trigger('unselect-all');
    $(e.currentTarget).addClass('selected');
  },
  unselectAll: function() {
    var selected = this.$el.find('.selected').removeClass('selected');
    selected.find('.remove-button').remove();
  },
  _renderDeleteButton: function(e) {
    var selectedFeature = $(e.currentTarget);
    selectedFeature.find('.remove-button').remove();
    var removeButton = $.removeButton(selectedFeature.text())
      .css('float', 'right');
    $(e.currentTarget).append(removeButton);
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
  removeFeature: function(e) {
    var featureName = $(e.currentTarget).data('target');
    this.collection.findWhere({ name: featureName }).del();
  },
  render: function() {
    this.$el.empty();
    this.collection.forEach(this.addFeatureItem, this);
  },
  addFeatureItem: function(featureItem) {
    var featureItemView = new FeatureItemView({ model: featureItem });
    this.$el.append(featureItemView.render().$el);
  },
  initialize: function() {
    this.collection.on('destroy', this.render, this);
  }
});

var ValueListView = Backbone.View.extend({
  events: {
    'dblclick .value-item': 'edit',
    'click button.vname': 'save',
  },
  edit: function(e) {
    var fieldToEdit = $(e.currentTarget);
    var inputField = $('<input>').addClass('form-control vname')
      .attr('type', 'text').val(fieldToEdit.text());
    var okButton = $('<button>').addClass('btn btn-info vname')
      .text('OK');
    fieldToEdit.hide();
    inputField.insertAfter(fieldToEdit);
    okButton.insertAfter(inputField);
    inputField.focus();
  },
  save: function(e) {
    var inputField = $(e.currentTarget).prev('input.vname');
    var newName = inputField.val();
    var taken = this.collection.where({ name: newName }).length > 0;
    if (newName && !taken) {
      var oldName = inputField.prev('.value-item').data('name');
      this.collection.findWhere({ name: oldName }).updateName(newName);
    } else {
      this.render();
    }
  },
  render: function() {
    this.$el.empty();
    this.collection.forEach(this.addValue, this);
  },
  addValue: function(valueItem) {
    var valueView = new ValueItemView({ model: valueItem }); // Should be a ValueItemView!
    this.$el.append(valueView.render().$el);
  },
  initialize: function() {
    this.collection.on('change', this.render, this);
    this.collection.on('add', this.render, this);
  },
  addItem: function(name) {
    var exists = this.collection.contains(function(v) {
      return v.get('name') === name
    });
    if (!exists) {
      var value = new Value({ id: name, name: name });
      this.collection.add(value);
    }
  }
});



$(document).ready(function() {

  var interactionBlock = $('#interaction-block');
  var newFeatureBlock = $('#new-feature-block');
  var newFeatureButton = $('#new-feature-button');

  newFeatureBlock.hide();

  newFeatureButton.on('click', function() {
    featureListView.render();
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
  valueListView.listenTo(
    featureList, 'update-success:add-target', function(target, featureType) {
      if (featureType === 'atomic') {
        valueListView.addItem(target);
      }
    });

  valueListView.render();
  $('#value-list').replaceWith(valueListView.$el);

  $('#feature-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    featureListView.collection.each(function(f) {
      if (!$.matches(f.get('name').toLowerCase(), currentInput)) {
        f.trigger('hide');
      } else {
        f.trigger('show');
      }
    });
  });

  $('#value-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    if (!currentInput) {
      featureListView.collection.each(function(f) { f.trigger('show') });
      valueListView.collection.each(function(v) { v.trigger('show') });
      return;
    }
    valueListView.collection.each(function(v) {
      var valueName = v.get('name');
      if (!$.matches(valueName.toLowerCase(), currentInput)) {
        v.trigger('hide');
      } else {
        v.trigger('show');
      }
    });
    featureListView.collection.each(function(f) {
      var targets = f.get('targets');
      if (f.get('type') === 'complex') {
        f.trigger('hide');
      } else if (_.filter(targets, function(t) {
        return t.indexOf(currentInput) !== -1;
      }).length === 0) {
        f.trigger('hide');
      } else {
        f.trigger('show');
      }
    });
  });

});
