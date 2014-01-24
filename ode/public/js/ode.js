$(document).ready(function() {

  function deleteFeature(clickEvent) {
    clickEvent.preventDefault();
    featureItem = $(this).parent();
    featureName = featureItem.find(".feature-name").text();
    route = jsRoutes.controllers.Features.deleteFeature(featureName);
    $.ajax({
      url: route.url,
      type: route.type,
      statusCode: {
        200: function() {
          featureItem.fadeOut(2000);
          alertBlock = $("<span>").addClass("text-success")
            .text("Feature successfully deleted!");
          $("#interaction-block").html(alertBlock);
          },
        400: function() {
          alertBlock = $("<span>").addClass("text-danger")
            .text("Can't delete feature.");
          $("#interaction-block").html(alertBlock);
          }
      }
    });
  }

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
  $(".btn-warning").hide();
  $(".update-type").hide();

  $("#new-feature-button").on("click", function(event) {
    event.preventDefault();
    $(this).hide();
    $(".alert").hide();
    $("#interaction-block").html($("#new-feature").html());
  });

  $(".feature-name").on("click", function(event) {
    event.preventDefault();
    $(".alert").hide();
    $("#interaction-block").html($("#"+$(this).text()).html());
    $(".name").on("click", showUpdateNameButton);
    $(".description").on("click", showButton);

    $(":radio").on("change", function() {
      $(this).parents("form").find("button").show();
    });

    $(".btn-warning").hide();
    $(".target-name").on("mouseenter", function() {
      $(this).parent().find(".btn-warning").show();
    });
    $(".target-name").on("mouseleave", function() {
      $(this).parent().find(".btn-warning").hide();
    });
    $("#new-feature-button").show();
  });

  $(".feature-item").on("mouseenter click", function() {
    deleteButton = $(this).find(".btn-warning");
    deleteButton.on("click", deleteFeature);
    deleteButton.show();
  });

  $(".feature-item").on("mouseleave", function() {
    deleteButton = $(this).find(".btn-warning");
    deleteButton.off("click");
    deleteButton.hide();
  });

  $("#feature-filter").on("keyup", function(event) {
    // ...
  });

  $("#value-filter").on("keyup", function(event) {
    // ...
  });

});
