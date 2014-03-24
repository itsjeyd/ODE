// Utility functions for operating on existing DOM elements

// Source: http://viralpatel.net/blogs/jquery-get-text-element-without-child-element/
jQuery.fn.textOnly = function() {
  return $(this)
    .clone()
    .children()
    .remove()
    .end() // Go back to cloned element
    .text();
};

jQuery.fn.exists = function() {
  return this.length;
}

jQuery.fn.isEmpty = function() {
  return !$(this).val();
}

jQuery.fn.check = function() {
  return $(this).attr('checked', true);
}

jQuery.fn.select = function() {
  return $(this).attr('selected', true);
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


// Utility functions for operating on strings

jQuery.matches = function(str, input) {
  return str.indexOf(input) !== -1;
}

jQuery.cap = function(str) {
  return str.charAt(0).toUpperCase() + str.substring(1);
}


// Utility functions for creating new DOM elements

jQuery.div = function(klass) {
  var div = $('<div>');
  if (klass) {
    div.addClass(klass);
  }
  return div;
}

jQuery.span = function(klass) {
  return $('<span>').addClass(klass);
}

jQuery.placeholder = function(text) {
  return $.div('droppable placeholder').text(text);
}

jQuery.alertMsg = function(text) {
  return $('<span>').addClass('alert-msg text-danger').text(text);
}

jQuery.hr = function() {
  return $('<hr>');
}

jQuery.h3 = function(text) {
  return $('<h3>').text(text);
}

jQuery.h4 = function(text) {
  return $('<h4>').text(text);
}

jQuery.h5 = function(text) {
  return $('<h5>').text(text);
}

jQuery.small = function() {
  return $('<small>');
}

jQuery.p = function(text) {
  return $('<p>').text(text);
}

jQuery.form = function() {
  return $('<form>').attr('role', 'form');
}

jQuery.formGroup = function(field, id) {
  var label = $.label().attr('for', id).text($.cap(field) + ':');
  var inputField = $.textInput()
    .attr('id', id)
    .attr('placeholder', 'Enter ' + field + ' ...');
  return $('<div>').addClass('form-group').append(label).append(inputField);
}

jQuery.radioButton = function(group, value, controlClass) {
  var label = $.label();
  var input = $.radioInput(group, value).addClass(controlClass);
  label.append(input);
  label.append(document.createTextNode(value));
  return $('<div>').addClass('radio').append(label);
}

jQuery.label = function() {
  return $('<label>');
}

jQuery.input = function(type) {
  return $('<input>').attr('type', type);
}

jQuery.textInput = function() {
  return $.input('text').addClass('form-control');
}

jQuery.selectMenu = function() {
  return $('<select>').addClass('form-control');
}

jQuery.option = function(text) {
  return $('<option>').text(text);
}

jQuery.radioInput = function(group, value) {
  return $.input('radio').attr('name', group).attr('value', value);
}

jQuery.button = function(text) {
  return $('<button>').addClass('btn').text(text);
}

jQuery.infoButton = function(text) {
  return $.button(text).addClass('btn-info');
}

jQuery.successButton = function(text) {
  return $.button(text).addClass('btn-success');
}

jQuery.addButton = function() {
  return $.infoButton('Add');
}

jQuery.okButton = function() {
  return $.infoButton('OK');
}

jQuery.createButton = function() {
  return $.successButton('Create').addClass('btn-lg').attr('id', 'create');
}

jQuery.editButton = function(target) {
  return $('<span>').addClass('glyphicon glyphicon-pencil edit-button')
    .data('target', target);
}

jQuery.plusButton = function(target) {
  return $('<span>').addClass('glyphicon glyphicon-plus plus-button');
}

jQuery.copyButton = function(target) {
  return $('<span>').addClass('glyphicon glyphicon-new-window copy-button');
}

jQuery.removeButton = function(target) {
  return $('<span>').addClass('glyphicon glyphicon-remove remove-button')
    .data('target', target);
}

jQuery.emptyButton = function() {
  return $('<span>').addClass('glyphicon glyphicon-unchecked empty-button');
}
