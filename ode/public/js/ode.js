$(document).ready(function() {

  $("#interaction-block").hide();
  $("#new-feature").hide();
  $(".edit-feature").hide();

  $("#new-feature-button").on("click", function(event) {
    event.preventDefault();
    $(this).hide();
    $("#interaction-block").html($("#new-feature").html());
    $("#interaction-block").show();
  });

  $(".feature-item").on("click", function(event) {
    event.preventDefault();
    $("#interaction-block").html($("#"+$(this).text()).html());
    $("#interaction-block").show();
    $("#new-feature-button").show();
  });

});
