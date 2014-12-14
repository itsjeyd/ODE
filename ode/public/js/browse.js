var Browse = {};

Browse.Model = {};
Browse.Collection = {};
Browse.View = {};


Browse.Model.Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
  },

  withInputOutput: function() {
    var rule = this;
    var lhs = this._lhs();
    var rhs = this._rhs();
    return $.when(lhs, rhs).then(function(x, y) {
      rule.set('lhs', x);
      rule.set('rhs', y);
      return rule;
    });
  },

  _lhs: function() {
    var lhs = new Browse.Model.LHS({ ruleName: this.get('name') });
    return $.when(lhs.fetch()).then(function() {
      return lhs;
    });
  },

  _rhs: function() {
    var rhs = new Browse.Model.RHS({ ruleName: this.get('name') });
    var jqXHR = rhs.fetch({ success: function(model, response, options) {
      model.setCrossRefs();
    }});
    return $.when(jqXHR).then(function() {
      return rhs;
    });
  },

  setNativeFormat: function(processed) {
    var nativeFormat = {};
    nativeFormat.crossRefs = '';
    _.each(this.get('rhs').get('crossRefs'), function(r) {
      nativeFormat.crossRefs += processed[r].get('nativeFormat').crossRefs;
      nativeFormat.crossRefs += processed[r].get('nativeFormat').rhs;
    });
    nativeFormat.name = this._nativeFormatName();
    nativeFormat.desc = this._nativeFormatDesc();
    nativeFormat.lhs = this.get('lhs').getNativeFormat();
    nativeFormat.rhs = this.get('rhs').getNativeFormat();
    this.set('nativeFormat', nativeFormat);
  },

  getNativeFormat: function() {
    var nativeFormat = this.get('nativeFormat');
    var outputVar = nativeFormat.rhs ?
      '###' + this.get('name') + '_output' : '';
    var commentStart = '';
    var commentEnd = '';
    if (!nativeFormat.lhs || !nativeFormat.rhs) {
      commentStart = '/*\n';
      commentEnd = '\n*/';
    }
    return nativeFormat.name +
      nativeFormat.desc +
      commentStart +
      ':dvp ^ ' + nativeFormat.lhs +
      '\n->\n' +
      nativeFormat.crossRefs +
      nativeFormat.rhs +
      '# ^ :canned ^ <stringOutput>' + outputVar + '.' +
      commentEnd + '\n\n\n';
  },

  _nativeFormatName: function() {
    return '// Name: @' + this.get('name') + '\n';
  },

  _nativeFormatDesc: function() {
    return '// Description: ' + this.get('description') + '\n';
  },

});


Browse.Model.LHS = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleName') + '/lhs';
  },

  getNativeFormat: function() {
    var pairs = this.get('json').pairs;
    var featureStrings = _.map(pairs, function(p) {
      return new Browse.Model.Feature({
        name: p.attribute.name,
        type: p.attribute.type,
        value: p.value
      }).getNativeFormat();
    });
    return featureStrings.join(' ^ ');
  },

});


Browse.Model.Feature = Backbone.Model.extend({

  getNativeFormat: function() {
      return this._nativeFormatName() + this._nativeFormatValue();
  },

  _nativeFormatName: function() {
    return '<' + this.get('name') + '>';
  },

  _nativeFormatValue: function() {
    if (this.get('type') === 'atomic') {
      var value = this.get('value');
      return value === 'underspecified' ? '' : value;
    } else {
      var embeddedFeatures = _.map(this.get('value').pairs, function(p) {
        return new Browse.Model.Feature({
          name: p.attribute.name,
          type: p.attribute.type,
          value: p.value
        }).getNativeFormat();
      });
      if (embeddedFeatures.length > 0) {
        return '(' + embeddedFeatures.join(' ^ ') + ')';
      }
      return '';
    }
  },

});


Browse.Model.RHS = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleName') + '/rhs';
    this.set('outputVar', '###' + this.get('ruleName') + '_output');
  },

  setCrossRefs: function() {
    var slots = _.flatten(_.map(this.get('json').groups, function(g) {
      return g.partsTable.slots;
    }), true);
    var crossRefs = _.uniq(_.flatten(_.map(slots, function(slot) {
      return slot.refs;
    })));
    this.set('crossRefs', crossRefs);
  },

  getNativeFormat: function() {
    var groups = this.get('json').groups;
    if (groups.length === 0) {
      return '';
    } else if (groups.length === 1) {
      return this._nativeFormatSingleGroup(groups.pop());
    } else {
      return this._nativeFormatMultipleGroups(groups);
    }
  },

  _nativeFormatSingleGroup: function(group) {
    var nativeFormat = new Browse.Model.CombinationGroup({ json: group })
      .getNativeFormat(this.get('outputVar'));
    if (nativeFormat) {
      return nativeFormat + '\n';
    } else {
      return '';
    }
  },

  _nativeFormatMultipleGroups: function(groups) {
    var counter = 1;
    var outputs = _.map(groups, function(g) {
      return new Browse.Model.CombinationGroup({ json: g })
        .getNativeFormat('###output' + counter++);
    });
    if (outputs && _.some(outputs, function(o) {
      return o !== '';
    })) {
      var variables = _.map(_.range(1, outputs.length+1), function(i) {
        return '###output' + i;
      });
      return outputs.join('\n') + '\n' +
        this.get('outputVar') + ' = random(' + variables.join(', ') + ')\n';
    } else {
      return '';
    }
  },

});


Browse.Model.CombinationGroup = Backbone.Model.extend({

  initialize: function() {
    var json = this.get('json');
    this.set('outputStrings', json.outputStrings);
    this.set('slots', json.partsTable.slots.sort(function(s1, s2) {
      return s1.position - s2.position;
    }));
  },

  getNativeFormat: function(variableName) {
    var noOutputStrings = this._noOutputStrings();
    var slotsEmpty = this._slotsEmpty();
    if (noOutputStrings && slotsEmpty) {
      return '';
    } else if (noOutputStrings) {
      return this._nativeFormatParts(variableName);
    } else if (slotsEmpty) {
      return this._nativeFormatStrings(variableName);
    } else {
      var x = this._nativeFormatStrings('###x');
      var y = this._nativeFormatParts('###y');
      return [x, y, variableName + ' = random(###x, ###y)'].join('\n');
    }
  },

  _noOutputStrings: function() {
    return this.get('outputStrings').length === 0;
  },

  _slotsEmpty: function() {
    return _.every(this.get('slots'), function(s) {
      return s.parts.length === 0;
    });
  },

  _nativeFormatParts: function(variableName) {
    var counter = 1;
    var parts = _.map(this.get('slots'), function(s) {
      var refs = _.map(s.refs, function(r) {
        return '###' + r + '_output';
      }).join(', ');
      var contents = _.map(s.parts, function(p) {
        return p.content;
      }).join('", "');
      var choices = refs ? refs + ', "' + contents + '"' :
        '"' + contents + '"';
      return '###part' + counter++ + ' = random(' + choices + ')';
    });
    var variables = _.map(_.range(1, parts.length+1), function(i) {
      return '###part' + i;
    });
    return parts.join('\n') + '\n' +
      variableName + ' = concatenate(' + variables.join(', ') + ')';
  },

  _nativeFormatStrings: function(variableName) {
    var strings = _.map(this.get('outputStrings'), function(os) {
      return os.content;
    });
    return variableName + ' = random("' + strings.join('", "') + '")';
  },

});


Browse.Collection.RuleList = Backbone.Collection.extend({

  model: Browse.Model.Rule,

  all: function(arrayOfPromises) {
    return $.when.apply($, arrayOfPromises).then(function() {
      return Array.prototype.slice.call(arguments, 0);
    });
  },

  exportRules: function() {
    var promises = this.map(function(rule) {
      return rule.withInputOutput();
    });
    var ruleList = this;
    return this.all(promises).then(function(rules) {
      rules = rules.sort(function(r1, r2) {
        return r1.get('rhs').get('crossRefs').length -
          r2.get('rhs').get('crossRefs').length;
      });
      var nativeFormats = ruleList._nativeFormats(rules, {});
      return nativeFormats;
    });
  },

  _nativeFormats: function(rulesToProcess, processed) {
    if (rulesToProcess.length === 0) {
      var nativeFormats = [];
      for (var r in processed) {
        nativeFormats.push(processed[r].getNativeFormat());
      }
      return nativeFormats;
    }
    var stillToProcess = [];
    _.each(rulesToProcess, function(rule) {
      if (rule.get('rhs').get('crossRefs') === 0) {
        rule.setNativeFormat();
        processed[rule.get('name')] = rule;
      } else {
        if (_.every(rule.get('crossRefs'), function(r) {
          return r in processed;
        })) {
          rule.setNativeFormat(processed);
          processed[rule.get('name')] = rule;
        } else {
          stillToProcess.push(rule);
        }
      }
    });
    return this._nativeFormats(stillToProcess, processed);
  },

});


Browse.View.RuleItemView = Backbone.View.extend({

  className: 'rule-item',

  initialize: function() {
    this.model.on({
      'hide': function() { this.$el.hide(); },
      'show': function() { this.$el.show(); },
    }, this);
  },

  render: function() {
    this.$el.attr('id', this.model.get('id'));
    var template = _.template(
      ['<div class="col-md-10 rule-info">',
         '<span class="h2">',
           '@<%= name %> <small><%= description %></small>',
         '</span>',
       '</div>'].join('\n'));
    var node = $(template(this.model.toJSON()));
    this.$el.append(node);
    return this;
  },

});


Browse.View.RuleListView = Backbone.View.extend({

  initialize: function() {
    this.collection.on({
      'remove': this.render,
    }, this);
  },

  render: function() {
    this.$el.empty();
    this.collection.forEach(this._addRuleItem, this);
    return this;
  },

  _addRuleItem: function(ruleItem) {
    var ruleItemView = new Browse.View.RuleItemView({ model: ruleItem });
    this.$el.append(ruleItemView.render().$el);
  },

  events: {
    'mouseenter .rule-item': '_active',
    'mouseleave .rule-item': '_inactive',
    'click .rule-item': '_goDetails',
    'click .similar-button': '_goSimilar',
    'click .edit-input-button': '_goEditInput',
    'click .edit-output-button': '_goEditOutput',
    'click .remove-button': '_delete',
  },

  _active: function(e) { this._highlight(e); this._showControls(e); },

  _inactive: function(e) { this._unhighlight(e); this._hideControls(e); },

  _highlight: function(e) {
    $(e.currentTarget).addClass('highlighted');
  },

  _unhighlight: function(e) {
    $(e.currentTarget).removeClass('highlighted');
  },

  _showControls: function(e) {
    var item = $(e.currentTarget);
    if (!item.find('.controls').exists()) {
      var controls = $('<div>').addClass('col-md-2 controls');
      var span = $('<span>').addClass('h2 pull-right');
      span.append($.similarButton(item.attr('id'))
                  .tooltip({ placement: 'top',
                             title: 'View similar rules' }));
      span.append($.editInputButton(item.attr('id'))
                  .tooltip({ placement: 'top',
                             title: 'Edit input' }));
      span.append($.editOutputButton(item.attr('id'))
                  .tooltip({ placement: 'top',
                             title: 'Edit output' }));
      span.append($.removeButton(item.attr('id'))
                  .tooltip({ placement: 'top',
                             title: 'Delete rule' }));
      controls.append(span)
      controls.insertAfter(item.find('.rule-info'));
    } else {
      item.find('.controls').show();
    }
  },

  _hideControls: function(e) {
    $(e.currentTarget).find('.controls').hide() },

  _goSimilar: function(e) {
    e.stopPropagation();
    var ruleID = $(e.currentTarget).parents('.rule-item').attr('id');
    window.location.href = this.collection.get(ruleID).url() + '/similar';
  },

  _goDetails: function(e) {
    var ruleID = $(e.currentTarget).attr('id');
    window.location.href = this.collection.get(ruleID).url();
  },

  _goEditInput: function(e) {
    e.stopPropagation();
    var ruleID = $(e.currentTarget).parents('.rule-item').attr('id');
    window.location.href = this.collection.get(ruleID).url() + '/input';
  },

  _goEditOutput: function(e) {
    e.stopPropagation();
    var ruleID = $(e.currentTarget).parents('.rule-item').attr('id');
    window.location.href = this.collection.get(ruleID).url() + '/output';
  },

  _delete: function(e) {
    e.stopPropagation();
    var ruleID = $(e.currentTarget).parents('.rule-item').attr('id');
    var rule = this.collection.get(ruleID);
    rule.destroy({ wait: true,
                   error: function(model, xhr, options) {
                     alert('Rule can not be deleted. ' +
                           'It is referenced by at least one other rule.');
                   }});
  },

  filterItems: function(input) {
    this.collection.each(function(i) {
      if (!$.matches(i.get('name').toLowerCase(), input) &&
          !$.matches(i.get('description').toLowerCase(), input)) {
        i.trigger('hide');
      } else {
        i.trigger('show');
      }
    });
  },

});



// Application

$(document).ready(function() {

  $('a#browse').parent("li").addClass('active');

  var ruleItems = $('.rule-item');

  var ruleList = new Browse.Collection.RuleList(
    _.map(ruleItems, function(i) {
      var item = $(i);
      var id = item.attr('id');
      var name = item.data('name');
      var description = item.data('description');
      return new Browse.Model.Rule({
        id: name, name: name, description: description
      });
    }),
    { comparator: 'name' }
  );

  var ruleListView = new Browse.View.RuleListView({
    el: '#rule-list',
    collection: ruleList,
  });
  ruleListView.render();

  $('#rule-filter').on('keyup', function() {
    var currentInput = $(this).val().toLowerCase();
    ruleListView.filterItems(currentInput);
  });

  $('#export-button').on('click', function(e) {
    var button = $(e.currentTarget);
    if (!button.attr('href')) {

      ruleList.exportRules().then(function(content) {
        var blob = new Blob(content, { type : 'text/plain' });
        var url = (window.URL || window.webkitURL).createObjectURL(blob);
        button.attr('href', url);
        button[0].click();
      });

    } else {
      return true;
    }
  });

});
