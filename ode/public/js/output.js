// output.js --- Client-side code for OutputBuilder.

// Copyright (C) 2013-2015  Tim Krones

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

var Output = {};

Output.Model = {};
Output.Collection = {};
Output.View = {};


// Rules

Output.Model.Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
  },

  _update: function(field, attrs) {
    this.save(attrs,
              { url: this.url() + '/' + field,
                wait: true,
                error: function(model, xhr, options) {
                  var response = $.parseJSON(xhr.responseText);
                  model.trigger('update-error:' + field, response.message);
                },
              });
  },

  updateName: function(newName) {
    this._update('name', { name: newName });
  },

  updateDescription: function(newDescription) {
    this._update('description', { description: newDescription });
  },

});


Output.View.RuleView = Backbone.View.extend({

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
    this._renderDescription();
    this._renderRHS();
    return this;
  },

  _renderName: function() {
    this.$('#rule-name').text('@' + this.model.get('name'));
    this.$('#rule-name').tooltip({
      title: 'Double-click to edit',
      placement: 'left',
      delay: { show: 250, hide: 0 },
    });
    this._renderSimilarRulesButton();
    this._renderOutputButton();
  },

  _renderSimilarRulesButton: function() {
    var successButton = $.successButton('Similar rules')
      .addClass('pull-right').attr('id', 'similar-button');
    this.$('#rule-name').append(successButton);
  },

  _renderOutputButton: function() {
    var successButton = $.successButton('Show output')
      .addClass('pull-right').attr('id', 'output-button');
    this.$('#rule-name').append(successButton);
  },

  _renderDescription: function() {
    this.$('#rule-description').text(this.model.get('description'));
    this.$('#rule-description').tooltip({
      title: 'Double-click to edit',
      placement: 'left',
      delay: { show: 250, hide: 0 },
    });
  },

  _renderRHS: function() {
    var rhsView = new Output.View.RHSView({
      model: this.model.get('rhs'),
      el: this.$('#rule-rhs')
    });
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
    'click #similar-button': '_goSimilar',
    'click #output-button': function() {
      var modal = $('#show-output-modal');
      var modalBody = modal.find('.modal-body')
      modalBody.empty();
      var output = this.model.get('rhs').compileOutput();
      _.each(output, function(o) {
        var item = $('<p>').text(o);
        modalBody.append(item);
      });
      modal.modal();
    },
  },

  _goSimilar: function() {
    window.location.href = this.model.url() + '/similar';
  },

  _renderEditControls: function(modelField) {
    return function(e) {
      var fieldToEdit = $(e.currentTarget);
      var currentValue = fieldToEdit.textOnly();
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



// Input: Models

Output.Model.AbbreviatedLHS = Backbone.Model.extend({

  initialize: function(attrs, options) {
    var pairs = [];
    _.each(options.json.pairs, function(p) {
      var pair = new Output.Model.AbbreviatedPair({
        attribute: p.attribute.name
      });
      if (p.attribute.type === 'atomic') {
        pair.set('value', p.value);
      } else {
        pair.set('value', '[...]');
      }
      pairs.push(pair);
    });
    this.set('pairs', new Backbone.Collection(pairs));
  },

});

Output.Model.AbbreviatedPair = Backbone.Model.extend({});



// Input: Views

Output.View.AbbreviatedLHSView = Backbone.View.extend({

  initialize: function() {
    this.on({
      'rendered': this._renderBrackets,
    });
  },

  render: function() {
    this._renderContent();
    return this;
  },

  _renderContent: function() {
    var content = $.div('content');
    this.model.get('pairs').each(function(pair) {
      content.append(this._renderPair(pair));
    }, this);
    this.$el.append(content);
  },

  _renderPair: function(pair) {
    return new Output.View.AbbreviatedPairView({ model: pair })
      .render().$el;
  },

  _renderBrackets: function() {
    var leftBracket = this._makeBracket('left');
    var rightBracket = this._makeBracket('right');
    var h = this.$('.content').height();
    leftBracket.insertBefore(this.$('.content'));
    rightBracket.insertAfter(this.$('.content'));
    leftBracket.height(h);
    rightBracket.height(h);
  },

  _makeBracket: function(type) {
    return $.div('bracket bracket-' + type);
  },

});


Output.View.AbbreviatedPairView = Backbone.View.extend({

  render: function() {
    this._renderAttr();
    this._renderValue();
    return this;
  },

  _renderAttr: function() {
    var attr = $.span('attribute').text(this.model.get('attribute'));
    this.$el.append(attr);
  },

  _renderValue: function() {
    var val = $.span('value').text(this.model.get('value'));
    this.$el.append(val);
  },

});



// Output: Models

Output.Model.RHS = Backbone.Model.extend({

  initialize: function(attrs, options) {
    this.set('uuid', options.json.uuid);
    var groups = [];
    _.each(options.json.groups, function(g) {
      var group = new Output.Model.CombinationGroup(
        { id: g.uuid,
          position: g.position,
          ruleID: this.get('ruleID'),
          rhsID: this.get('uuid'), },
        { json: g }
      );
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


Output.Model.Part = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups/' +
      this.get('groupID') + '/slots/' + this.get('slotID') + '/parts';
  },

});


Output.Collection.PartsInventory = Backbone.Collection.extend({

  model: Output.Model.Part,

});


Output.Model.OutputString = Backbone.Model.extend({

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


Output.Model.CombinationGroup = Backbone.Model.extend({

  initialize: function(attrs, options) {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups';
    if (options.json) {
      var outputStrings = _.map(options.json.outputStrings, function(os) {
        return new Output.Model.OutputString({
          id: os.uuid,
          tokens: os.tokens,
          content: os.content,
          ruleID: this.get('ruleID'),
          groupID: this.id
        });
      }, this);
      this.set('outputStrings', new Backbone.Collection(outputStrings));
      var slots = _.map(options.json.partsTable.slots, function(s) {
        var parts = _.map(s.parts, function(p) {
          return new Output.Model.Part({
            id: p.uuid,
            content: p.content,
            ruleID: this.get('ruleID'),
            groupID: this.id,
            slotID: s.uuid
          });
        }, this);
        var refs = _.map(s.refs, function(r) {
          return new Output.Model.CrossRef({
            id: r,
            ruleID: this.get('ruleID'),
            groupID: this.id,
            slotID: s.uuid
          });
        }, this);
        return new Output.Model.Slot({
          id: s.uuid,
          position: s.position,
          parts: new Backbone.Collection(parts),
          refs: new Backbone.Collection(refs),
          ruleID: this.get('ruleID'),
          groupID: this.id
        });
      }, this);
      this.set('partsTable', new Output.Model.PartsTable({
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
      var string = new Output.Model.OutputString({
        tokens: os.get('tokens'),
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
    var partsTableCopy = new Output.Model.PartsTable({
      slots: new Backbone.Collection([], { comparator: 'position' }),
      ruleID: group.get('ruleID'),
      groupID: group.id,
    });
    partsTable.get('slots').each(function(s) {
      var slot = new Output.Model.Slot({
        position: s.get('position'),
        parts: new Backbone.Collection([]),
        refs: new Backbone.Collection([]),
        ruleID: group.get('ruleID'),
        groupID: group.id
      });
      slot.save(null, { wait: true,
                        success: function(model, response, options) {
                          partsTableCopy.get('slots').add(model);
                          s.get('parts').each(function(p) {
                            var part = new Output.Model.Part({
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


Output.Model.PartsTable = Backbone.Model.extend({

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
      var partsTable = this;
      var firstSlotDone = this._makeSlot(1, leftTokens);
      $.when(firstSlotDone).then(function() {
        partsTable._makeSlot(2, rightTokens);
      });
    } else {
      this._addParts(outputString);
    }
  },

  _makeSlot: function(position, tokens) {
    var slot = new Output.Model.Slot({
      position: position,
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
    return new Output.Model.Part({
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
    return cart(parts);
  },

});


Output.Model.Slot = Backbone.Model.extend({

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


Output.Model.CrossRef = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleID') + '/groups/' +
      this.get('groupID') + '/slots/' + this.get('slotID') + '/refs';
  },

});



// Output: Views

Output.View.RHSView = Backbone.View.extend({

  initialize: function() {
    this.model.get('groups').on({
      'remove': function(group) {
        var position = group.get('position');
        var groupsToUpdate = this.model.get('groups').filter(function(g) {
          return g.get('position') > position;
        });
        _.each(groupsToUpdate, function(g) {
          g.save({ position: g.get('position')-1 }, { wait: true });
        });
      },
      'add': function(newGroup) {
        var groupView = new Output.View.CombinationGroupView({
          model: newGroup
        });
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
      var groupView = new Output.View.CombinationGroupView({ model: group });
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


Output.View.OutputStringView = Backbone.View.extend({

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
      this.$el.append(
        $.span('sep').data('ID', sepID++)
          .tooltip({ placement: 'top',
                     title: 'Click to split',
                     delay: { show: 250, hide: 0 }, }));
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
      tolerance: 'pointer',
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
      if (content !== this.model.get('content')) {
        this.model.set('tokens', content.split(' '));
        this.model.set('content', content);
        this.model.save(null, { wait: true });
      }
    }
  },

});


Output.View.CombinationGroupView = Backbone.View.extend({

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
    small.append($.plusButton().tooltip({ placement: 'top',
                                          title: 'Add group' }));
    small.append($.copyButton().tooltip({ placement: 'top',
                                          title: 'Copy group' }));
    small.append($.removeButton().tooltip({ placement: 'top',
                                          title: 'Delete group' }));
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
    small.append($.plusButton().tooltip({ placement: 'top',
                                          title: 'Add group' }));
    small.append($.copyButton().tooltip({ placement: 'top',
                                          title: 'Copy group' }));
    small.append($.removeButton().tooltip({ placement: 'top',
                                            title: 'Delete group' }));
    groupHeader.append(small);
    this.$el.append(groupHeader);
  },

  _renderPartsTable: function() {
    this.$el.append(new Output.View.PartsTableView({
      model: this.model.get('partsTable')
    }).render().$el);
  },

  _renderOutputStrings: function() {
    var list = $.div('output-strings');
    this.model.get('outputStrings').each(function(os) {
      list.append(new Output.View.OutputStringView({
        model: os
      }).render().$el);
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
      tolerance: 'pointer',
      drop: function(e, ui) {
        var content = $(ui.helper).text();
        var tokens = content.split(' ');
        var outputString = new Output.Model.OutputString({
          tokens: tokens,
          content: content,
          ruleID: group.get('ruleID'),
          groupID: group.id,
        });
        outputString.save(
          null,
          { wait: true,
            success: function(model, response, options) {
              group.addOutputString(outputString);
              var outputStringView =
                new Output.View.OutputStringView({ model: outputString });
              outputStringView.render().$el.insertBefore(placeholder);
              groupView._resetPlaceholder();
            },
          });
      },
    });
    this.$el.append($.addButton().css('visibility', 'hidden'));
  },

  events: {
    'click .plus-button': function(e) {
      var json = { outputStrings: [],
                   partsTable: { slots: [] } }
      var emptyGroup = new Output.Model.CombinationGroup(
        { position: this.model.get('position') + 1,
          ruleID: this.model.get('ruleID'),
          rhsID: this.model.get('rhsID'), },
        { json: json });
      this.trigger('added', emptyGroup);
    },
    'click .copy-button': function(e) {
      var json = { outputStrings: [],
                   partsTable: { slots: [] } }
      var groupCopy = new Output.Model.CombinationGroup(
        { position: this.model.get('position') + 1,
          ruleID: this.model.get('ruleID'),
          rhsID: this.model.get('rhsID'), },
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
    'paste .placeholder': function(e) {
      var inputField = $(e.currentTarget);
      if (inputField.text() === 'Add more content ...') {
        inputField.empty();
      }
      inputField.next('button').css('visibility', 'visible');
    },
    'click .placeholder + button': function(e) {
      var button = $(e.currentTarget);
      button.next('.alert-msg').remove();
      var placeholder = button.prev('.placeholder');
      var tokens = placeholder.text().split(' ');
      var outputString = new Output.Model.OutputString({
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
              new Output.View.OutputStringView({ model: outputString });
            outputStringView.render().$el.insertBefore(placeholder);
            groupView._resetPlaceholder();
          },
          error: function(model, xhr, options) {
            var response = $.parseJSON(xhr.responseText);
            $.alertMsg(response.message).insertAfter(button);
          }});
    },
    'blur .placeholder': function(e) {
      var placeholder = $(e.currentTarget);
      if (!placeholder.text()) {
        this._resetPlaceholder();
      }
    },
  },

  _resetPlaceholder: function() {
    var placeholder = this.$el.children('.placeholder');
    placeholder.text('Add more content ...');
    placeholder.next('button').css('visibility', 'hidden');
  },

});


Output.View.PartsTableView = Backbone.View.extend({

  className: 'parts-table',

  initialize: function() {
    this.model.get('slots').on({
      'delete': function(slot) {
        slot.destroy({ wait: true });
        var slotsToUpdate = this.model.get('slots').filter(function(s) {
          return s.get('position') > slot.get('position');
        });
        _.each(slotsToUpdate, function(s) {
          s.save({ position: s.get('position')-1 }, { wait: true });
        });
      },
      'add': function(slot) {
        this.$('.controls').remove();
        var slots = this.$('.slots');
        var slotView = new Output.View.SlotView({ model: slot });
        slots.append(slotView.render().$el);
        this._renderControls();
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
      slots.append(new Output.View.SlotView({ model: slot }).render().$el);
    });
    this.$el.append(slots);
    if (this.model.get('slots').size() === 0) {
      slots.hide();
    }
  },

  _renderControls: function() {
    var controls = $.div('controls');
    controls.append($.h5('Add slot ...').css('visibility', 'hidden'));
    this.$('.slots').append(controls);
  },

  events: {
    'mouseenter .controls': function(e) {
      $(e.currentTarget).find('h5').css('visibility', 'visible');
    },
    'mouseleave .controls': function(e) {
      $(e.currentTarget).find('h5').css('visibility', 'hidden');
    },
    'click .controls': '_addSlot',
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
    var position = this.model.get('slots').size() + 1;
    var slot = new Output.Model.Slot({
      position: position,
      parts: new Backbone.Collection([]),
      refs: new Backbone.Collection([]),
      ruleID: this.model.get('ruleID'),
      groupID: this.model.get('groupID'),
    });
    var partsTable = this.model;
    slot.save(null,
              { wait: true,
                success: function(model, response, options) {
                  partsTable.get('slots').add(slot);
                }});
},

});


Output.View.SlotView = Backbone.View.extend({

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
      tolerance: 'pointer',
      drop: function(e, ui) {
        var part = new Output.Model.Part({
          content: $(ui.helper).text(),
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
          var crossRef = new Output.Model.CrossRef({
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
          var part = new Output.Model.Part({
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
    'blur .placeholder': function(e) {
      var placeholder = $(e.currentTarget);
      if (!placeholder.text()) {
        placeholder.text('...');
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


Output.View.PartsInventoryView = Backbone.View.extend({

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
    var partItemView = new Output.View.PartItemView({ model: part });
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

  events: {
    'mouseenter .part': '_highlight',
    'mouseleave .part': '_unhighlight',
  },

  _highlight: function(e) {
    $(e.currentTarget).addClass('highlighted');
  },

  _unhighlight: function(e) {
    $(e.currentTarget).removeClass('highlighted');
  },

});


Output.View.PartItemView = Backbone.View.extend({

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
      revert: 'invalid',
      appendTo: 'body',
      zIndex: 1,
      cursor: 'pointer',
      helper: function(event) {
        return $('<div>').addClass('in-motion').text($(this).text());
      },
    });
    return this;
  }

});



// Application

$(document).ready(function() {

  // "Parts" sidebar

  var parts = $('.part');

  var partsInventory = new Output.Collection.PartsInventory(
    _.map(parts, function(p) {
      return new Output.Model.Part({ content: $(p).data('content') });
    }),
    { comparator: 'content' }
  );

  var partsInventoryView = new Output.View.PartsInventoryView({
    el: '#parts-list',
    collection: partsInventory,
  });
  partsInventoryView.render();

  $('#parts-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    partsInventoryView.filterItems(currentInput);
  });

  // Output Builder

  var lhsJSON = $('#rule-lhs').data('json');
  var lhs = new Output.Model.AbbreviatedLHS(null, { json: lhsJSON });
  var lhsView = new Output.View.AbbreviatedLHSView({
    model: lhs,
    el: '#rule-lhs',
  });
  lhsView.render();
  lhsView.trigger('rendered');

  var name = $('#rule-name').text();
  var description = $('#rule-description').text();
  var uuid = $('#rule-name').data('uuid');
  var rhsJSON = $('#rule-rhs').data('json');

  var rhs = new Output.Model.RHS({ ruleID: name }, { json: rhsJSON });

  var rule = new Output.Model.Rule({
    id: name,
    name: name,
    description: description,
    uuid: uuid,
    rhs: rhs,
  });

  var ruleView = new Output.View.RuleView({
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
