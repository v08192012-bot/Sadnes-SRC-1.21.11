// @name {name}
// @author {author}
// @version 1.0
// @description Мост под ногами: блок ставится только туда, куда игрок действительно смотрит

var mod = Module("{name}");
mod.description("Строит мост под ногами на ходу и приседает у края");

var turnSpeed = mod.slider("Turn speed", 45, 10, 180, 5);
var autoSwap = mod.checkbox("Take blocks", true);
var eagle = mod.checkbox("Sneak at edge", true);
var highlight = mod.checkbox("Highlight", true);

var target = null; // опора этого тика: {side, pos, block, target} из world.placeTarget

// Клетка под ногами через тик — на ходу опору ищем заранее, а не когда уже падаем.
function nextSupportCell() {
    var next = player.predict(1);
    return {x: next.x, y: Math.floor(player.y()) - 1, z: next.z};
}

// Хотбар-слот с блоком; активный слот не трогаем, если в руке уже блок.
function selectBlocks() {
    if (player.mainHand().block) return true;
    var hotbar = player.inventory.hotbar();
    for (var i = 0; i < hotbar.length; i++) {
        if (hotbar[i].block && hotbar[i].count > 0) {
            player.slot(i);
            return true;
        }
    }
    return false;
}

function sameBlock(a, b) {
    return a && b && a.x === b.x && a.y === b.y && a.z === b.z;
}

// pre_update — точка, где крутят и встроенные модули: до клика и до пакета движения этого тика.
events.pre_update(function () {
    target = null;
    if (!mod.enabled() || !player.exists()) return;
    var cell = nextSupportCell();
    if (!world.block(cell.x, cell.y, cell.z).replaceable) return;
    target = world.placeTarget(cell.x, cell.y, cell.z);
    if (!target || (autoSwap.get() && !selectBlocks())) return;

    rotation.lookAt(target.pos.x, target.pos.y, target.pos.z, {speed: turnSpeed.get(), returnSpeed: 25, timeout: 4});

    // Ставим, только когда луч серверной ротации уже упирается в нужную грань опоры.
    var hit = world.raycast();
    if (hit && hit.type === "block" && hit.side === target.side && sameBlock(hit.block, target.block)) {
        player.place(hit);
    }
});

// У края приседаем — ванильное приседание не даёт сойти с блока, пока опора не встала.
events.move_input(function (e) {
    if (!mod.enabled() || !eagle.get() || !player.exists() || !player.onGround()) return;
    var cell = nextSupportCell();
    if (world.block(cell.x, cell.y, cell.z).replaceable) e.setSneaking(true);
});

// Пока строим, бег выключен: взгляд назад-вниз и спринт вперёд вместе выглядят неестественно.
events.player_input(function (e) {
    if (mod.enabled() && target) e.sprint(false);
});

events.render3d(function () {
    if (!mod.enabled() || !highlight.get() || !target) return;
    var accent = Color.accent();
    render3d.blockBox(target.target.x, target.target.y, target.target.z, Color.alpha(accent, 0.2), accent);
});
