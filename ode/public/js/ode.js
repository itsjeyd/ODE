$(document).ready(function() {

  function showUpdateNameButton(clickEvent) {
    clickEvent.preventDefault();
    $(this).off("click");
    button = $("<button>").addClass("btn btn-sm btn-info")
      .html("Update name");
    button.insertAfter($(this).parent());
    button.on("click", updateFeatureName);
  }

  function updateFeatureName(clickEvent) {
    clickEvent.preventDefault();
    nameField = $(this).parent().find(".name");
    oldName = nameField.data("name");
    newName = nameField.text();
    featureItem = $("a[href='#" + oldName + "']");
    editBlock = $("#"+oldName);
    updateButton = $(this);
    route = jsRoutes.controllers.Features.updateFeatureName(oldName);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "name": newName },
      statusCode: {
        200: function() {
          featureItem.attr("href", "#"+newName)
            .text(newName);
          editBlock.attr("id", newName)
            .find(".name").data("name", newName).text(newName);
          updateButton.hide();
          alertBlock = $("<span>").addClass("text-success")
            .css("padding-left", "10px")
            .text("Name successfully updated!");
          nameField.append(alertBlock);
          alertBlock.fadeOut(5000);
        },
        400: function() {
          nameField.text(oldName);
          updateButton.hide();
          alertBlock = $("<span>").addClass("text-danger")
            .css("padding-left", "10px")
            .text("A feature with that name already exists.");
          nameField.append(alertBlock);
          alertBlock.fadeOut(5000);
        }
      }
    });
    nameField.on("click", showUpdateNameButton);
  }

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
    $(".name").on("click", showUpdateNameButton);
    $(".description").on("click", showButton);
    $(".btn-warning").hide();
    $(".target-name").on("mouseenter", function() {
      $(this).parent().find(".btn-warning").show();
    });
    $(".target-name").on("mouseleave", function() {
      $(this).parent().find(".btn-warning").hide();
    });
    $("#new-feature-button").show();
  });

  $("#feature-filter").on("keyup", function(event) {
    // ...
  });

  $("#value-filter").on("keyup", function(event) {
    // ...
  });

});
