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

jQuery.cap = function(str) {
  return str.charAt(0).toUpperCase() + str.substring(1);
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

jQuery.h3 = function(text) {
  return $('<h3>').text(text);
}

jQuery.form = function() {
  return $('<form>').attr('role', 'form');
}

jQuery.formGroup = function(field, id) {
  var label = $.label(field, id);
  var inputField = $('<input>').addClass('form-control')
    .attr('type', 'text')
    .attr('id', id)
    .attr('placeholder', 'Enter ' + field + ' ...');
  return $('<div>').addClass('form-group').append(label).append(inputField);
}

jQuery.label = function(field, id) {
  return $('<label>').attr('for', id).text($.cap(field) + ':');
}

jQuery.radio = function(value, controlClass) {
  var label = $('<label>')
  var input = $('<input>').addClass(controlClass)
    .attr('type', 'radio')
    .attr('value', value);
  label.append(input);
  label.append(document.createTextNode(value));
  return $('<div>').addClass('radio').append(label);
}

jQuery.createButton = function() {
  return $('<button>').addClass('btn btn-lg btn-success')
    .attr('id', 'create')
    .text('Save');
}
