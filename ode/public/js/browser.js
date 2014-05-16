var Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
  },

  getNativeFormat: function() {
    var name = '// Name: @' + this.get('name') + '\n';
    var doc = '// Description: ' + this.get('description') + '\n';
    var nativeFormatLHS = this._nativeFormatLHS();
    var nativeFormatRHS = this._nativeFormatRHS();
    return $.when(nativeFormatLHS, nativeFormatRHS).then(function(x, y) {
      var nativeFormat = x + '\n->\n' + y + '.\n\n';
      return name + doc + nativeFormat;
    });
  },

  _nativeFormatLHS: function() {
    var lhs = new LHS({ ruleName: this.get('name') });
    var promise = $.when(lhs.fetch()).then(function(model) {
      return lhs.getNativeFormat();
    });
    return promise;
  },

  _nativeFormatRHS: function() {
    var rhs = new RHS({ ruleName: this.get('name') });
    var promise = $.when(rhs.fetch()).then(function(model) {
      return rhs.getNativeFormat();
    });
    return promise;
  }

});

var LHS = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleName') + '/lhs';
    this.set('defaultFormat', ':dvp ^ ');
  },

  getNativeFormat: function() {
    var pairs = this.get('json').pairs;
    var featureStrings = _.map(pairs, function(p) {
      return new Feature({ name: p.attribute.name,
                           type: p.attribute.type,
                           value: p.value }).getNativeFormat();
    });
    return this.get('defaultFormat') + featureStrings.join(' ^ ');
  },

});

var Feature = Backbone.Model.extend({

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
        return new Feature({ name: p.attribute.name,
                             type: p.attribute.type,
                             value: p.value }).getNativeFormat();
      });
      return '(' + embeddedFeatures.join(' ^ ') + ')';
    }
  },

});

var RHS = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules/' + this.get('ruleName') + '/rhs';
    this.set('defaultFormat', '# ^ :canned ^ <stringOutput>');
  },

  getNativeFormat: function() {
    var groups = this.get('json').groups;
    if (groups.length === 0) {
      return this.get('defaultFormat');
    } else if (groups.length === 1) {
      return this._nativeFormatSingleGroup(groups.pop());
    } else {
      return this._nativeFormatMultipleGroups(groups);
    }
  },

  _nativeFormatSingleGroup: function(group) {
    var nativeFormat = new CombinationGroup({ json: group })
      .getNativeFormat();
    if (nativeFormat) {
      return nativeFormat + '\n' + this.get('defaultFormat') + '###output';
    } else {
      return this.get('defaultFormat');
    }
  },

  _nativeFormatMultipleGroups: function(groups) {
    var counter = 1;
    var outputs = _.map(groups, function(g) {
      return new CombinationGroup({ json: g })
        .getNativeFormat('output' + counter++);
    });
    if (outputs && _.some(outputs, function(o) {
      return o !== '';
    })) {
      var variables = _.map(_.range(1, outputs.length+1), function(i) {
        return '###output' + i;
      });
      return outputs.join('\n') + '\n' +
        '###output = random(' + variables.join(', ') + ')\n' +
        this.get('defaultFormat') + '###output';
    } else {
      return this.get('defaultFormat');
    }
  },

});

var CombinationGroup = Backbone.Model.extend({

  initialize: function() {
    var json = this.get('json');
    this.set('outputStrings', json.outputStrings);
    this.set('slots', json.partsTable.slots.sort(function(s1, s2) {
      return s1.position - s2.position;
    }));
  },

  getNativeFormat: function(variableName) {
    var defaultFormat = '';
    var noOutputStrings = this._noOutputStrings();
    var slotsEmpty = this._slotsEmpty();
    if (noOutputStrings && slotsEmpty) {
      return defaultFormat;
    } else if (noOutputStrings) {
      return this._nativeFormatParts(variableName);
    } else if (slotsEmpty) {
      return this._nativeFormatStrings(variableName);
    } else {
      var x = this._nativeFormatStrings('x');
      var y = this._nativeFormatParts('y');
      return [x, y, '###' + (variableName || 'output') + ' = random(###x, ###y)'].join('\n');
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
      var contents = _.map(s.parts, function(p) {
        return p.content;
      });
      return '###part' + counter++ +
        ' = random("' + contents.join('", "') + '")';
    });
    var variables = _.map(_.range(1, parts.length+1), function(i) {
      return '###part' + i;
    });
    return parts.join('\n') + '\n' +
      '###' + (variableName || 'output') +
      ' = concatenate(' + variables.join(', ') + ')';
  },

  _nativeFormatStrings: function(variableName) {
    var strings = _.map(this.get('outputStrings'), function(os) {
      return os.content;
    });
    return '###' + (variableName || 'output') +
      ' = random("' + strings.join('", "') + '")';
  },

});

var RuleList = Backbone.Collection.extend({

  model: Rule,

  exportRules: function() {
    var promises = this.map(function(r) {
      return r.getNativeFormat();
    });
    return promises;
  },

});

var RuleItemView = Backbone.View.extend({

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
      '<h2>@<%= name %> <small><%= description %></h2>');
    var node = $(template(this.model.toJSON()));
    this.$el.append(node);
    return this;
  },

});

var RuleListView = Backbone.View.extend({

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
    var ruleItemView = new RuleItemView({ model: ruleItem });
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
    $(e.currentTarget).addClass('highlighted')
  },

  _unhighlight: function(e) {
    $(e.currentTarget).removeClass('highlighted')
  },

  _showControls: function(e) {
    var item = $(e.currentTarget);
    var controls = $('<span>').addClass('pull-right controls');
    controls.append($.similarButton(item.attr('id')));
    controls.append($.editInputButton(item.attr('id')));
    controls.append($.editOutputButton(item.attr('id')));
    controls.append($.removeButton(item.attr('id')));
    item.find('h2').append(controls);
  },

  _hideControls: function(e) {
    $(e.currentTarget).find('.controls').remove() },

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


$(document).ready(function() {

  var ruleItems = $('.rule-item');

  var ruleList = new RuleList(
    _.map(ruleItems, function(i) {
      var item = $(i);
      var id = item.attr('id');
      var name = item.data('name');
      var description = item.data('description');
      return new Rule({ id: name, name: name, description: description });
    }),
    { comparator: 'name' }
  );

  var ruleListView = new RuleListView({
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

      function all(arrayOfPromises) {
        return $.when.apply($, arrayOfPromises).then(function() {
          return Array.prototype.slice.call(arguments, 0);
        });
      };

      all(ruleList.exportRules()).then(function(content) {
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
