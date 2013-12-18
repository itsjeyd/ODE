$(document).ready(function() {

  $("#feature-form").hide();
  $("#feature-properties").hide();

  $("#new-feature-button").on("click", function(event) {
    event.preventDefault();
    $("#feature-form").show();
    $(this).hide();
    $("#feature-properties").hide();
  });

  $(".feature-item").on("click", function(event) {
    $("#feature-form").hide();
    $("#feature-properties h3").text($(this).text());
    $("#feature-properties form").prop(
      "action", "/feature/" + $(this).text());
    $("#type-complex").removeProp("checked");
    $("#type-atomic").removeProp("checked");
    if ($(this).data("ftype") === "complex") {
      $("#type-complex").prop("checked", "checked");
      $("#permitted-values").hide();
      $("#permitted-features").show();
    } else {
      $("#type-atomic").prop("checked", "checked");
      $("#permitted-features").hide();
      $("#permitted-values").show();
    }
    $("#feature-properties").show();
    $("#new-feature-button").show();
  });

});
