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

  function updateFeatureType(clickEvent) {
    clickEvent.preventDefault();
    updateButton = $(this);
    featureName = updateButton.parents("form").data("feature");
    newType = updateButton.parents("form").find("input:checked").val();
    route = jsRoutes.controllers.Features.updateFeatureType(featureName);
    editBlock = $("#"+featureName);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "type": newType },
      statusCode: {
        200: function() {
          oldType = newType === "complex" ? "atomic" : "complex";
          editBlock.find("input[value='" + oldType + "']")
            .removeAttr('checked');
          editBlock.find("input[value='" + newType + "']")
            .attr('checked', true);
          updateButton.hide();
          alertBlock = $("<span>").addClass("text-success")
            .text("Type successfully updated!");
          updateButton.parent().append(alertBlock);
          alertBlock.fadeOut(2000);
        },
        400: function() {
          alertBlock = $("<span>").addClass("text-danger")
            .text("Type not updated.");
          updateButton.parent().append(alertBlock);
        }
      }
    });
  }

  function addTargets(clickEvent) {
    clickEvent.preventDefault();
    addTargetsButton = $(this);
    targetsForm = addTargetsButton.parent("form");
    featureName = targetsForm.data("feature");
    featureType = targetsForm.data("type");
    inputField = targetsForm.find("input[name='target']");
    target = inputField.val();
    editBlock = $("#"+featureName);
    route = jsRoutes.controllers.Features.addTargets(featureName);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "type": featureType, "target": target },
      statusCode: {
        200: function() {
          newTarget = $("<p>").addClass("target").text(target);
          targetsForm.before(newTarget);
          inputField.val("");
          alertBlock = $("<span>").addClass("text-success")
            .css("padding-left", "10px")
            .text("OK!");
          alertBlock.insertAfter(addTargetsButton).fadeOut(5000);
          addTargetsButton.attr("disabled", true);
          interactionBlock = targetsForm.parents("#interaction-block");
          editBlock.html(interactionBlock.html());
          editBlock.find(".text-success").remove();
        },
        400: function() {
          inputField.val("");
          alertBlock = $("<span>").addClass("text-danger")
            .css("padding-left", "10px")
            .text("Error. Could not add target.");
          alertBlock.insertAfter(addTargetsButton).fadeOut(5000);
          addTargetsButton.attr("disabled", true);
        }
      }
    });
  }

  $("#new-feature").hide();
  $(".edit-feature").hide();
  $(".btn-warning").hide();

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

    $(".update-type").hide();
    $(":radio").on("change", function() {
      updateButton = $(this).parents("form").find("button");
      updateButton.one("click", updateFeatureType);
      updateButton.show();
    });

    $(".btn-warning").hide();
    $(".target-name").on("mouseenter", function() {
      $(this).parent().find(".btn-warning").show();
    });
    $(".target-name").on("mouseleave", function() {
      $(this).parent().find(".btn-warning").hide();
    });

    $(".add-targets").attr("disabled", true);
    $(".add-targets").on("click", addTargets);
    $("input[name='target']").on("keyup", function() {
      addTargetsButton = $(".add-targets");
      if (addTargetsButton.is(":disabled")) {
        if ($(this).val()) {
          addTargetsButton.removeAttr("disabled");
        }
      } else {
        if (!$(this).val()) {
          addTargetsButton.attr("disabled", true);
        }
      }
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

  // - Gather list of all features:
  featureNames = $(".feature-name").map(function() {
    return $(this);
  });

  $("#feature-filter").on("keyup", function(event) {
    // - Get current input:
    currentInput = $(this).val().toLowerCase();
    // - Match list items against current input:
    featureNames.each(function() {
      featureItem = $(this).parent();
      featureName = $(this).text();
      if (featureName.toLowerCase().indexOf(currentInput) === -1) {
        featureItem.hide();
      } else {
        if (!featureItem.is(":visible")) {
          featureItem.show();
        }
      }
    });
  });

  // - Gather list of all values:
  valueNames = $(".value").map(function() {
    return $(this);
  });

  $("#value-filter").on("keyup", function(event) {
    // - Get current input:
    currentInput = $(this).val().toLowerCase();
    // - Match list items against current input:
    valueNames.each(function() {
      value = $(this);
      valueName = value.text();
      if (valueName.toLowerCase().indexOf(currentInput) === -1) {
        value.hide();
      } else {
        if (!value.is(":visible")) {
          value.show();
        }
      }
    });
  });

});
