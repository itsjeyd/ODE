$(document).ready(function() {

  function showButton(clickEvent) {
    clickEvent.preventDefault();
    $(this).off("click");
    button = $("<button>").addClass("btn btn-sm btn-info")
      .html("Update description");
    button.insertAfter($(this).parent());
    button.on("click", updateFeatureDescription);
  }

  function updateFeatureDescription(clickEvent) {
    clickEvent.preventDefault();
    feature = $(this).parent().find("h3").text();
    editBlock = $("#"+feature);
    descriptionField = $(this).parent().find(".description");
    newDescription = descriptionField.text();
    updateButton = $(this);
    route = jsRoutes.controllers.Features.updateFeatureDescription(feature);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "description": newDescription },
      success: function(result) {
        editBlock.find(".description").text(newDescription);
        updateButton.hide();
        alertBlock = $("<span>").addClass("text-success")
          .css("padding-left", "10px")
          .text("Description successfully updated!");
        alertBlock.insertAfter(descriptionField).fadeOut(5000);
      }
    });
    descriptionField.on("click", showButton);
  }

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
    $(".description").on("click", showButton);
    $("#new-feature-button").show();
  });

  $("#feature-filter").on("keyup", function(event) {
    // ...
  });

  $("#value-filter").on("keyup", function(event) {
    // ...
  });

});
