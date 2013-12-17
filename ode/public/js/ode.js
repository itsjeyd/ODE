$(document).ready(function() {

  $("#feature-form").hide();

  $("#show-feature-form").on("click", function(event) {
    event.preventDefault();
    $("#feature-form").show();
    $(this).hide();
  });

});
