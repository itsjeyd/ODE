$(document).ready(function() {

  $("#new-feature").hide();
  $(".edit-feature").hide();

  $("#new-feature-button").on("click", function(event) {
    event.preventDefault();
    $(this).hide();
    $(".alert").hide();
    $("#interaction-block").html($("#new-feature").html());
  });

  $(".feature-item").on("click", function(event) {
    event.preventDefault();
    $(".alert").hide();
    $("#interaction-block").html($("#"+$(this).text()).html());
    $("#new-feature-button").show();
  });

  $("#feature-filter").on("keyup", function(event) {
    // ...
  });

  $("#value-filter").on("keyup", function(event) {
    // ...
  });

});
