// @name AutoDodge
// @author {author}
// @version 1.0
// @description Уворачивается от удара булавой сверху: прикрывается пластом или убегает в свободную сторону

var pack = Module("AutoDodge", true);
var feature = pack.feature("AutoDodge", "Movement", "MOVEMENT_ASSIST")
    .description("feature.impl.movement.autododge");
var threats = feature.multi("autododge.threats", ["mace"], ["mace"]);
var plastShield = feature.checkbox("autododge.plast", true);

var plastWatch = Timer();
var groundLevels = {}; // id игрока → высота, где он последний раз стоял на земле
var threat = null;
var escape = null;     // {x, z} — куда бежим, null — никуда

function reset() {
    groundLevels = {};
    threat = null;
    escape = null;
}

feature.onDisabled(reset);
events.disable(reset);

events.update(function () {
    if (!feature.enabled()) return;
    if (!player.exists()) {
        reset();
        return;
    }
    var players = world.players();
    trackGround(players);
    threat = threats.enabled("mace") ? findMaceThreat(players) : null;
    var shielded = threat !== null && inPlastRange(threat) && shieldWithPlast();
    escape = threat === null || shielded || stunned() ? null : move.escapeFrom(threat);
    if (!escape) return;
    var escapeYaw = Math.atan2(-escape.x, escape.z) * 180 / Math.PI;
    rotation.set(escapeYaw, player.pitch(), {speed: 60, returnSpeed: 60, timeout: 2, priority: 5});
});

// Низкий приоритет: руль побега ложится поверх ввода остальных модулей.
events.move_input(function (e) {
    if (!escape || !feature.enabled() || !player.exists()) return;
    e.steer(escape.x, escape.z);
}, "low");

events.forced_sprint(function (e) {
    if (escape && feature.enabled() && e.stage === "afsprint") e.setForced(true);
});

function trackGround(players) {
    var present = {};
    for (var i = 0; i < players.length; i++) {
        var other = players[i];
        present[other.id()] = true;
        if (other.onGround()) groundLevels[other.id()] = other.y();
    }
    for (var id in groundLevels) {
        if (!present[id]) delete groundLevels[id];
    }
}

// Ныряет сверху с булавой: выше нас, падает, ушёл ниже точки прыжка, рядом по горизонтали и в зоне видимости.
function findMaceThreat(players) {
    if (threat && stillDiving(threat)) return threat;
    var top = player.box().y2;
    for (var i = 0; i < players.length; i++) {
        var other = players[i];
        if (!other.alive()) continue;
        var hand = other.mainHand();
        if (!hand || hand.id !== "minecraft:mace") continue;
        if (other.y() <= top) continue;
        if (other.prevPos().y - other.y() < 0.05) continue;
        if (droppedBelowLaunch(other) < 0.1) continue;
        if (horizontalDistanceSq(other) > 7 * 7) continue;
        if (client.isFriend(other.name())) continue;
        if (!other.canSee()) continue;
        return other;
    }
    return null;
}

function stillDiving(other) {
    return other.alive() && !other.onGround() && other.prevPos().y - other.y() > 0 && horizontalDistanceSq(other) <= 7 * 7;
}

function droppedBelowLaunch(other) {
    var launch = groundLevels[other.id()];
    return launch === undefined ? Number.MAX_VALUE : launch - other.y();
}

function inPlastRange(other) {
    return other.y() - player.y() >= 1.5 && horizontalDistanceSq(other) <= 3.5 * 3.5;
}

function horizontalDistanceSq(other) {
    var dx = other.x() - player.x();
    var dz = other.z() - player.z();
    return dx * dx + dz * dz;
}

// Оглушение сервера (замедление X + слабость I) — бежать бесполезно.
function stunned() {
    var slowness = player.effect("minecraft:slowness");
    var weakness = player.effect("minecraft:weakness");
    return slowness && weakness && slowness.level === 10 && weakness.level === 1;
}

// Пласт под себя: пока он летит (полсекунды), считаем, что прикрыты.
function shieldWithPlast() {
    if (!plastShield.get()) return false;
    if (!plastWatch.passed(500)) return true;
    var slot = player.inventory.findServerItem("plast");
    if (slot === -1 || container.slot(slot).onCooldown) return false;
    if (player.inventory.throwing()) return false;
    plastWatch.reset();
    rotation.set(player.yaw(), -90, {speed: 999, returnSpeed: 999, timeout: 2, priority: 6});
    player.inventory.throwItem(slot, player.yaw(), -90);
    return true;
}
