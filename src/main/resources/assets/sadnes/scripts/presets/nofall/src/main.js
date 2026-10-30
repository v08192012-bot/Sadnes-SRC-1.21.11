// @name NoFall
// @author {author}
// @version 1.0
// @description Гасит падение ведром воды и убирает отскок от слизи

var pack = Module("NoFall", true);
var feature = pack.feature("NoFall", "Movement", "MOVEMENT_AIR")
    .description("feature.impl.movement.nofall");
var mode = feature.mode("nofall.mode", ["legit_water"]);
var slime = feature.checkbox("nofall.slimeUnbounce", true);

var BUCKET = "minecraft:water_bucket";
var oldSlot = -1; // слот экрана, откуда достали ведро, — туда же вернём то, что было в руке

events.slime_bounce(function () {
    if (feature.enabled() && slime.get()) return true;
});

events.update(function () {
    if (!feature.enabled() || !player.exists() || !mode.is("legit_water")) return;
    var falling = player.fallDistance() >= 2.5 && !player.inWater();
    if (!falling && oldSlot === -1) return; // опора под ногами нужна только для ведра или возврата руки
    var ground = lowestBlock();
    if (!ground) return;
    if (falling && !ground.liquid) {
        catchFall(ground);
        return;
    }
    if (oldSlot !== -1) {
        player.inventory.swapHand(oldSlot, "main");
        oldSlot = -1;
    }
});

function catchFall(ground) {
    var bucket = bucketSlot();
    if (bucket === -1) return;
    if (player.heldItem() !== BUCKET && player.age() % 4 === 0) {
        if (oldSlot === -1) oldSlot = bucket;
        player.inventory.swapHand(bucket, "main");
        return;
    }
    var angle = rotation.angleTo(ground.x, ground.y, ground.z);
    rotation.set(angle.yaw, angle.pitch, {speed: 999, returnSpeed: 999, timeout: 2, priority: 5});
    var hit = world.raycast(angle.yaw, angle.pitch, 4.5, "source");
    if (hit && hit.type === "block" && player.heldItem() === BUCKET) {
        player.interactItem("main");
        player.place(hit, "main");
        player.swing("main");
    }
}

// Первая опора под ногами: твёрдый блок или жидкость.
function lowestBlock() {
    var x = player.x();
    var z = player.z();
    for (var y = Math.floor(player.y()) - 1, bottom = world.bottomY(); y >= bottom; y--) {
        var block = world.block(x, y, z);
        if (block.liquid || block.solid) return block;
    }
    return null;
}

// Ведро в нижних 36 слотах экрана — рюкзак и хотбар.
function bucketSlot() {
    var slots = container.slots();
    for (var i = slots.length - 36, n = slots.length; i < n; i++) {
        if (slots[i].id === BUCKET) return slots[i].slot;
    }
    return -1;
}
