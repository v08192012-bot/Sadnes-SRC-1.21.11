// @name ElytraFly
// @author {author}
// @version 1.0
// @description Полёт на элитре без фейерверков: взлёт сам, высота набирается плавно

var pack = Module("ElytraFly", true);
var feature = pack.feature("ElytraFly", "Movement", "MOVEMENT_ELYTRA")
    .description("feature.impl.movement.elytrafly");
var mode = feature.mode("elytrafly.mode", ["HolyWorld"]);

function wearingElytra() {
    return player.equipment().armor[1].id === "minecraft:elytra"; // armor: шлем, нагрудник, поножи, ботинки
}

events.update(function () {
    if (!feature.enabled() || !player.exists()) return;
    if (move.speed() * 20 > 2 || !wearingElytra()) return;
    if (player.gliding()) {
        rotation.set(rotation.camera().yaw, 0, {speed: 999, returnSpeed: 999, timeout: 2, priority: 14});
        var velocity = player.velocity();
        move.velocity(velocity.x, velocity.y + 0.0305, velocity.z);
    } else if (player.onGround()) {
        move.holdJump(200);
    } else if (player.fallDistance() !== 0) {
        player.startGliding();
    }
});

// В элитре клавиши движения не тянут в сторону — полёт ведёт взгляд.
events.move_input(function (e) {
    if (!feature.enabled() || !player.exists() || !wearingElytra()) return;
    e.setForward(0);
    e.setStrafe(0);
});
