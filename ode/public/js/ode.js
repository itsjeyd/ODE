// Source: http://viralpatel.net/blogs/jquery-get-text-element-without-child-element/
jQuery.fn.textOnly = function() {
  return $(this)
    .clone()
    .children()
    .remove()
    .end() // Go back to cloned element
    .text();
};

jQuery.fn.isEmpty = function() {
  return !$(this).val();
}

jQuery.fn.check = function() {
  return $(this).attr('checked', true);
}

jQuery.fn.enable = function() {
  return $(this).prop('disabled', false);
}

jQuery.fn.disable = function() {
  return $(this).prop('disabled', true);
}

jQuery.fn.makeEditable = function() {
  return $(this).addClass('editable').attr('contenteditable', true);
}

jQuery.fn.dataToArray = function(dataAttribute) {
  var dataToConvert = $(this).data(dataAttribute);
  if (typeof(dataToConvert) === 'string') {
    return dataToConvert.substring(1, dataToConvert.length-1).split(/, */);
  } else {
    return dataToConvert;
  }
}

jQuery.matches = function(str, input) {
  return str.indexOf(input) !== -1;
}

jQuery.alertMsg = function(text) {
  return $('<span>').addClass('alert-msg text-danger').text(text);
}

jQuery.input = function(controlClass) {
  return $('<input>').addClass('form-control ' + controlClass)
    .attr('type', 'text');
}

jQuery.button = function(controlClass, text) {
  return $('<button>').addClass('btn btn-info ' + controlClass).text(text);
}

jQuery.removeButton = function(target) {
  return $('<span>').addClass('glyphicon glyphicon-remove remove-button')
    .data('target', target);
}
