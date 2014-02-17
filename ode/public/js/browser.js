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

  });

  $('.rule-item').on('mouseleave', function() {
    $(this).removeClass('highlighted');
    $(this).find('.controls').remove();
  });

});
