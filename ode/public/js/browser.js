$(document).ready(function() {

  $('.rule-item').on('mouseenter', function() {
    $(this).addClass('highlighted');
  });

  $('.rule-item').on('mouseleave', function() {
    $(this).removeClass('highlighted');
  });

  $('.rule-item').on('click', function(e) {
    alert(e.currentTarget.id);
  });

});
