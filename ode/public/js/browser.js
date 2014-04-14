var Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
  },

});

var RuleList = Backbone.Collection.extend({

  model: Rule,

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
    'click .edit-button': '_goEdit',
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
    controls.append($.editButton(item.attr('id')));
    controls.append($.removeButton(item.attr('id')));
    item.find('h2').append(controls);
  },

  _hideControls: function(e) {
    $(e.currentTarget).find('.controls').remove() },

  _goEdit: function(e) {
    var ruleID = $(e.currentTarget).parents('.rule-item').attr('id')
    window.location.href = this.collection.get(ruleID).url() + '/input';
  },

  _delete: function(e) {
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

});
