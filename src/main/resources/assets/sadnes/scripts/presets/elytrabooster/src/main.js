// @name ElytraBooster
// @author {author}
// @version 1.0
// @description Сильнее разгоняет фейерверк в полёте на элитре

var pack = Module("ElytraBooster", true);
var feature = pack.feature("ElytraBooster", "Movement", "MOVEMENT_ELYTRA")
    .description("feature.impl.movement.elytrabooster");

// Тумблер группы включает разгон по углу наклона (шаг 5°), выключенный — одно значение на все углы.
var xzAngles = feature.group("elytrabooster.xzAngles", false);
var speedXZ = xzAngles.slider("elytrabooster.speedXZ", 1.65, 1.5, 5.0, 0.01);
var xzByAngle = [
    xzAngles.slider("elytrabooster.xz_0_5", 1.6, 1.5, 5.0, 0.01),
    xzAngles.slider("elytrabooster.xz_5_10", 1.62, 1.5, 5.0, 0.01),
    xzAngles.slider("elytrabooster.xz_10_15", 1.65, 1.5, 5.0, 0.01),
    xzAngles.slider("elytrabooster.xz_15_20", 1.68, 1.5, 5.0, 0.01),
    xzAngles.slider("elytrabooster.xz_20_25", 1.74, 1.5, 5.0, 0.01),
    xzAngles.slider("elytrabooster.xz_25_30", 1.8, 1.5, 5.0, 0.01),
    xzAngles.slider("elytrabooster.xz_30_35", 1.8, 1.5, 5.0, 0.01),
    xzAngles.slider("elytrabooster.xz_35_40", 1.8, 1.5, 5.0, 0.01)
];

var yAngles = feature.group("elytrabooster.yAngles", false);
var speedY = yAngles.slider("elytrabooster.speedY", 1.59, 1.5, 5.0, 0.01);
var yByAngle = [
    yAngles.slider("elytrabooster.y_0_5", 1.59, 1.5, 5.0, 0.01),
    yAngles.slider("elytrabooster.y_5_10", 1.6, 1.5, 5.0, 0.01),
    yAngles.slider("elytrabooster.y_10_15", 1.61, 1.5, 5.0, 0.01),
    yAngles.slider("elytrabooster.y_15_20", 1.62, 1.5, 5.0, 0.01),
    yAngles.slider("elytrabooster.y_20_25", 1.68, 1.5, 5.0, 0.01),
    yAngles.slider("elytrabooster.y_25_30", 1.74, 1.5, 5.0, 0.01),
    yAngles.slider("elytrabooster.y_30_35", 1.95, 1.5, 5.0, 0.01),
    yAngles.slider("elytrabooster.y_35_40", 2.0, 1.5, 5.0, 0.01)
];

events.firework_boost(function (e) {
    if (!feature.enabled() || !player.exists() || !e.shooter || e.shooter.id() !== player.wrap().id()) return;
    var index = Math.min(Math.floor(Math.abs(player.pitch()) / 5), 7);
    e.setMultipliers(
        xzAngles.get() ? xzByAngle[index].get() : speedXZ.get(),
        yAngles.get() ? yByAngle[index].get() : speedY.get());
});
