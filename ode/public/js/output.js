// Rules

var Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
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

});

var RuleView = Backbone.View.extend({

  initialize: function() {
    this.model.on({
      'change:name': function() {
        this._updateURL();
      },
      'change:description': function() {
        this._removeEditControls('description');
        this._renderDescription();
      },
      'update-error:name': function(msg) {
        this._renderAlert('button.name', msg);
      },
      'update-error:description': function(msg) {
        this._renderAlert('button.description', msg);
      },
    }, this);
  },

  render: function() {
    this.$el.empty();
    this.$el.append($.h3('').attr('id', 'rule-name'));
    this.$el.append($.p('').attr('id', 'rule-description'));
    this.$el.append($.div().attr('id', 'rule-rhs'));
    this._renderName();
    this._renderOutputButton();
    this._renderDescription();
    this._renderRHS();
    return this;
  },

  _renderName: function() {
    this.$('#rule-name').text('@' + this.model.get('name'));
  },

  _renderOutputButton: function() {
    var successButton = $.successButton('Show output')
      .addClass('pull-right output-button');
    this.$('#rule-name').append(successButton);
  },

  _renderDescription: function() {
    this.$('#rule-description').text(this.model.get('description'));
  },

  _renderRHS: function() {
    var rhsView = new RHSView({ model: this.model.get('rhs'),
                                el: this.$('#rule-rhs') });
    rhsView.render();
  },

  _updateURL: function() {
    window.location.replace(this.model.url() + '/output');
  },

  _renderAlert: function(button, msg) {
    var updateButton = this.$(button);
    updateButton.next('.alert-msg').remove();
    $.alertMsg(msg).insertAfter(updateButton);
  },

  events: {
    'dblclick #rule-name': function(e) {
      this._renderEditControls('name')(e)
    },
    'click button.name': function() {
      this._saveEdits('name')(this)
    },
    'dblclick #rule-description': function(e) {
      this._renderEditControls('description')(e)
    },
    'click button.description': function() {
      this._saveEdits('description')(this)
    },
    'click .output-button': function() {
      $('.modal-body').empty();
      var output = this.model.get('rhs').compileOutput();
      _.each(output, function(o) {
        var item = $('<p>').text(o);
        $('.modal-body').append(item);
      });
      $('.modal').modal();
    },
  },

  _renderEditControls: function(modelField) {
    return function(e) {
      var fieldToEdit = $(e.currentTarget);
      var currentValue = fieldToEdit.text();
      if (modelField === 'name') {
        currentValue = currentValue.substring(1);
      }
      var inputField = $.textInput().addClass(modelField)
        .val(currentValue);
      var okButton = $.okButton().addClass(modelField);
      fieldToEdit.hide();
      inputField.insertAfter(fieldToEdit);
      okButton.insertAfter(inputField);
      inputField.focus();
    };
  },

  _saveEdits: function(modelField) {
    return function(view) {
      var inputField = view.$('input.' + modelField);
      if (!inputField.isEmpty() &&
          inputField.val() !== view.model.get(modelField)) {
        var updateFunction = 'update' + $.cap(modelField);
        view.model[updateFunction](inputField.val());
      } else {
        view._removeEditControls(modelField);
        var renderFunction = '_render' + $.cap(modelField);
        view[renderFunction]();
      }
    };
  },

  _removeEditControls: function(modelField) {
    this.$('.' + modelField).remove();
    this.$('.alert-msg').remove();
    this.$('#rule-' + modelField).show();
  },

});


// Output: Models

var RHS = Backbone.Model.extend({

  initialize: function(attrs, options) {
    var groups = [];
    _.each(options.json.groups, function(g) {
      var group = new CombinationGroup({ id: g.uuid,
                                         position: g.position,
                                         ruleID: this.get('ruleID') },
                                       { json: g });
      groups.push(group);
    }, this);
    this.set('groups', new Backbone.Collection(groups,
                                               { comparator: 'position'}));
    if (this.get('groups').size() === 1) {
      this.get('groups').at(0).set('last', true);
    }
    this.get('groups').on({
      'add': function() {
        if (this.get('groups').size() === 2) {
          this.get('groups').findWhere({ last: true }).unset('last');
        }
      },
      'remove': function() {
        if (this.get('groups').size() === 1) {
          this.get('groups').at(0).set('last', true);
        }
      },
      'new:part': function(part) {
        this.trigger('new:part', part);
      },
    }, this);
  },

  compileOutput: function() {
    var output = [];
    this.get('groups').each(function(g) {
      output = output.concat(g.compileOutput());
    });
    return output;
  },

});

var Part = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups/' +
      this.get('groupID') + '/slots/' + this.get('slotID') + '/parts';
  },

});

var PartsInventory = Backbone.Collection.extend({

  model: Part,

});

var OutputString = Backbone.Model.extend({

  defaults: { tokens: [] },

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') +
      '/groups/' + this.get('groupID') + '/strings';
    this.on({
      'split': function(splitPoint) {
        this.set('splitPoint', splitPoint);
      },
    }, this);
  },

});

var CombinationGroup = Backbone.Model.extend({

  initialize: function(attrs, options) {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups';
    if (options.json) {
      var outputStrings = _.map(options.json.outputStrings, function(os) {
        return new OutputString({ id: os.uuid,
                                  tokens: os.tokens,
                                  content: os.content,
                                  ruleID: this.get('ruleID'),
                                  groupID: this.id });
      }, this);
      this.set('outputStrings', new Backbone.Collection(outputStrings));
      var slots = _.map(options.json.partsTable.slots, function(s) {
        var parts = _.map(s.parts, function(p) {
          return new Part({ id: p.uuid,
                            content: p.content,
                            ruleID: this.get('ruleID'),
                            groupID: this.id,
                            slotID: s.uuid });
        }, this);
        var refs = _.map(s.refs, function(r) {
          return new CrossRef({ id: r,
                                ruleID: this.get('ruleID'),
                                groupID: this.id,
                                slotID: s.uuid });
        }, this);
        return new Slot({ id: s.uuid,
                          position: s.position,
                          parts: new Backbone.Collection(parts),
                          refs: new Backbone.Collection(refs),
                          ruleID: this.get('ruleID'),
                          groupID: this.id });
      }, this);
      this.set('partsTable', new PartsTable({
        slots: new Backbone.Collection(slots, { comparator: 'position' }),
        ruleID: this.get('ruleID'),
        groupID: this.id,
      }));
    }
    this.get('outputStrings').on({
      'change:splitPoint': function(model) {
        this.get('outputStrings').get(model).destroy();
        this.get('partsTable').add(model);
        this.trigger('update');
      },
    }, this);
    this.get('partsTable').on({
      'new:part': function(part) {
        this.trigger('new:part', part);
      },
    }, this);
  },

  addOutputString: function(outputString) {
    this.get('outputStrings').push(outputString);
  },

  addStrings: function(strings) {
    var group = this;
    strings.each(function(os) {
      var string = new OutputString({ tokens: os.get('tokens'),
                                      content: os.get('tokens').join(' '),
                                      ruleID: group.get('ruleID'),
                                      groupID: group.id,
                                    });
      string.save(null, { wait: true,
                          success: function(model, response, options) {
                            group.get('outputStrings').add(model);
                            group.trigger('update');
                          }});
    });
  },

  addPartsTable: function(partsTable) {
    var group = this;
    var partsTableCopy = new PartsTable({
      slots: new Backbone.Collection([], { comparator: 'position' }),
      ruleID: group.get('ruleID'),
      groupID: group.id,
    });
    partsTable.get('slots').each(function(s) {
      var slot = new Slot({ position: s.get('position'),
                            parts: new Backbone.Collection([]),
                            refs: new Backbone.Collection([]),
                            ruleID: group.get('ruleID'),
                            groupID: group.id });
      slot.save(null, { wait: true,
                        success: function(model, response, options) {
                          partsTableCopy.get('slots').add(model);
                          s.get('parts').each(function(p) {
                            var part = new Part({
                              content: p.get('content'),
                              ruleID: group.get('ruleID'),
                              groupID: group.id,
                              slotID: model.id
                            });
                            part.save(null,
                                      { wait: true,
                                        success: function(
                                          model, response, options) {
                                          slot.get('parts').add(model);
                                          group.trigger('update');
                                        }});
                          });
                        }});
    });
    this.set('partsTable', partsTableCopy);
  },

  compileOutput: function() {
    return this.get('outputStrings').map(function(os) {
      return os.get('content');
    }).concat(this.get('partsTable').getFullStrings());
  },

});

var PartsTable = Backbone.Model.extend({

  initialize: function() {
    this.get('slots').on({
      'new:part': function(part) {
        this.trigger('new:part', part);
      },
    }, this);
  },

  add: function(outputString) {
    if (this.get('slots').size() === 0) {
      var tokens = outputString.get('tokens');
      var splitPoint = outputString.get('splitPoint');
      var leftTokens = tokens.slice(0, splitPoint).join(' ');
      var rightTokens = tokens.slice(splitPoint).join(' ');
      this._makeSlot(1, leftTokens);
      this._makeSlot(2, rightTokens);
    } else {
      this._addParts(outputString);
    }
  },

  _makeSlot: function(position, tokens) {
    var slot = new Slot({ position: position,
                          parts: new Backbone.Collection([]),
                          refs: new Backbone.Collection([]),
                          ruleID: this.get('ruleID'),
                          groupID: this.get('groupID'),
                        });
    var partsTable = this;
    return slot.save(null,
                     { wait: true,
                       success: function(model, response, options) {
                         partsTable.get('slots').add(model);
                         var part = partsTable._makePart(tokens, model);
                         var createdSlot = model;
                         part.save(null,
                                   { wait: true,
                                     success: function(
                                       model, response, options) {
                                       createdSlot.add(model);
                                     }});
                       }});
  },

  _addParts: function(outputString) {
    var tokens = outputString.get('tokens');
    var splitPoint = outputString.get('splitPoint');
    var firstSlot = this.get('slots').at(0);
    var secondSlot = this.get('slots').at(1);
    var leftPart = this._makePart(tokens.slice(0, splitPoint).join(' '),
                                  firstSlot);
    var rightPart = this._makePart(tokens.slice(splitPoint).join(' '),
                                   secondSlot);
    leftPart.save(null,
                  { wait: true,
                    success: function(model, response, options) {
                      firstSlot.add(model);
                    }});
    rightPart.save(null,
                   { wait: true,
                     success: function(model, response, options) {
                       secondSlot.add(model);
                     }});
  },

  _makePart: function(content, slot) {
    return new Part({
      content: content,
      ruleID: this.get('ruleID'),
      groupID: this.get('groupID'),
      slotID: slot.id,
    });
  },

  hasOptionalSlots: function() {
    return this.get('slots').size() > 2;
  },

  getFullStrings: function() {
    var parts = this.get('slots').map(function(s) {
      return s.get('parts').map(function(p) {
        return p.get('content');
      });
    });
    return this._cart(parts);
  },

  _cart: function(parts) {
    if (parts.length === 0) {
      return [];
    } else if (parts.length === 1) {
      return parts.pop();
    } else if (parts.length === 2) {
      return this._combineSlots(parts[0], parts[1]);
    } else {
      var intermediateResult = this._combineSlots(parts[0], parts[1]);
      var remainingSlots = parts.slice(2);
      return this._cart([intermediateResult].concat(remainingSlots));
    }
  },

  _combineSlots: function(slot1, slot2) {
    var partsTable = this;
    var acc = function(a, b, result) {
      if (a.length === 0) {
        return result
      } else {
        var intermediateResult = partsTable._combineStrings(a[0], b);
        return acc(a.slice(1), b, result.concat(intermediateResult));
      }
    };
    if (slot1.length === 0 && slot2.length === 0) {
      return [];
    } else {
      return acc(slot1, slot2, []);
    }
  },

  _combineStrings: function(string, slot) {
    var acc = function(str, slt, result) {
      if (slt.length === 0) {
        return result
      } else {
        var concatenatedString = str + ' ' + slt[0];
        return acc(str, slt.slice(1), result.concat([concatenatedString]));
      }
    }
    if (slot.length === 0) {
      return [str];
    } else {
      return acc(string, slot, []);
    }
  },

});

var Slot = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups/' +
      this.get('groupID') + '/slots';
    this.get('parts').on({
      'add': function(part) {
        this.trigger('new:part', part);
      },
    }, this);
  },

  add: function(part) {
    this.get('parts').add(part);
  },

  addRef: function(ref) {
    this.get('refs').add(ref);
  },

});

var CrossRef = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups/' +
      this.get('groupID') + '/slots/' + this.get('slotID') + '/refs';
  },

});


// Output: Views

var RHSView = Backbone.View.extend({

  initialize: function() {
    this.model.get('groups').on({
      'remove': function(group) {
        var position = group.get('position');
        var groupsToUpdate = this.model.get('groups').filter(function(g) {
          return g.get('position') > position;
        });
        _.each(groupsToUpdate, function(g) {
          g.save({ position: g.get('position') - 1}, { wait: true });
        });
      },
      'add': function(newGroup) {
        var groupView = new CombinationGroupView({ model: newGroup });
        var prevPos = newGroup.get('position') - 1;
        groupView.render().$el
          .insertAfter(this.$('[data-position="' + prevPos + '"]'));
        this.listenTo(groupView, 'added', this._addGroup);
        this.listenTo(groupView, 'copied', this._copyGroup);
      },
    }, this);
  },

  render: function() {
    this._renderGroups();
    return this;
  },

  _renderGroups: function() {
    this.model.get('groups').each(function(group) {
      var groupView = new CombinationGroupView({ model: group });
      this.$el.append(groupView.render().$el);
      this.listenTo(groupView, 'added', this._addGroup);
      this.listenTo(groupView, 'copied', this._copyGroup);
    }, this);
  },

  _addGroup: function(newGroup) {
    this._updateGroups(newGroup);
    var rhs = this.model;
    newGroup.save(null,
                  { wait: true,
                    success: function(model, response, options) {
                      model.get('partsTable').set('groupID', model.id);
                      rhs.get('groups').add(model);
                    }});
  },

  _copyGroup: function(existingGroup, newGroup) {
    this._updateGroups(newGroup);
    var outputStrings = existingGroup.get('outputStrings');
    var partsTable = existingGroup.get('partsTable');
    var rhs = this.model;
    newGroup.save(null,
                  { wait: true,
                    success: function(model, response, options) {
                      model.get('partsTable').set('groupID', model.id);
                      rhs.get('groups').add(model);
                      model.addStrings(outputStrings);
                      model.addPartsTable(partsTable);
                    }});
  },

  _updateGroups: function(newGroup) {
    var position = newGroup.get('position');
    var groupsToUpdate = this.model.get('groups').filter(function(g) {
      return g.get('position') >= position;
    });
    _.each(groupsToUpdate, function(g) {
      g.save({ position: g.get('position') + 1}, { wait: true });
    });
  },

});

var OutputStringView = Backbone.View.extend({

  className: 'output-string',

  initialize: function() {
    this.model.on({
      'remove': this.remove,
    }, this);
  },

  render: function() {
    var sepID = 1;
    var tokens = this.model.get('tokens');
    _.each(_.initial(tokens), function(t) {
      this.$el.append($.span('token').text(t));
      this.$el.append($.span('sep').data('ID', sepID++));
    }, this);
    this.$el.append($.span('token').text(_.last(tokens)));
    this._renderPlaceholder();
    this.$el.append($.removeButton().css('visibility', 'hidden'));
    return this;
  },

  _renderPlaceholder: function() {
    var placeholder = $.span('placeholder').text('...');
    this.$el.append(placeholder);
    var view = this;
    placeholder.droppable({
      accept: '.part',
      drop: function(e, ui) {
        var tokens = view.model.get('tokens');
        var partTokens = $(ui.helper).text().split(' ');
        _.each(partTokens, function(t) {
          tokens.push(t);
        });
        view.model.set('content', tokens.join(' '));
        view.model.save(null, { wait: true });
        view.$el.empty();
        view.render();
      },
    });
  },

  events: {
    'mouseenter': function() {
      this.$('.remove-button').css('visibility', 'visible');
    },
    'mouseleave': function() {
      this.$('.remove-button').css('visibility', 'hidden');
    },
    'click .sep': function(e) {
      this.model.trigger('split', $(e.currentTarget).data('ID'));
    },
    'click .remove-button': function() {
      this.model.destroy({ wait: true });
      this.remove();
    },
    'dblclick': '_renderEditControls',
    'click button': function() {
      this._updateModel();
      this.$el.empty();
      this.render();
    },
  },

  _renderEditControls: function(e) {
    if (!this.$('input').exists()) {
      var tokens = _.map(this.$('.token'), function(t) {
        return $(t).text();
      });
      var inputField = $.textInput().val(tokens.join(' '));
      var okButton = $.okButton();
      this.$('.token').remove();
      this.$('.sep').remove();
      this.$('.placeholder').remove();
      this.$('.remove-button').remove();
      this.$el.append(inputField);
      this.$el.append(okButton);
      inputField.focus();
    }
  },

  _updateModel: function() {
    var inputField = this.$('input');
    if (!inputField.isEmpty()) {
      var content = inputField.val();
      this.model.set('tokens', content.split(' '));
      this.model.set('content', content);
      this.model.save(null, { wait: true });
    }
  },

});

var CombinationGroupView = Backbone.View.extend({

  className: 'combination-group',

  attributes: function() {
    return {
      'data-position': this.model.get('position'),
    }
  },

  initialize: function() {
    this.model.on({
      'update': function() {
        this.$el.empty();
        this.render();
      },
      'change:position': function() {
        this.$el.attr('data-position', this.model.get('position'));
        this._updateHeader();
      },
    }, this);
    this.model.get('outputStrings').on({
      'remove': function(outputString) {
        outputString.trigger('remove');
      },
    }, this);
  },

  _updateHeader: function() {
    this.$('h4').remove();
    var groupHeader = $.h4('Group ' + this.model.get('position'));
    var small = $.small();
    small.append($.plusButton());
    small.append($.copyButton());
    small.append($.removeButton());
    groupHeader.append(small);
    groupHeader.insertBefore(this.$('.parts-table'));
  },

  render: function() {
    this._renderHeader();
    this._renderPartsTable();
    this._renderOutputStrings();
    this._renderPlaceholder();
    return this;
  },

  _renderHeader: function() {
    var groupHeader = $.h4('Group ' + this.model.get('position'));
    var small = $.small();
    small.append($.plusButton());
    small.append($.copyButton());
    small.append($.removeButton());
    groupHeader.append(small);
    this.$el.append(groupHeader);
  },

  _renderPartsTable: function() {
    this.$el.append(new PartsTableView({
      model: this.model.get('partsTable')
    }).render().$el);
  },

  _renderOutputStrings: function() {
    var list = $.div('output-strings');
    this.model.get('outputStrings').each(function(os) {
      list.append(new OutputStringView({ model: os }).render().$el);
    });
    this.$el.append(list);
  },

  _renderPlaceholder: function() {
    var placeholder = $.div('placeholder').text('Add more content ...');
    this.$el.append(placeholder);
    placeholder.makeEditable();
    var group = this.model;
    var groupView = this;
    placeholder.droppable({
      accept: '.part',
      drop: function(e, ui) {
        var content = $(ui.helper).text()
        var tokens = content.split(' ');
        var outputString = new OutputString({
          tokens: tokens,
          content: content,
          ruleID: group.get('ruleID'),
          groupID: group.id,
        });
        outputString.save(
          null,
          { success: function(model, response, options) {
            group.addOutputString(outputString);
            var outputStringView =
              new OutputStringView({ model: outputString });
            outputStringView.render().$el.insertBefore(placeholder);
            groupView._resetPlaceholder();
          }});
      },
    });
    this.$el.append($.addButton().css('visibility', 'hidden'));
  },

  events: {
    'click .plus-button': function(e) {
      var json = { outputStrings: [],
                   partsTable: { slots: [] } }
      var emptyGroup = new CombinationGroup(
        { position: this.model.get('position') + 1,
          ruleID: this.model.get('ruleID') },
        { json: json });
      this.trigger('added', emptyGroup);
    },
    'click .copy-button': function(e) {
      var json = { outputStrings: [],
                   partsTable: { slots: [] } }
      var groupCopy = new CombinationGroup(
        { position: this.model.get('position') + 1,
          ruleID: this.model.get('ruleID') },
        { json: json });
      this.trigger('copied', this.model, groupCopy);
    },
    'click .remove-button': function() {
      if (this.model.has('last')) {
        alert('You can not delete the last remaining group.');
      } else {
        var groupView = this;
        this.model.destroy({ wait: true,
                             success: function(model, response, options) {
                               groupView.remove();
                             }});
      }
    },
    'click .placeholder': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() === 'Add more content ...') {
        inputField.empty();
      }
    },
    'keyup .placeholder': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text()) {
        inputField.next('button').css('visibility', 'visible');
      } else {
        inputField.next('button').css('visibility', 'hidden');
      }
    },
    'click .placeholder + button': function(e) {
      var placeholder = $(e.currentTarget).prev('.placeholder');
      var tokens = placeholder.text().split(' ');
      var outputString = new OutputString({
        content: placeholder.text(),
        tokens: tokens,
        ruleID: this.model.get('ruleID'),
        groupID: this.model.id,
      });
      var group = this.model;
      var groupView =  this;
      outputString.save(
        null,
        { wait: true,
          success: function(model, response, options) {
            group.addOutputString(outputString);
            var outputStringView =
              new OutputStringView({ model: outputString });
            outputStringView.render().$el.insertBefore(placeholder);
            groupView._resetPlaceholder();
          }});
    },
  },

  _resetPlaceholder: function() {
    var placeholder = this.$el.children('.placeholder');
    placeholder.text('Add more content ...');
    placeholder.next('button').css('visibility', 'hidden');
  },

});

var PartsTableView = Backbone.View.extend({

  className: 'parts-table',

  initialize: function() {
    this.model.get('slots').on({
      'delete': function(slot) {
        slot.destroy({ wait: true });
        var slotsToUpdate = this.model.get('slots').filter(function(s) {
          return s.get('position') > slot.get('position');
        });
        _.each(slotsToUpdate, function(s) {
          s.set('position', s.get('position')-1);
        });
      },
      'add': function(slot) {
        var slots = this.$('.slots');
        var slotView = new SlotView({ model: slot });
        slotView.render().$el.insertBefore(slots.find('.controls'));
        slots.show();
      },
    }, this);
  },

  render: function() {
    this._renderSlots();
    this._renderControls();
    return this;
  },

  _renderSlots: function() {
    var slots = $.div('slots');
    this.model.get('slots').each(function(slot) {
      slots.append(new SlotView({ model: slot }).render().$el);
    });
    this.$el.append(slots);
    if (this.model.get('slots').size() === 0) {
      slots.hide();
    }
  },

  _renderControls: function() {
    var controls = $.div('controls');
    controls.append($.infoButton('Add slot'));
    this.$('.slots').append(controls);
  },

  events: {
    'click .btn': '_addSlot',
    'mouseenter h5': function(e) {
      if (this.model.hasOptionalSlots()) {
        $(e.currentTarget).find('.remove-button')
          .css('visibility', 'visible');
      }
    },
    'mouseleave h5': function(e) {
      if (this.model.hasOptionalSlots()) {
        $(e.currentTarget).find('.remove-button')
          .css('visibility', 'hidden');
      }
    },
  },

  _addSlot: function() {
    this.$('.controls').remove();
    var position = this.model.get('slots').size() + 1;
    var slot = new Slot({ position: position,
                          parts: new Backbone.Collection([]),
                          refs: new Backbone.Collection([]),
                          ruleID: this.model.get('ruleID'),
                          groupID: this.model.get('groupID'),
                        });
    var partsTable = this.model;
    var partsTableView = this;
    slot.save(null,
              { wait: true,
                success: function(model, response, options) {
                  partsTable.get('slots').add(slot);
                  var slotView = new SlotView({ model: slot });
                  partsTableView.$('.slots').append(slotView.render().$el);
                  partsTableView._renderControls();
                }});
},

});

var SlotView = Backbone.View.extend({

  className: 'slot',

  initialize: function() {
    this.model.get('parts').on({
      'add remove': function() {
        this.$el.empty();
        this.render();
      },
    }, this);
    this.model.get('refs').on({
      'add remove': function() {
        this.$el.empty();
        this.render();
      },
    }, this);
    this.model.on({
      'change:position': this._updateHeader,
    }, this);
  },

  _updateHeader: function() {
    this.$('h5').text('Slot ' + this.model.get('position'));
  },

  render: function() {
    this._renderHeader();
    this._renderLine();
    this._renderParts();
    this._renderRefs();
    this._renderPlaceholder();
    return this;
  },

  _renderHeader: function() {
    var slotHeader = $.h5('Slot ' + this.model.get('position'));
    slotHeader.append($.removeButton().css('visibility', 'hidden'));
    this.$el.append(slotHeader);
  },

  _renderLine: function() {
    this.$el.append($.hr());
  },

  _renderParts: function() {
    this.model.get('parts').each(function(p) {
      var part = $.div('part').text(p.get("content"));
      part.append($.removeButton().css('visibility', 'hidden'));
      this.$el.append(part);
    }, this);
  },

  _renderRefs: function() {
    this.model.get('refs').each(function(r) {
      var link = $('<a>').attr('href', '/rules/' + r.id + '/output')
        .text('@' + r.id);
      var ref = $.div('ref').html(link);
      ref.append($.removeButton().css('visibility', 'hidden'));
      this.$el.append(ref);
    }, this);
  },

  _renderPlaceholder: function() {
    var placeholder = $.div('placeholder').text('...');
    this.$el.append(placeholder);
    placeholder.makeEditable();
    var view = this;
    placeholder.droppable({
      accept: '.part',
      drop: function(e, ui) {
        var part = new Part({ content: $(ui.helper).text(),
                              ruleID: view.model.get('ruleID'),
                              groupID: view.model.get('groupID'),
                              slotID: view.model.id,
                            });
        part.save(null,
                  { success: function(model, response, options) {
                    view.model.add(part);
                  }});
      },
    });
  },

  events: {
    'click h5 > .remove-button': function() {
      this.model.trigger('delete', this.model);
      this.remove();
    },
    'mouseenter .part': function(e) {
      $(e.currentTarget).find('.remove-button').css('visibility', 'visible');
    },
    'mouseleave .part': function(e) {
      $(e.currentTarget).find('.remove-button').css('visibility', 'hidden');
    },
    'click .part > .remove-button': function(e) {
      e.stopPropagation();
      var parts = this.model.get('parts');
      var part = parts.findWhere({
        content: $(e.currentTarget).parent().text()
      });
      part.destroy({ wait: true });
    },
    'dblclick .part': '_renderEditControls',
    'mouseenter .ref': function(e) {
      $(e.currentTarget).find('.remove-button').css('visibility', 'visible');
    },
    'mouseleave .ref': function(e) {
      $(e.currentTarget).find('.remove-button').css('visibility', 'hidden');
    },
    'click .ref > .remove-button': function(e) {
      e.stopPropagation();
      var refs = this.model.get('refs');
      var ref = refs.get(
        $(e.currentTarget).prev().text().substring(1));
      ref.destroy({ wait: true });
    },
    'click .placeholder': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() === '...') {
        inputField.empty();
      }
    },
    'keydown .placeholder': function(e) {
      if (e.which === 13) {
        e.preventDefault();
      }
    },
    'keyup .placeholder': function(e) {
      if (e.which === 13) {
        var slot = this.model;
        var text = $(e.currentTarget).text();
        if (text.charAt(0) === '@') {
          var crossRef = new CrossRef({
            ruleName: text.substring(1),
            ruleID: slot.get('ruleID'),
            groupID: slot.get('groupID'),
            slotID: slot.id,
          });
          crossRef.save(null, { wait: true,
                                success: function(model, response, options) {
                                  slot.addRef(model);
                                },
                                error: function(model, xhr, options) {
                                  var response =
                                    $.parseJSON(xhr.responseText);
                                  alert(response.message);
                                }});
        } else {
          var part = new Part({
            content: text,
            ruleID: slot.get('ruleID'),
            groupID: slot.get('groupID'),
            slotID: slot.id,
          });
          part.save(null,
                    { wait: true,
                      success: function(model, response, options) {
                        slot.add(part);
                      }});
        }
      }
    },
    'keyup input': function(e) {
      if (e.which === 13) {
        var inputField = $(e.currentTarget);
        var newContent = inputField.val();
        var hiddenPart = inputField.prev('.part');
        var oldContent = hiddenPart.text();
        if (newContent && !(newContent === oldContent)) {
          var part = this.model.get('parts')
            .findWhere({ content: hiddenPart.text() });
          part.set('content', newContent);
          part.save(null, { wait: true });
          hiddenPart.text(newContent);
          hiddenPart.append($.removeButton().css('visibility', 'hidden'));
        }
        hiddenPart.show();
        inputField.remove();
      }
    },
  },

  _renderEditControls: function(e) {
    if (!this.$('input').exists()) {
      var part = $(e.currentTarget);
      var inputField = $.textInput().val(part.text());
      part.hide();
      inputField.insertAfter(part);
      inputField.focus();
    }
  },

});

var PartsInventoryView = Backbone.View.extend({

  initialize: function() {
    this.collection.on({
      'add': function(part) {
        this._renderPart(part)
      },
    }, this);
  },

  render: function() {
    this.$el.empty();
    this.collection.each(this._renderPart, this);
    return this;
  },

  _renderPart: function(part) {
    var partItemView = new PartItemView({ model: part });
    this.$el.append(partItemView.render().$el);
  },

  filterItems: function(input) {
    this.collection.each(function(i) {
      if (!$.matches(i.get('content').toLowerCase(), input)) {
        i.trigger('hide');
      } else {
        i.trigger('show');
      }
    });
  },

});

var PartItemView = Backbone.View.extend({

  className: 'part draggable',

  initialize: function() {
    this.model.on({
      'hide': function() { this.$el.hide(); },
      'show': function() { this.$el.show(); },
    }, this);
  },

  render: function() {
    this.$el.text(this.model.get('content'));
    this.$el.draggable({
      helper: 'clone',
      revert: 'invalid',
    });
    return this;
  }

});


// Application

$(document).ready(function() {

  // "Parts" sidebar

  var parts = $('.part');

  var partsInventory = new PartsInventory(
    _.map(parts, function(p) {
      return new Part({ content: $(p).data('content') });
    }),
    { comparator: 'content' }
  );

  var partsInventoryView = new PartsInventoryView({
    el: '#parts-list',
    collection: partsInventory,
  });
  partsInventoryView.render();

  $('#parts-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    partsInventoryView.filterItems(currentInput);
  });

  // Output Builder

  var name = $('#rule-name').text();
  var description = $('#rule-description').text();
  var rhsJSON = $('#rule-rhs').data('json');

  var rhs = new RHS({ ruleID: name }, { json: rhsJSON });

  var rule = new Rule({
    id: name,
    name: name,
    description: description,
    rhs: rhs,
  });

  var ruleView = new RuleView({
    model: rule,
    el: '#interaction-block',
  });
  ruleView.render();

  partsInventory.listenTo(rhs, 'new:part', function(part) {
    if (!this.findWhere({ content: part.get('content') })) {
      this.add(part);
    }
  });

  // Header

  var navbarButton = $('<li>');
  var navbarLink = $('<a>').attr('href', rule.url() + '/input')
    .text('InputBuilder');
  navbarButton.append(navbarLink);
  var browseButton = $('.navbar-right').children('li').first();
  navbarButton.insertBefore(browseButton);

});
