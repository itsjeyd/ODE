var NewRule = Backbone.Model.extend({

  defaults: {
    description: '...',
  },

  initialize: function() {
    this.urlRoot = '/rules';
  },

});

$(document).ready(function() {

  $('a#new').on('click', function(e) {
    e.preventDefault();
    $('.alert').hide();
    $('#new-rule-modal').modal();
  });

  $('button#create-new').on('click', function(e) {
    // Remove validation messages:
    $('.alert').hide();
    var nameField = $('#new-rule-name');
    nameField.parents('.form-group').removeClass('has-error');
    nameField.next('.help-block').empty();
    // Try to save new rule:
    var name = nameField.val();
    if (!name) {
      nameField.parents('.form-group').addClass('has-error');
      nameField.next('.help-block').text('This field can not be empty.');
    } else {
      var newRule = new NewRule({ name: name });
      var description = $('#new-rule-description').val();
      if (description) {
        newRule.set('description', description);
      }
      newRule.save(
        {},
        { success: function(model, response, options) {
            window.location.replace(model.url() + '/input');
          },
          error: function(model, xhr, options) {
            $('.alert').show();
          },
        });
    }
  });

});
