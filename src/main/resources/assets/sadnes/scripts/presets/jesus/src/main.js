// @name Jesus
// @author {author}
// @version 1.0
// @description Ходьба по воде без провалов

var pack = Module("Jesus", true);
var feature = pack.feature("Jesus", "Movement", "MOVEMENT_TERRAIN")
    .description("feature.impl.movement.jesus");
var type = feature.mode("jesus.type", ["Grim 2.3.71"]);

// Касаемся воды, но не плывём — тогда бежим по ней.
function active() {
    return feature.enabled() && type.is("Grim 2.3.71") && player.exists() && player.inWater() && !player.swimming();
}

events.forced_sprint(function (e) {
    if (active()) e.setForced(true);
});

// На старых версиях протокола бег в воде сбрасывается — по воде бег оставляем.
events.keep_sprint(function (e) {
    if (e.stage === "legacy" && active()) return true;
});

// На воде аура и триггербот не ждут крита — как в воде без прыжка. Крит в прыжке решает клиент сам.
aura.hitMomentHook(function (ctx) {
    if (!active() || ctx.canCrit || ctx.noRoomToJump) return;
    return true;
});
