$(document).ready(function() {

  // Source: http://viralpatel.net/blogs/jquery-get-text-element-without-child-element/
  jQuery.fn.textOnly = function() {
    return $(this)
      .clone()
      .children()
      .remove()
      .end() // Go back to cloned element
      .text();
  };


  $(".draggable").draggable({
    cursor: "crosshair",
    helper: "clone",
    revert: "invalid",
  });
  $(".draggable.value").draggable({
    helper: function(event, ui) {
      var clonedValue = $(this).clone();
      var targetName = clonedValue.textOnly();
      clonedValue.empty();
      clonedValue.text(targetName);
      return clonedValue;
    }
  });
  $(".draggable.value").on("dblclick", function() {
    $(this).draggable({ disabled: true });
  });
  $(".droppable").css({
    "width": "200px",
    "height": "30px",
    "border": "solid 3px #94D6EA",
    "border-radius": "10px",
    "margin-top": "5px",
    "margin-bottom": "5px",
  });


  var interactionBlock = $("#interaction-block");
  var newFeatureBlock = $("#new-feature-block");
  var newFeatureButton = $("#new-feature-button");
  var editFeatureBlocks = $(".edit-feature-block");
  var featureItems = $(".feature-item");
  var values = $(".value");
  var deleteFeatureButtons = $(".delete-feature-button");
  var renameValueButtons = $(".rename-value-button");


  // Functions showing action buttons for in-line editing

  function showUpdateFeatureButton() {
    $(this).off("click");
    $(this).parent().next().show();
  }

  function showRenameValueButton() {
    $(this).off("dblclick");
    $(this).draggable({ disabled: true });
    $(this).find(".rename-value-button").show();
  }


  // Functions invoking AJAX actions

  function deleteFeature(clickEvent) {
    clickEvent.preventDefault();
    var featureItem = $(this).parent();
    var featureName = $.trim(featureItem.find(".feature-name").text());
    var route = jsFeatureRoutes.controllers.Features
      .deleteFeature(featureName);
    $.ajax({
      url: route.url,
      type: route.type,
      statusCode: {
        200: function() {
          featureItem.fadeOut(2000);
          var alertBlock = $("<span>").addClass("text-success")
            .text("Feature successfully deleted!");
          interactionBlock.html(alertBlock);
          },
        400: function() {
          var alertBlock = $("<span>").addClass("text-danger")
            .text("Can't delete feature.");
          interactionBlock.html(alertBlock);
          }
      }
    });
  }

  function updateFeatureName(clickEvent) {
    clickEvent.preventDefault();
    var nameField = $(this).parent().find(".name");
    var oldName = nameField.data("name");
    var newName = nameField.text();
    var featureItem = $("a[href='#" + oldName + "']");
    var editBlock = $("#"+oldName);
    var updateButton = $(this);
    var route = jsFeatureRoutes.controllers.Features
      .updateFeatureName(oldName);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "name": newName },
      statusCode: {
        200: function() {
          nameField.data("name", newName);
          featureItem.attr("href", "#"+newName)
            .text(newName);
          editBlock.attr("id", newName);
          editBlock
            .find(".name").attr("data-name", newName);
          editBlock
            .find(".name").text(newName);
          var alertBlock = $("<span>").addClass("text-success")
            .css("padding-left", "10px")
            .text("Name successfully updated!");
          alertBlock.insertAfter(updateButton);
          updateButton.fadeOut(5000);
          alertBlock.fadeOut(5000);
        },
        400: function() {
          nameField.text(oldName);
          updateButton.hide();
          var alertBlock = $("<span>").addClass("text-danger")
            .css("padding-left", "10px")
            .text("A feature with that name already exists.");
          nameField.append(alertBlock);
          alertBlock.fadeOut(5000);
        }
      }
    });
    nameField.on("click", showUpdateFeatureButton);
  }

  function updateFeatureDescription(clickEvent) {
    clickEvent.preventDefault();
    var feature = $(this).parent().find("h3").text();
    var editBlock = $("#"+feature);
    var descriptionField = $(this).parent().find(".description");
    var newDescription = descriptionField.text();
    var updateButton = $(this);
    var route = jsFeatureRoutes.controllers.Features
      .updateFeatureDescription(feature);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "description": newDescription },
      success: function(result) {
        editBlock.find(".description").text(newDescription);
        var alertBlock = $("<span>").addClass("text-success")
          .css("padding-left", "10px")
          .text("Description successfully updated!");
        alertBlock.insertAfter(updateButton);
        updateButton.fadeOut(5000);
        alertBlock.fadeOut(5000);
      }
    });
    descriptionField.on("click", showUpdateFeatureButton);
  }

  function updateFeatureType(clickEvent) {
    clickEvent.preventDefault();
    var updateButton = $(this);
    var featureName = updateButton.parents("form").data("feature");
    var newType = updateButton.parents("form").find("input:checked").val();
    var route = jsFeatureRoutes.controllers.Features
      .updateFeatureType(featureName);
    var editBlock = $("#"+featureName);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "type": newType },
      statusCode: {
        200: function() {
          var oldType = newType === "complex" ? "atomic" : "complex";
          editBlock.find("input[value='" + oldType + "']")
            .removeAttr('checked');
          editBlock.find("input[value='" + newType + "']")
            .attr('checked', true);
          updateButton.hide();
          var alertBlock = $("<span>").addClass("text-success")
            .text("Type successfully updated!");
          updateButton.parent().append(alertBlock);
          alertBlock.fadeOut(2000);
          var updateTypeForm = updateButton.parents("form");
          var targetsHeading = newType === "complex" ?
            "Features permitted in substructure:" : "Permitted values:";
          updateTypeForm.next("h4").text(targetsHeading).show();
          var addTargetForm = updateTypeForm.nextAll("form").last();
          addTargetForm.attr("data-type", newType);
          addTargetForm.show();
          editBlock.find("h4").text(targetsHeading);
          editBlock.find(".target-name").parent("form").remove();
          editBlock.find("form").last().attr("data-type", newType);
        },
        400: function() {
          var alertBlock = $("<span>").addClass("text-danger")
            .text("Type not updated.");
          updateButton.parent().append(alertBlock);
        }
      }
    });
  }

  function deleteTarget(clickEvent) {
    clickEvent.preventDefault();
    var deleteTargetButton = $(this);
    var targetForm = deleteTargetButton.parents("form");
    var featureName = targetForm.data("feature");
    var featureType = targetForm.data("type");
    var targetName = targetForm.data("target");
    var editBlock = $("#"+featureName);
    var route = jsFeatureRoutes.controllers.Features.deleteTarget(
      featureName, targetName);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "type": featureType },
      statusCode: {
        200: function() {
          var alertBlock = $("<span>").addClass("text-success")
            .css("padding-left", "10px")
            .text("OK!");
          alertBlock.insertAfter(deleteTargetButton);
          deleteTargetButton.remove();
          targetForm.fadeOut(3000);
          setTimeout(function() { targetForm.remove(); }, 3500);
          editBlock.find("form[data-target='" + targetName + "']").remove();
        },
        400: function() {
          var alertBlock = $("<span>").addClass("text-danger")
            .css("padding-left", "10px")
            .text("Error. Could not remove target from feature.");
          alertBlock.insertAfter(deleteTargetButton);
        }
      }
    });
  }

  function addTargets(clickEvent) {
    clickEvent.preventDefault();
    var addTargetsButton = $(this);
    var targetsForm = addTargetsButton.parent("form");
    var featureName = targetsForm.data("feature");
    var featureType = targetsForm.data("type");
    var inputField = targetsForm.find("div.droppable");
    var target = $.trim(inputField.text());
    var editBlock = $("#"+featureName);
    var route = jsFeatureRoutes.controllers.Features.addTargets(featureName);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "type": featureType, "target": target },
      statusCode: {
        200: function() {
          var newTargetForm = $("<form>").attr("role", "form")
            .attr("data-feature", featureName)
            .attr("data-type", featureType)
            .attr("data-target", target);
          var newTargetName = $("<div>").addClass("target-name").text(target);
          var newDeleteButton = $("<button>")
            .css("margin-left", "5px")
            .addClass("btn btn-xs btn-warning delete-target")
            .attr("type", "submit")
            .text("Delete");
          newDeleteButton.on("click", deleteTarget);
          newDeleteButton.hide();
          newTargetName.append(newDeleteButton);
          newTargetName.on("mouseenter", function() {
            $(this).parent().find(".delete-target").show();
          });
          newTargetName.on("mouseleave", function() {
            $(this).parent().find(".delete-target").hide();
          });
          newTargetForm.html(newTargetName);
          targetsForm.before(newTargetForm);
          inputField.text("");
          var alertBlock = $("<span>").addClass("text-success")
            .css("padding-left", "10px")
            .text("OK!");
          alertBlock.insertAfter(addTargetsButton).fadeOut(5000);
          addTargetsButton.attr("disabled", true);
          editBlock.html(interactionBlock.html());
          editBlock.find(".text-success").remove();
        },
        400: function() {
          inputField.text("");
          var alertBlock = $("<span>").addClass("text-danger")
            .css("padding-left", "10px")
            .text("Error. Could not add target.");
          alertBlock.insertAfter(addTargetsButton).fadeOut(5000);
          addTargetsButton.attr("disabled", true);
        }
      }
    });
  }

  function renameValue(clickEvent) {
    clickEvent.preventDefault();
    var renameButton = $(this);
    var value = $(this).parent();
    var oldName = value.data("value");
    var newName = $.trim(value.textOnly());
    var route = jsValueRoutes.controllers.Values.renameValue(oldName);
    $.ajax({
      url: route.url,
      type: route.type,
      data: { "name": newName },
      statusCode: {
        200: function() {
          $("form[data-target='" + oldName + "'] > .target-name")
            .each(function() {
              var deleteButton = $(this).children("button");
              $(this).empty();
              $(this).text(newName);
              deleteButton.css("margin-left", "5px");
              $(this).append(deleteButton);
              $(this).parent().attr("data-target", newName);
              deleteButton.on("click", deleteTarget);
            });
          value.data("value", newName);
          var alertBlock = $("<span>").addClass("text-success")
            .css("padding-left", "10px")
            .text("Rename successful!");
          alertBlock.insertAfter(renameButton);
          renameButton.fadeOut(5000);
          alertBlock.fadeOut(5000);
          value.on("dblclick", showRenameValueButton);
          value.draggable({ disabled: false });
        },
        400: function() {
          var alertBlock = $("<span>").addClass("text-danger")
            .css("padding-left", "10px")
            .text("Value not renamed.");
          alertBlock.insertAfter(renameButton);
          renameButton.fadeOut(5000);
          alertBlock.fadeOut(5000);
          value.on("dblclick", showRenameValueButton);
          value.draggable({ disabled: false });
        }
      }
    });
  }


  // Hide some elements by default

  newFeatureBlock.hide();
  editFeatureBlocks.hide();
  deleteFeatureButtons.hide();
  renameValueButtons.hide();


  // Set up event handlers

  newFeatureButton.on("click", function(event) {
    event.preventDefault();
    $(".alert").hide();
    $(this).hide();
    interactionBlock.html(newFeatureBlock.html());
  });

  deleteFeatureButtons.on("click", deleteFeature);

  featureItems.on("mouseenter click", function() {
    deleteButton = $(this).find(".delete-feature-button");
    deleteButton.show();
  });

  featureItems.on("mouseleave", function() {
    deleteButton = $(this).find(".delete-feature-button");
    deleteButton.hide();
  });

  $(".feature-name").on("click", function(event) {
    event.preventDefault();
    $(".alert").hide();

    var editBlockID = $(this).attr("href");
    interactionBlock.html($(editBlockID).html());

    $(".update-feature-name").on("click", updateFeatureName);
    $(".update-feature-name").hide();
    $(".name").on("click", showUpdateFeatureButton);

    $(".update-feature-description").on("click", updateFeatureDescription);
    $(".update-feature-description").hide();
    $(".description").on("click", showUpdateFeatureButton);

    $(".update-type").hide();
    var storedType = interactionBlock.find(":radio:checked").val();
    $(":radio").on("change", function() {
      var updateTypeForm = $(this).parents("form");
      var updateButton = updateTypeForm.find("button");
      if ($(this).val() !== storedType) {
        updateTypeForm.next("h4").hide();
        updateTypeForm.nextAll("form").hide();
        updateButton.one("click", updateFeatureType);
        updateButton.show();
      } else {
        updateTypeForm.next("h4").show();
        updateTypeForm.nextAll("form").show();
        updateButton.hide()
      }
    });

    $(".delete-target").hide();
    $(".delete-target").on("click", deleteTarget);
    $(".target-name").on("mouseenter", function() {
      $(this).find(".delete-target").show();
    });
    $(".target-name").on("mouseleave", function() {
      $(this).find(".delete-target").hide();
    });

    $(".droppable").droppable({
      drop: function(event, ui) {
        var featureName = $(this).parent().data("feature");
        var featureType = $(this).parent().data("type");
        var target = ui.helper;
        var targetName = $.trim(target.text());
        var alertBlock;
        if (featureType === "complex") {
          if (target.hasClass("value")) {
            alertBlock = $("<span>").addClass("text-danger")
              .css("padding-left", "10px")
              .text("Can't add value to complex feature!");
          } else if (targetName === featureName) {
            alertBlock = $("<span>").addClass("text-danger")
              .css("padding-left", "10px")
              .text("Can't add circular dependency!");
          } else {
            ui.draggable
              .next(".delete-feature-button").attr("disabled", true);
          }
        } else {
          if (target.hasClass("feature-name")) {
            alertBlock = $("<span>").addClass("text-danger")
              .css("padding-left", "10px")
              .text("Can only add values to atomic features.");
          }
        }
        if (alertBlock) {
          alertBlock.insertAfter($(this).next("button"));
          alertBlock.fadeOut(5000);
        } else {
          $(this).text(targetName);
          $(".add-targets").removeAttr("disabled");
        }
      }
    });

    $(".add-targets").attr("disabled", true);
    $(".add-targets").on("click", addTargets);
    $(".droppable").on("click", function() {
      if ($(this).parent().data("type") === "atomic") {
        $(this).on("keyup", function() {
          var addTargetsButton = $(".add-targets");
          if (addTargetsButton.is(":disabled")) {
            if ($(this).text()) {
              addTargetsButton.removeAttr("disabled");
            }
          } else {
            if (!$(this).text()) {
              addTargetsButton.attr("disabled", true);
            }
          }
        });
      }
    });

    newFeatureButton.show();
  });

  renameValueButtons.on("click", renameValue);
  values.on("dblclick", showRenameValueButton);


  // Functionality for filtering global feature and value lists

  // - Gather list of all features:
  var featureNames = $(".feature-name").map(function() {
    return $(this);
  });

  $("#feature-filter").on("keyup", function(event) {
    // - Get current input:
    var currentInput = $(this).val().toLowerCase();
    // - Match list items against current input:
    featureNames.each(function() {
      var featureItem = $(this).parent();
      var featureName = $(this).text();
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
  var valueNames = values.map(function() {
    return $(this);
  });

  $("#value-filter").on("keyup", function(event) {
    // - Get current input:
    var currentInput = $(this).val().toLowerCase();
    // - Match list items against current input:
    valueNames.each(function() {
      var value = $(this);
      var valueName = value.textOnly();
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
