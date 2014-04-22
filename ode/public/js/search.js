// Models

var SearchTarget = Backbone.Model.extend({

  defaults: {
    features: new Backbone.Collection([]),
    strings: new Backbone.Collection([]),
  },

});

var Feature = Backbone.Model.extend({

  defaults: { value: '' }

});

// Views

var SearchTargetView = Backbone.View.extend({});

// Application

$(document).ready(function() {

  var searchTarget = new SearchTarget();
  var searchTargetView = new SearchTargetView({
    model: searchTarget,
    el: '.container-full',
  });

});
