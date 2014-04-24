// Collections

var AVM = Backbone.Collection.extend({});

var RHS = Backbone.Collection.extend({});


// Views

var AVMView = Backbone.View.extend({});

var RHSView = Backbone.View.extend({});


// Application

$(document).ready(function() {

  var lhsJSON = $('#lhs').data('json');
  var lhs = new AVM(null, { json: lhsJSON });

  var lhsView = new AVMView({
    collection: lhs,
    el: '#lhs'
  });
  lhsView.render();

  var rhsJSON = $('#rhs').data('json');
  var rhs = new RHS(null, { json: rhsJSON });

  var rhsView = new RHSView({
    collection: rhs,
    el: '#rhs'
  });
  rhsView.render();

});
