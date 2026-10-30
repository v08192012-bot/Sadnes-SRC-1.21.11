// @name Nulls
// @author {author}
// @version 1.0
// @description Противоположные клавиши движения не гасят друг друга: идёте в ту сторону, что нажата последней

var pack = Module("Nulls", true);
var feature = pack.feature("Nulls", "Movement", "MOVEMENT_ASSIST")
    .description("feature.impl.movement.nulls");
var priority = feature.mode("nulls.priority", ["last", "first"], "last");
var axes = feature.multi("nulls.axes", ["strafe", "forward"], ["strafe", "forward"]);

// Состояние клавиш прошлого тика — по нему видно, какая из пары нажата позже.
var leftHeld = false;
var rightHeld = false;
var forwardHeld = false;
var backHeld = false;
var strafeWinner = 0;
var forwardWinner = 0;

feature.onEnabled(function () {
    leftHeld = input.physical("left");
    rightHeld = input.physical("right");
    forwardHeld = input.physical("forward");
    backHeld = input.physical("back");
    strafeWinner = 0;
    forwardWinner = 0;
});

events.move_input(function (e) {
    if (!feature.enabled() || !player.exists()) return;
    var left = input.physical("left");
    var right = input.physical("right");
    var forward = input.physical("forward");
    var back = input.physical("back");
    var lastWins = priority.is("last");

    if (!left && !right) strafeWinner = 0;
    if (!forward && !back) forwardWinner = 0;

    if (left && !leftHeld && (lastWins || !right)) strafeWinner = 1;
    if (right && !rightHeld && (lastWins || !left)) strafeWinner = -1;
    if (forward && !forwardHeld && (lastWins || !back)) forwardWinner = 1;
    if (back && !backHeld && (lastWins || !forward)) forwardWinner = -1;

    leftHeld = left;
    rightHeld = right;
    forwardHeld = forward;
    backHeld = back;

    if (axes.enabled("strafe") && left && right && strafeWinner !== 0) e.setStrafe(strafeWinner);
    if (axes.enabled("forward") && forward && back && forwardWinner !== 0) e.setForward(forwardWinner);
});
