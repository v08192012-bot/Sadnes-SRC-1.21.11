// @name FastLadder
// @author {author}
// @version 1.0
// @description Быстрый подъём и спуск по лестницам

var pack = Module("FastLadder", true);
var feature = pack.feature("FastLadder", "Movement", "MOVEMENT_TERRAIN")
    .description("feature.impl.movement.fastladder");

events.move(function (e) {
    if (!feature.enabled() || !player.exists()) return;
    var box = player.box();
    if (world.block(player.x(), (box.y1 + box.y2) / 2, player.z()).id !== "minecraft:ladder") return;
    e.setOnGround(true);
    player.setOnGround(true);
    var controls = player.movementInput();
    if (controls && controls.jump) {
        e.setY(0.6);
        e.cancel(); // без отмены setY не применяется
    }
});
