var TEXT = 'text';
var RADIO = 'radio';

var mandatoryFields = {

  'education': { 'type': TEXT },

  'student': {
    'type': RADIO,
    'subFieldTrigger': 'yes',
    'subField': { 'name': 'student-major',
                  'type': TEXT } // if student
  },

  'language': { 'type': TEXT },

  'computer-literacy': { 'type': RADIO },

  'basic-understanding': { 'type': RADIO },

  'difficult': {
    'type': RADIO,
    'subFieldTrigger': 'yes',
    'subField': { 'name': 'difficult-reasons', // if difficult
                  'type': RADIO,
                  'subFieldTrigger': 'other',
                  'subField': { 'name': 'difficult-reasons-other', // if other
                                'type': TEXT } }
  },

  'most-effort': { 'type': RADIO },

  'errors': {
    'type': RADIO,
    'subFieldTrigger': 'yes',
    'subField': { 'name': 'error-causes',
                  'type': TEXT } // if errors
  },

  'confused': {
    'type': RADIO,
    'subFieldTrigger': 'yes',
    'subField': { 'name': 'confused-causes',
                  'type': TEXT } // if confused
  },

  'slow': {
    'type': RADIO,
    'subFieldTrigger': 'yes',
    'subField': { 'name': 'slow-causes',
                  'type': TEXT } // if slow
  },

};

var validate = function() {

  $('li, input, p, textarea').removeClass('has-error');

  var ok = true;

  $.each(mandatoryFields, function(name, attrs) {

    var valid = validateField(name, attrs);
    ok = ok && valid;

  });

  return ok;

};

var validateField = function(name, attrs) {
  var selector;
  var val;
  var type = attrs['type'];

  if (type === TEXT) {
    selector = '#' + name;
    val = $(selector).val();
  } else if (type === RADIO) {
    selector = 'input[name="' + name + '"]';
    val = $(selector + ':checked').val();
  }

  if (!val) {
    $(selector).parents('li').addClass('has-error');
    return false;
  } else {
    // Validate subField:
    if (val === attrs['subFieldTrigger']) {
      var subFieldValid =
        validateSubField(attrs['subField']);
      if (!subFieldValid) {
        return false;
      }
    }
    return true;
  }
};

var validateSubField = function(subField) {
  var selector;
  var val;
  var name = subField['name'];
  var type = subField['type'];

  if (type === TEXT) {
    selector = '#' + name;
    val = $(selector).val();
    if (!val) {
      $(selector).addClass('has-error');
      if ($(selector).is('textarea')) {
        $(selector).prev('p').addClass('has-error');
      }
    }
  } else if (type === RADIO) {
    selector = 'input[name="' + name + '"]';
    val = $(selector + ':checked').val();
    if (!val) {
      $(selector).parent('li').addClass('has-error');
    }
  }

  if (!val) {
    return false;
  } else {
    // Validate subField:
    if (val === subField['subFieldTrigger']) {
      var subFieldValid =
        validateSubField(subField['subField']);
      if (!subFieldValid) {
        return false;
      }
    }
    return true;
  }
}

var persist = function(results) {

  var jqxhr = $.ajax({
    url: 'http://localhost/persist.php',
    type: 'POST',
    data: { file: 'data.json', results: results },
  });

};



$(document).ready(function() {

  $('input, textarea').prop('disabled', false);


  // Preserve enabled/disabled state of input fields on page refresh
  // (based on current values of top-level fields):

  if ($('input[name="student"][value="no"]').is(':checked')) {
    $('#student-major').val('').prop('disabled', true);
  }

  if ($('input[name="difficult"][value="yes"]').is(':checked')) {
    if (!$('input[name="difficult-reasons"][value="other"]').is(':checked')) {
      $('#difficult-reasons-other').prop('disabled', true);
    }
  }

  if ($('input[name="difficult"][value="no"]').is(':checked')) {
    $('input[name="difficult-reasons"]').prop('disabled', true);
    $('#difficult-reasons-other').prop('disabled', true);
  }

  if ($('input[name="errors"][value="no"]').is(':checked')) {
    $('#error-causes').prop('disabled', true);
  }

  if ($('input[name="confused"][value="no"]').is(':checked')) {
    $('#confused-causes').prop('disabled', true);
  }

  if ($('input[name="slow"][value="no"]').is(':checked')) {
    $('#slow-causes').prop('disabled', true);
  }


  // Automatically check appropriate radio buttons when participant
  // starts to type into subFields (that depend on specific options
  // being selected):

  $('#student-major').on('keyup', function() {
    var yes = $('input[name="student"][value="yes"]');
    if (!yes.is(':checked')) {
      yes.prop('checked', true);
    }
  });

  $('#difficult-reasons-other').on('keyup', function() {
    var yes = $('input[name="difficult"][value="yes"]');
    if (!yes.is(':checked')) {
      yes.prop('checked', true);
    }
    var other = $('input[name="difficult-reasons"][value="other"]');
    if (!other.is(':checked')) {
      other.prop('checked', true);
    }
  });

  $('#error-causes').on('keyup', function() {
    var yes = $('input[name="errors"][value="yes"]');
    if (!yes.is(':checked')) {
      yes.prop('checked', true);
    }
  });

  $('#confused-causes').on('keyup', function() {
    var yes = $('input[name="confused"][value="yes"]');
    if (!yes.is(':checked')) {
      yes.prop('checked', true);
    }
  });

  $('#slow-causes').on('keyup', function() {
    var yes = $('input[name="slow"][value="yes"]');
    if (!yes.is(':checked')) {
      yes.prop('checked', true);
    }
  });


  // Enable/Disable subFields depending on current value of their
  // parent fields:

  $('input[name="student"][value="yes"]').on('click', function() {
    $('#student-major').prop('disabled', false);
  });

  $('input[name="student"][value="no"]').on('click', function() {
    $('#student-major').val('').prop('disabled', true);
  });

  $('input[name="difficult"][value="yes"]').on('click', function() {
    $('input[name="difficult-reasons"]').prop('disabled', false);
    if (!$('input[name="difficult-reasons"][value!="other"]').is(':checked')) {
      $('#difficult-reasons-other').prop('disabled', false);
    }
  });

  $('input[name="difficult-reasons"][value!="other"]').on('click', function(e) {
    $('#difficult-reasons-other').val('').prop('disabled', true);
  });

  $('input[name="difficult-reasons"][value="other"]').on('click', function(e) {
    $('#difficult-reasons-other').prop('disabled', false);
  });

  $('input[name="difficult"][value="no"]').on('click', function() {
    $('input[name="difficult-reasons"]').prop('checked', false)
      .prop('disabled', true);
    $('#difficult-reasons-other').val('').prop('disabled', true);
  });

  $('input[name="errors"][value="yes"]').on('click', function() {
    $('#error-causes').prop('disabled', false);
  });

  $('input[name="errors"][value="no"]').on('click', function() {
    $('#error-causes').val('').prop('disabled', true);
  });

  $('input[name="confused"][value="yes"]').on('click', function() {
    $('#confused-causes').prop('disabled', false);
  });

  $('input[name="confused"][value="no"]').on('click', function() {
    $('#confused-causes').val('').prop('disabled', true);
  });

  $('input[name="slow"][value="yes"]').on('click', function() {
    $('#slow-causes').prop('disabled', false);
  });

  $('input[name="slow"][value="no"]').on('click', function() {
    $('#slow-causes').val('').prop('disabled', true);
  });


  // Configure behavior of "Reset" and "Submit" controls:

  $('form').submit(function(e) {

    e.preventDefault();

    // Assume participants won't want to reset after clicking "Submit":
    $('#reset').remove();

    if (validate()) {

      var results = $(this).serializeArray();
      persist(results);

      $('input, textarea').prop('disabled', true);
      $('#submit').remove();
      $('#thanks').text('Thank you!');
      $('#alert').get(0).play();

    } else {

      alert('You left some mandatory fields empty. Please fill them in and submit again.');

      // Handling validation states of input fields:

      // 1. Text input fields (top-level)
      //    Covers questions: 1, 3

      $('.has-error > input[type="text"][id!="student-major"]').on(
        'keyup', function(e) {
          var inputField = $(e.currentTarget);
          if (inputField.val()) {
            inputField.parent('li').removeClass('has-error');
          } else {
            inputField.parent('li').addClass('has-error');
          }
        });

      // 2. Radio buttons (top-level)
      //    Covers questions: 4, 5, 7

      $('.has-error > ul > li > input[type="radio"]').on('change', function(e) {
        var radioButton = $(e.currentTarget);
        radioButton.parents('li').removeClass('has-error');
        radioButton.parent('li').prevAll('li').removeClass('has-error');
        radioButton.parent('li').nextAll('li').removeClass('has-error');
      });

      // 3. Radio buttons + follow-up question (top-level)
      //    - Builds on handler for questions 4, 5, 7
      //    - Covers questions: 8, 9, 10

      // Participant clicks on
      // - yes: If textarea is empty, add .has-error to follow-up
      //        question and textarea
      // - no: Remove .has-error from follow-up question and textarea

      // Participant types in textarea:
      // On the very first keystroke, check "yes" if it is not checked
      // (handled by default handler for "keyup" events in text input
      // fields) and trigger a "change" event on "yes" to trigger the
      // handler for validation states on top-level radio buttons. On
      // subsequent keystrokes, add .has-error to follow-up question
      // and textarea if textarea is empty, and remove it otherwise.

      $('input[type="radio"][name="errors"][value="yes"]').on(
        'click', function() {
          var textarea = $('#error-causes');
          if (!textarea.val()) {
            textarea.addClass('has-error');
            textarea.prev('p').addClass('has-error');
          }
      });

      $('input[type="radio"][name="errors"][value="no"]').on(
        'click', function() {
          var textarea = $('#error-causes');
          textarea.removeClass('has-error');
          textarea.prev('p').removeClass('has-error');
      });

      $('input[type="radio"][name="confused"][value="yes"]').on(
        'click', function() {
          var textarea = $('#confused-causes');
          if (!textarea.val()) {
            textarea.addClass('has-error');
            textarea.prev('p').addClass('has-error');
          }
      });

      $('input[type="radio"][name="confused"][value="no"]').on(
        'click', function() {
          var textarea = $('#confused-causes');
          textarea.removeClass('has-error');
          textarea.prev('p').removeClass('has-error');
      });

      $('input[type="radio"][name="slow"][value="yes"]').on(
        'click', function() {
          var textarea = $('#slow-causes');
          if (!textarea.val()) {
            textarea.addClass('has-error');
            textarea.prev('p').addClass('has-error');
          }
      });

      $('input[type="radio"][name="slow"][value="no"]').on(
        'click', function() {
          var textarea = $('#slow-causes');
          textarea.removeClass('has-error');
          textarea.prev('p').removeClass('has-error');
      });

      $('#error-causes, #confused-causes, #slow-causes').one(
        'keyup', function(e) {
          var textarea = $(e.currentTarget);
          textarea.parent('li').find('input[value="yes"]').change();
        });

      $('#error-causes, #confused-causes, #slow-causes').on(
        'keyup', function(e) {
          var textarea = $(e.currentTarget);
          if (textarea.val()) {
            textarea.removeClass('has-error');
            textarea.prev('p').removeClass('has-error');
          } else {
            textarea.addClass('has-error');
            textarea.prev('p').addClass('has-error');
          }
        });

      // 4. Radio buttons and follow-up question (nested level)
      //    Covers questions: 2

      $('input[type="radio"][name="student"][value="yes"]').on(
        'click', function() {
          var inputField = $('#student-major');
          if (!inputField.val()) {
            inputField.addClass('has-error');
          }
      });

      $('input[type="radio"][name="student"][value="no"]').on(
        'click', function() {
          var inputField = $('#student-major');
          inputField.removeClass('has-error');
      });

      $('#student-major').on('keyup', function(e) {
          var inputField = $(e.currentTarget);
          if (inputField.val()) {
            inputField.removeClass('has-error');
          } else {
            inputField.addClass('has-error');
          }
          inputField.parent('li').find('input[value="yes"]').change();
        });

      // 5. Covers questions: 6

      $('input[type="radio"][name="difficult"][value="yes"]').on(
        'click', function() {
          var reasons = $('input[name="difficult-reasons"]');
          if (!reasons.is(':checked')) {
            reasons.parent('li').addClass('has-error');
          }
      });

      $('input[type="radio"][name="difficult"][value="no"]').on(
        'click', function() {
          $('input[name="difficult-reasons"]').parent('li')
            .removeClass('has-error');
          $('#difficult-reasons-other').removeClass('has-error');
      });

      $('input[name="difficult-reasons"][value="other"]').on(
        'click', function() {
          var inputField = $('#difficult-reasons-other');
          if (!inputField.val()) {
            inputField.addClass('has-error');
          }
      });

      $('input[name="difficult-reasons"][value!="other"]').on(
        'click', function() {
          var inputField = $('#difficult-reasons-other');
          if (!inputField.val()) {
            inputField.removeClass('has-error');
          }
      });

      $('#difficult-reasons-other').on('keyup', function(e) {
          var inputField = $(e.currentTarget);
          if (inputField.val()) {
            inputField.removeClass('has-error');
          } else {
            inputField.addClass('has-error');
          }
          inputField.parents('li').find('input[value="yes"]').change();
        });

    }
  });

  $('#reset').on('click', function(e) {
    e.preventDefault();
    $('input[type="radio"]').prop('checked', false);
    $('input[type="text"], textarea').val('');
  });

});
