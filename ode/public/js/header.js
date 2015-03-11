// header.js --- Client-side code for functionality available from the navigation bar.

// Copyright (C) 2013-2015  Tim Krones

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

var Header = {};

Header.Model = {};


Header.Model.NewRule = Backbone.Model.extend({

  defaults: {
    description: '...',
  },

  initialize: function() {
    this.urlRoot = '/rules';
  },

});



// Application

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
      var newRule = new Header.Model.NewRule({ name: name });
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
