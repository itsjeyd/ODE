var Rule = Backbone.Model.extend({

  defaults: {
    description: '',
  },

  initialize: function() {
    this.urlRoot = '/rules';
  },

});

$(document).ready(function() {

  $('a#new').on('click', function(e) {
    e.preventDefault();
    var name = prompt('Enter name:');
    var description = prompt('Enter description (optional):');
    var newRule = new Rule({ name: name, description: description });
    newRule.save(
      {},
      { success: function(model, response, options) {
        alert(JSON.stringify(response));
      },
      });
  });

});
