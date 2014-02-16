// Models

var Feature = Backbone.Model.extend({

  defaults: {
    targets: [],
  },

  initialize: function() {
    this.urlRoot = '/features'
  },

  validate: function(attrs, options) {
    if (attrs.name.split(/ +/).length > 1) {
      return 'Feature names can not contain whitespace.';
    }
  },

  _update: function(field, attrs, success) {
    this.save(attrs,
              { url: this.url() + '/' + field,
                wait: true,
                success: success,
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:' + field, response.message);
                },
              });
  },

  create: function(view) {
    this.save({},
              { wait: true,
                success: function(model, response, options) {
                  model.id = model.get('id');
                  view.trigger('create-success', model);
                },
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  view.trigger('create-error', response.message);
                },
              });
    return this;
  },

  updateName: function(newName) {
    var success = function(model, response, options) {
      model.id = newName;
    };
    this._update('name', { name: newName }, success);
  },

  updateDescription: function(newDescription) {
    var success = function(model, response, options) {};
    this._update('description', { description: newDescription }, success);
  },

  updateType: function(newType) {
    var success = function(model, response, options) {
      model.trigger('update-success:type', model);
      model.set({ targets: [] });
    };
    this._update('type', { type: newType }, success);
  },

  removeTarget: function(targetName) {
    var targets = _.without(this.get('targets'), targetName);
    var success = function(model, response, options) {
      model.set({ targets: targets });
      model.trigger('update-success:remove-target', targetName);
    };
    this._update(
      'targets', { target: targetName, action: 'REMOVE' }, success);
  },

  addTarget: function(targetName) {
    var targets = this.get('targets');
    if (_.contains(targets, targetName)) {
      this.trigger('update-error:add-target', 'Target already in list.');
    } else {
      targets.push(targetName);
      var success = function(model, response, options) {
        model.set({ targets: targets });
        model.trigger(
          'update-success:add-target', targetName, model.get('type'));
      };
      this._update(
        'targets', { target: targetName, action: 'ADD' }, success);
    }
  },

  del: function() {
    this.destroy({
      wait: true,
      success: function(model, response, options) {
        model.trigger('destroy', model);
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
    var oldName = this.get('name');
    this.save({ name: newName },
              { wait: true,
                success: function(model, response, options) {
                  model.id = newName;
                  model.trigger('update-success:name', oldName, newName);
                },
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:name', response.message);
                },
              });
  },

});



// Collections

var FeatureList = Backbone.Collection.extend({

  model: Feature,

  initialize: function() {
    this.on('change:name', function() {
      this.sort();
      this.trigger('update-success:name');
    }, this);
  },

  removeItem: function(name) {
    this.findWhere({ name: name }).del();
  },

  updateItems: function(target, newName) {
    var affectedFeatures = this.filter(function(f) {
      return _.contains(f.get('targets'), target);
    });
    _.each(affectedFeatures, function(f) {
      var newTargets = _.without(f.get('targets'), target);
      newTargets.push(newName);
      f.set({ targets: newTargets });
    });
  },

});

var ValueList = Backbone.Collection.extend({

  model: Value,

  renameItem: function(oldName, newName) {
    var nameTaken = this.findWhere({ name: newName });
    if (!nameTaken) {
      this.findWhere({ name: oldName }).updateName(newName);
    } else {
      nameTaken.trigger('update-error:name', 'Name already taken.');
    }
  },

  addItem: function(name) {
    var exists = this.contains(function(v) {
      return v.get('name') === name;
    });
    if (!exists) {
      this.add(new Value({ id: name, name: name }));
    }
  },

  removeIfOrphaned: function(valuesToCheck, featureList) {
    _.each(valuesToCheck, function(v) {
      var stillInUse = featureList.some(function(f) {
        return _.contains(f.get('targets'), v);
      });
      if (!stillInUse) {
        this._removeItem(v);
      }
    }, this);
  },

  _removeItem: function(name) {
    var item = this.findWhere({ name: name });
    this.remove(item);
  },

});



// Model views

var FeatureFormView = Backbone.View.extend({

  initialize: function() {
    this.on('create-success', function(model) {
      this.$el.empty();
      this.render();
      this.collection.add(model);
    }, this);
    this.on('create-error', function(msg) {
      this._renderAlert(msg);
    }, this);
  },

  _renderAlert: function(msg) {
    var createButton = this.$('button#create');
    createButton.next('.alert-msg').remove();
    $.alertMsg(msg).insertAfter(createButton);
  },

  render: function() {
    this._renderHeading();
    this._renderForm();
    return this;
  },

  _renderHeading: function() {
    this.$el.append($.h3('Create a new feature'));
  },

  _renderForm: function() {
    var form = $.form();
    var nameField = $.formGroup('name', 'fname');
    form.append(nameField);
    var descriptionField = $.formGroup('description', 'fdescription');
    form.append(descriptionField);
    var complexRadio = $.radio('type', 'complex', 'ftype');
    form.append(complexRadio);
    var atomicRadio = $.radio('type', 'atomic', 'ftype');
    form.append(atomicRadio);
    var createButton = $.createButton();
    form.append(createButton);
    this.$el.append(form);
  },

  events: {
    'click button#create': '_createFeature',
  },

  _createFeature: function(e) {
    e.preventDefault();
    var name = this.$('#fname').val();
    var description = this.$('#fdescription').val();
    var type = this.$('.ftype:checked').val();
    var feature = new Feature({ name: name,
                                description: description,
                                type: type,
                              });
    feature.create(this);
  },

});

var FeatureView = Backbone.View.extend({

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
    this.model.on('update-error:type', function(msg) {
      this._renderAlert('button.ftype', msg);
    }, this);
    this.model.on('update-error:add-target', function(msg) {
      this._renderAlert('button.ftarget', msg);
    }, this);
    this.model.on('destroy', function() { this.remove() }, this);
  },

  _renderAlert: function(button, msg) {
    var updateButton = this.$(button);
    updateButton.next('.alert-msg').remove();
    $.alertMsg(msg).insertAfter(updateButton);
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

  _activateTargetField: function() {
    var targetType;
    if (this.model.get('type') === 'complex') {
      targetType = '.feature-item';
    } else {
      targetType = '.value-item';
    }
    var viewModel = this.model;
    this.$('.droppable').droppable({
      accept: targetType,
      drop: function(e, ui) {
        var targetName = $(ui.helper).text();
        var targetField = $(this);
        targetField.next('button.ftarget').remove();
        targetField.text(targetName);
        var addButton = $.button('ftarget', 'Add');
        addButton.insertAfter(targetField);
      },
    });
  },

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
        this.$('button.ftype').remove();
        this._showTargets();
      } else {
        var okButton = $.button('ftype', 'OK');
        okButton.insertAfter(this.$('.radio').last());
        this._hideTargets();
      }
    },
    'click button.ftype': function(e) {
      e.preventDefault();
      this.model.updateType(this.$('input:checked').val());
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
      var targetName = this.$('.droppable').text();
      this.model.addTarget(targetName);
    },
    'click .editable': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() === 'Drop value here ...') {
        inputField.empty();
      }
      inputField.next('button.ftarget').remove();
      var addButton = $.button('ftarget', 'Add').disable();
      addButton.insertAfter(inputField);
    },
    'keyup .editable': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() !== '') {
        this.$('button.ftarget').enable();
      } else {
        this.$('button.ftarget').disable();
      }
    },
  },

  _renderEditControls: function(modelField) {
    return function(e) {
      var fieldToEdit = $(e.currentTarget);
      var inputField = $.input('f' + modelField).val(fieldToEdit.text());
      var okButton = $.button('f' + modelField, 'OK');
      fieldToEdit.hide();
      inputField.insertAfter(fieldToEdit);
      okButton.insertAfter(inputField);
      inputField.focus();
    };
  },

  _saveEdits: function(modelField) {
    return function(view) {
      var inputField = view.$('input.f' + modelField);
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

  _showTargets: function() {
    this.$('h4').show();
    this.$('.target').show();
    this.$('.droppable').show();
    this.$('button.ftarget').show();
    this.$('.alert-msg').show();
  },

  _hideTargets: function() {
    this.$('h4').hide();
    this.$('.target').hide();
    this.$('.droppable').hide();
    this.$('button.ftarget').hide();
    this.$('.alert-msg').hide();
  },

});


var ItemView = Backbone.View.extend({

  attributes: function() {
    return {
      id: this.model.id,
      'data-name': this.model.get('name'),
    }
  },

  initialize: function() {
    this.model.on('hide', function() { this.$el.hide(); }, this);
    this.model.on('show', function() { this.$el.show(); }, this);
  }

});


var FeatureItemView = ItemView.extend({

  className: 'feature-item draggable',

  initialize: function() {
    ItemView.prototype.initialize.apply(this);
  },

  render: function() {
    this.$el.text(this.model.get('name'));
    if (this.$el.hasClass('selected')) {
      var removeButton = $.removeButton(this.model.get('name'))
        .addClass('pull-right');
      this.$el.append(removeButton);
    }
    this.$el.draggable({
      helper: 'clone',
      revert: 'invalid',
    });
    return this;
  },

});


var ValueItemView = ItemView.extend({

  className: 'value-item draggable',

  initialize: function() {
    ItemView.prototype.initialize.apply(this);
    this.model.on('change', this.render, this);
    this.model.on('update-error:name', function(msg) {
      this._renderAlert(msg);
    }, this);
  },

  _renderAlert: function(msg) {
    var updateButton = this.$el.next().next();
    updateButton.next('.alert-msg').remove();
    $.alertMsg(msg).insertAfter(updateButton);
  },

  render: function() {
    this.$el.text(this.model.get('name'));
    this.$el.draggable({
      helper: 'clone',
      revert: 'invalid',
    });
    return this;
  },

});



// Collection views

var ListView = Backbone.View.extend({

  filterItems: function(input) {
    this.collection.each(function(i) {
      if (!$.matches(i.get('name').toLowerCase(), input)) {
        i.trigger('hide');
      } else {
        i.trigger('show');
      }
    });
  },

  showAll: function() {
    this.collection.each(function(i) { i.trigger('show') });
  },

});

var FeatureListView = ListView.extend({

  initialize: function() {
    this.collection.on('destroy', this.render, this);
    this.collection.on('add', this.render, this);
    this.collection.on('update-success:name', function() {
      var currentItems = this.$('.feature-item')
        .map(function() { return $(this).data('name') });
      var itemToSelect = this.collection.find(function(i) {
        return !_.contains(currentItems, i.get('name'))
      });
      this._renderWithSelection(itemToSelect);
    }, this);
  },

  _renderWithSelection: function(itemToSelect) {
    this.$('.feature-item').remove();
    this.collection.each(function(f) {
      var featureItemView = new FeatureItemView({ model: f });
      if (f.get('name') === itemToSelect.get('name')) {
        featureItemView.$el.addClass('selected');
      }
      featureItemView.render().$el
        .insertBefore(this.$('button#new-feature'));
    }, this);
  },

  render: function() {
    this.$('.feature-item').remove();
    this.collection.forEach(this._addFeatureItem, this);
  },

  _addFeatureItem: function(featureItem) {
    var featureItemView = new FeatureItemView({ model: featureItem });
    featureItemView.render().$el.insertBefore(this.$('button#new-feature'));
  },

  events: {
    'mouseenter .feature-item': '_highlight',
    'mouseleave .feature-item': '_unhighlight',
    'click .feature-item': '_dispatcher',
    'unselect': '_unselect',
    'click .remove-button': '_removeItem',
    'click button#new-feature': '_showForm',
  },

  _highlight: function(e) {
    $(e.currentTarget).addClass('highlighted');
  },

  _unhighlight: function(e) {
    $(e.currentTarget).removeClass('highlighted');
  },

  _dispatcher: function(e) {
    this._select(e); this._renderDeleteButton(e); this._showEditBlock(e);
  },

  _select: function(e) {
    this.$el.trigger('unselect');
    $(e.currentTarget).addClass('selected');
  },

  _renderDeleteButton: function(e) {
    var selectedFeature = $(e.currentTarget);
    selectedFeature.find('.remove-button').remove();
    var removeButton = $.removeButton(selectedFeature.text())
      .addClass('pull-right');
    $(e.currentTarget).append(removeButton);
  },

  _showEditBlock: function(e) {
    var featureID = e.currentTarget.id;
    var feature = this.collection.get(featureID);
    var featureView = new FeatureView({ model: feature });
    featureView.render();
    $('#interaction-block').html(featureView.$el);
  },

  _unselect: function() {
    var selected = this.$('.selected').removeClass('selected');
    selected.find('.remove-button').remove();
  },

  _removeItem: function(e) {
    var itemName = $(e.currentTarget).data('target');
    this.collection.removeItem(itemName);
  },

  _showForm: function() {
    this.$el.trigger('unselect');
    var formView = new FeatureFormView({ collection: this.collection });
    formView.render();
    $('#interaction-block').html(formView.$el);
  },

  updateItems: function(target, newName) {
    this.collection.updateItems(target, newName);
  },

  filterByValue: function(input) {
    this.collection.each(function(f) {
      if (f.get('type') === 'complex') {
        f.trigger('hide');
        return;
      }
      var targets = f.get('targets');
      if (_.every(targets, function(t) { return !$.matches(t, input) })) {
        f.trigger('hide');
      } else {
        f.trigger('show');
      }
    });
  },

});


var ValueListView = ListView.extend({

  initialize: function() {
    this.collection.on('change', this.render, this);
    this.collection.on('add', this.render, this);
    this.collection.on('remove', this.render, this);
  },

  render: function() {
    this.$el.empty();
    this.collection.forEach(this._addValueItem, this);
  },

  _addValueItem: function(valueItem) {
    var valueView = new ValueItemView({ model: valueItem });
    this.$el.append(valueView.render().$el);
  },

  events: {
    'dblclick .value-item': '_edit',
    'click button.vname': '_save',
  },

  _edit: function(e) {
    var fieldToEdit = $(e.currentTarget);
    var inputField = $.input('vname').val(fieldToEdit.text());
    var okButton = $.button('vname', 'OK');
    fieldToEdit.hide();
    inputField.insertAfter(fieldToEdit);
    okButton.insertAfter(inputField);
    inputField.focus();
  },

  _save: function(e) {
    var inputField = $(e.currentTarget).prev('input.vname');
    var newName = inputField.val();
    if (newName) {
      var oldName = inputField.prev('.value-item').data('name');
      this.collection.renameItem(oldName, newName);
    } else {
      this.render();
    }
  },

  addItem: function(name) {
    this.collection.addItem(name);
  },

  removeIfOrphaned: function(valuesToCheck, featureList) {
    this.collection.removeIfOrphaned(valuesToCheck, featureList);
  },

});



// Application

$(document).ready(function() {

  var interactionBlock = $('#interaction-block');


  // Instantiate collections

  var featureItems = $('.feature-item');
  var valueItems = $('.value-item');

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
  var valueList = new ValueList(
    _.map(valueItems, function(i) {
      var item = $(i);
      var name = item.data('name');
      return new Value({ id: name, name: name, });
    }),
    { comparator: 'name' }
  );


  // Instantiate views

  var featureListView = new FeatureListView({
    el: '#feature-list',
    collection: featureList,
  });
  featureListView.render();

  var valueListView = new ValueListView({
    id: 'value-list',
    collection: valueList,
  });
  valueListView.render();
  $('#value-list').replaceWith(valueListView.$el);


  // Set up event handlers

  featureListView.listenTo(
    valueList, 'update-success:name', function(target, newName) {
      featureListView.updateItems(target, newName);
  });

  valueListView.listenTo(
    featureList, 'destroy', function(destroyed) {
      valueListView.removeIfOrphaned(destroyed.get('targets'), featureList);
    });
  valueListView.listenTo(
    featureList, 'update-success:type', function(updated) {
      valueListView.removeIfOrphaned(
        updated.get('targets'), featureList.without(updated));
    });
  valueListView.listenTo(
    featureList, 'update-success:remove-target', function(target) {
      valueListView.removeIfOrphaned([target], featureList);
    });
  valueListView.listenTo(
    featureList, 'update-success:add-target', function(target, featureType) {
      if (featureType === 'atomic') {
        valueListView.addItem(target);
      }
    });

  $('#feature-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    featureListView.filterItems(currentInput);
  });

  $('#value-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    if (!currentInput) {
      featureListView.showAll();
      valueListView.showAll();
      return;
    }
    valueListView.filterItems(currentInput);
    featureListView.filterByValue(currentInput);
  });


});
