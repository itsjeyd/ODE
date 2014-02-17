var Rule = Backbone.Model.extend({

  initialize: function() {
    this.urlRoot = '/rules';
  },

});

$(document).ready(function() {

  $('.rule-item').on('mouseenter', function() {
    $(this).addClass('highlighted');
    var controls = $('<span>').addClass('pull-right controls');
    controls.append($.editButton($(this).attr('id')));
    controls.append($.removeButton($(this).attr('id')));
    $(this).find('h2').append(controls);
    $('.edit-button').on('click', function(e) {
      var ruleID = $(e.currentTarget).parents('.rule-item').attr('id');
      window.location.href =
        document.URL + '/' + ruleID + '/input';
    });
    $('.remove-button').on('click', function(e) {
      var ruleID = $(e.currentTarget).parents('.rule-item').attr('id')
        .substring(1);
      var rule = new Rule({ id: ruleID });
      rule.destroy();
    });
  });

  $('.rule-item').on('mouseleave', function() {
    $(this).removeClass('highlighted');
    $(this).find('.controls').remove();
  });

});
