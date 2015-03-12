var intervalID;
var minutesLeft = 45;
var running = $('<small>').text(' minutes left');
var stopped = $('<small>').text(' minutes left (stopped)');

var updateClock = function() {
  minutesLeft -= 1;
  $('#clock').text(minutesLeft).append(running);
  if (minutesLeft === 0) {
    clearInterval(intervalID);
    $('#stop').prop('disabled', true);
    $('#questionaire').show();
    $('#alert').get(0).play();
  }
};

$(document).ready(function() {

  // Firefox remembers "disabled" state of buttons, so reset them on
  // page load:
  $('button').prop('disabled', false);

  $('#stop').hide();
  $('#questionaire').hide();

  $('#start').on('click', function(e) {
    $(e.currentTarget).prop('disabled', true);
    $('#stop').show();
    $('#clock').text(minutesLeft).append(running);
    intervalID = setInterval(updateClock, 60000);
  });

  $('#stop').on('click', function(e) {
    $(e.currentTarget).hide();
    $('#start').prop('disabled', false);
    $('#clock').text(minutesLeft).append(stopped);
    clearInterval(intervalID);
  });

  $('#questionaire').on('click', function() {
    window.location.href = '../questionaire/questionaire.html' ;
  });

});
