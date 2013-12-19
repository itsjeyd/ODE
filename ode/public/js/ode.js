$(document).ready(function() {

  $("#new-feature").hide();
  $(".edit-feature").hide();

  $("#new-feature-button").on("click", function(event) {
    event.preventDefault();
    $(this).hide();
    $("#interaction-block").html($("#new-feature").html());
  });

  $(".feature-item").on("click", function(event) {
    event.preventDefault();
    $("#interaction-block").html($("#"+$(this).text()).html());
    $("#new-feature-button").show();
  });

});
