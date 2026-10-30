// @name HighJump
// @author {author}
// @version 1.0
// @description Прыжки выше обычного: от слизи, разгоном или со снега

var pack = Module("HighJump", true);
var feature = pack.feature("HighJump", "Movement", "MOVEMENT_AIR")
    .description("feature.impl.movement.highjump");
var mode = feature.mode("highjump.mode", ["slime_bounce", "matrix", "snow"]);

var SNOW = "minecraft:snow"; // слой снега: так зовутся и блок, и предмет

var jumpHeight = 0;
var jumping = false;
var ticks = 0;
var snowStep = 0;
var snowReturnSlot = -1; // слот экрана, откуда снег ушёл в левую руку
var snowHeldSlot = -1;   // ячейка хотбара до переключения на снег

function reset() {
    restoreSnow();
    jumping = false;
    ticks = 0;
    snowStep = 0;
    snowReturnSlot = -1;
    snowHeldSlot = -1;
}

feature.onDisabled(reset);
events.disable(reset);

events.update(function () {
    if (!feature.enabled() || !player.exists()) return;
    if (mode.is("slime_bounce")) slimeBounce();
    else if (mode.is("matrix")) matrixTick();
    else if (mode.is("snow")) snowTick();
});

// ── Снег: ставим слой под ноги, прыгаем и на приземлении подбрасываем себя ──

function snowTick() {
    if (player.hurtTime() !== 0) return;
    var feet = world.block(player.x(), player.y(), player.z());

    if (snowStep === 0) {
        if (feet.id !== SNOW) {
            if (player.onGround() && placeSnow(feet.x, feet.y - 1, feet.z)) snowStep = 1;
        } else {
            snowStep = 1;
        }
        return;
    }

    if (snowStep === 1) {
        move.jump(true);
        snowStep = 2;
        return;
    }

    // Как только снова встали на снег — один тик броска вверх; после второго модуль выключается.
    if (player.onGround()) {
        if (feet.id === SNOW) {
            var velocity = player.velocity();
            move.velocity(velocity.x, 1, velocity.z);
            snowStep++;
        }
        return;
    }
    if (snowStep >= 4) feature.setEnabled(false);
}

function placeSnow(x, y, z) {
    var hotbar = hotbarSnow();
    if (hotbar !== -1) {
        if (player.inventory.selectedSlot() === hotbar) {
            placeAt(x, y, z, "main");
            return true;
        }
        if (snowHeldSlot === -1) snowHeldSlot = player.inventory.selectedSlot();
        player.slot(hotbar);
        return false;
    }
    if (player.offhandItem() === SNOW) {
        placeAt(x, y, z, "off");
        return true;
    }
    var inventory = inventorySnow();
    if (inventory !== -1) {
        snowReturnSlot = inventory;
        player.inventory.swapHand(inventory, "off");
    }
    return false;
}

function restoreSnow() {
    if (!player.exists()) return;
    if (snowHeldSlot !== -1) player.slot(snowHeldSlot);
    if (snowReturnSlot !== -1 && player.offhandItem() === SNOW) player.inventory.swapHand(snowReturnSlot, "off");
}

// Клик по верхней грани опоры (x, y, z) — слой ляжет в клетку ног.
function placeAt(x, y, z, hand) {
    var point = {x: x + 0.5, y: y + 1.0, z: z + 0.5};
    var angle = rotation.angleTo(point.x, point.y, point.z);
    rotation.set(angle.yaw, angle.pitch, {speed: 180, returnSpeed: 90, timeout: 2, priority: 8});
    player.place({side: "up", pos: point, block: {x: x, y: y, z: z}}, hand);
}

function hotbarSnow() {
    var hotbar = player.inventory.hotbar();
    for (var i = 0; i < 9; i++) {
        if (hotbar[i].id === SNOW) return i;
    }
    return -1;
}

// Снег в инвентаре — с конца: слоты экрана 44…9.
function inventorySnow() {
    var slots = container.slots();
    for (var i = Math.min(44, slots.length - 1); i >= 9; i--) {
        if (slots[i].id === SNOW) return slots[i].slot;
    }
    return -1;
}

// ── Слизь: высота прыжка = высота, с которой упали на слизь ──

function slimeBounce() {
    if (firstBlockBelow() !== "minecraft:slime_block") {
        jumpHeight = 0;
        return;
    }
    if (player.fallDistance() !== 0) jumpHeight = player.fallDistance();
    if (player.onGround() && jumpHeight !== 0) {
        var velocity = player.velocity();
        move.velocity(velocity.x, Math.min(1.0, Math.max(0.42, jumpHeight / 2.25)), velocity.z);
        jumpHeight = 0;
    }
}

function firstBlockBelow() {
    var x = player.x();
    var z = player.z();
    var start = player.y() < 0 ? Math.ceil(player.y()) : Math.floor(player.y()); // как (int) в Java
    for (var y = start, bottom = world.bottomY(); y >= bottom; y--) {
        var id = world.block(x, y, z).id;
        if (id !== "minecraft:air") return id;
    }
    return null;
}

// ── Разгон: раз в секунду на ходу — прыжок с добавкой вверх, пока не начнём падать ──

function matrixTick() {
    if (!jumping) {
        ticks = 0;
        return;
    }
    if (ticks === 1) {
        player.setOnGround(false);
        var velocity = player.velocity();
        move.velocity(velocity.x, 0.999, velocity.z);
    }
    if (player.verticalCollision() && ticks > 4) {
        jumping = false;
        return;
    }
    if (player.fallDistance() > 0) jumping = false;
    ticks++;
}

events.move_input(function (e) {
    if (!feature.enabled() || !player.exists() || !mode.is("matrix")) return;
    if (player.onGround() && player.age() % 20 === 0 && e.forward === 1) jumping = true;
});

events.pre_update(function () {
    if (!feature.enabled() || !player.exists() || !mode.is("matrix")) return;
    if (!jumping) {
        ticks = 0;
        return;
    }
    var velocity = player.velocity();
    if (velocity.y > 0) move.velocity(velocity.x, velocity.y + 0.0034999, velocity.z);
    if (ticks > 0) player.setOnGround(false);
});

// Пока летим вверх, сервер не тянет нас вниз.
events.packet(function (e) {
    if (!feature.enabled() || !jumping || !mode.is("matrix") || !e.receive || e.name !== "set_entity_motion") return;
    var data = e.data();
    if (data && data.entityId === player.wrap().id() && data.velocity.y < -0.0625) return true;
});
