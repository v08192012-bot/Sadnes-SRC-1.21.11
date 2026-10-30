// @name WTap
// @author {author}
// @version 1.0
// @description Ненадолго останавливает движение после удара — меньше отдачи и лучше дистанция

var pack = Module("WTap", true);
var feature = pack.feature("WTap", "Combat", "COMBAT_TWEAKS")
    .description("feature.impl.combat.wtapfeature");

var stopUntil = 0; // до какого момента (Date.now) ввод движения отпущен

events.attack(function (e) {
    if (feature.enabled() && e.post) stopUntil = Date.now() + 100;
});

events.move_input(function (e) {
    if (!feature.enabled() || Date.now() > stopUntil) return;
    e.setForward(0);
    e.setStrafe(0);
});
