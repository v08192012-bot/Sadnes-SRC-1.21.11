// @name AutoUse
// @author {author}
// @version 1.0
// @description Ест при голоде и поддерживает невидимость

var pack = Module("AutoUse", true);
var feature = pack.feature("AutoUse", "Combat", "COMBAT_INVENTORY")
    .description("feature.impl.combat.autouse");
var modes = feature.multi("autouse.modes", ["food", "invis"]);
var hunger = feature.slider("autouse.hunger", 17, 0, 20, 1)
    .visibleIf(function () { return modes.enabled("food"); });
var invisAction = feature.mode("autouse.invisAction", ["auto_drink", "notify"])
    .visibleIf(function () { return modes.enabled("invis"); });

var INVIS = "minecraft:invisibility";
var POTION = "minecraft:potion";
// Золотые яблоки и хорус едой не считаем: их берегут для боя и телепорта.
var SKIPPED_FOOD = ["minecraft:golden_apple", "minecraft:enchanted_golden_apple", "minecraft:chorus_fruit"];

var task = null;        // "food" / "invis" — что сейчас едим или пьём из левой руки
var restoreSlot = -1;   // слот экрана, откуда взяли предмет в левую руку; -1 — он уже был в руке
var reached = false;    // предмет уже доехал до левой руки
var invisWarned = false;
var swapWatch = Timer();

events.disable(stopUse);

events.update(function () {
    if (!player.exists()) return;
    if (!feature.enabled()) {
        stopUse();
        return;
    }

    var invis = modes.enabled("invis");
    if (invis && invisAction.is("notify")) warnInvisEnding();

    if (task) {
        continueUse();
        return;
    }

    if (player.inventory.busy()) return;

    if (modes.enabled("food") && player.hunger() <= hunger.get()) {
        start("food", player.offHand().food, bestFoodSlot);
        return;
    }

    if (invis && invisAction.is("auto_drink")) {
        var effect = player.effect(INVIS);
        if (!effect || effect.duration < 100) start("invis", isInvisPotion(player.offHand()), invisPotionSlot);
    }
});

function warnInvisEnding() {
    var effect = player.effect(INVIS);
    if (effect && effect.duration > 0 && effect.duration <= 400) {
        if (!invisWarned) {
            client.warn(client.lang("autouse.invisEnding", "Невидимость скоро закончится"), {effect: INVIS});
            invisWarned = true;
        }
    } else {
        invisWarned = false;
    }
}

// Нужный предмет уже в левой руке — едим сразу, иначе берём его из инвентаря и ждём, пока доедет.
function start(kind, alreadyInHand, findSlot) {
    if (alreadyInHand) {
        begin(kind, -1);
        return;
    }
    var slot = findSlot();
    if (slot === -1) return;
    player.inventory.swapHand(slot, "off");
    begin(kind, slot);
}

function begin(kind, slot) {
    task = kind;
    restoreSlot = slot;
    reached = slot === -1;
    swapWatch.reset();
}

function continueUse() {
    if (player.inventory.busy()) return;
    var offHand = player.offHand();
    var inHand = task === "food" ? offHand.food : isInvisPotion(offHand);
    if (!reached) {
        if (inHand) reached = true;
        else if (swapWatch.passed(2000)) stopUse();
        return;
    }

    var done = task === "food" ? player.hunger() > hunger.get() : invisLasts();
    if (done || !inHand) {
        stopUse();
        return;
    }

    // С открытым экраном игра не обрабатывает зажатую ПКМ — жмём сами, пока не начнём есть.
    if (client.screenOpen() && !player.usingItem()) player.rightClick();
    else input.hold("use");
}

function stopUse() {
    if (!task) return;
    input.release("use");
    if (restoreSlot !== -1) player.inventory.swapHand(restoreSlot, "off");
    restoreSlot = -1;
    task = null;
}

function invisLasts() {
    var effect = player.effect(INVIS);
    return effect && effect.duration > 100;
}

function isInvisPotion(item) {
    return item.id === POTION && hasInvis(item.effects);
}

function hasInvis(effects) {
    for (var i = 0; i < effects.length; i++) {
        if (effects[i].id === INVIS) return true;
    }
    return false;
}

// Самая сытная еда в инвентаре: сытость + сытость × насыщение.
function bestFoodSlot() {
    var best = -1;
    var bestScore = -1;
    var slots = container.slots();
    for (var i = 0; i < slots.length; i++) {
        var it = slots[i];
        if (!it.playerInventory || !it.food || SKIPPED_FOOD.indexOf(it.id) !== -1) continue;
        var score = it.nutrition + it.nutrition * it.saturation;
        if (score > bestScore) {
            bestScore = score;
            best = it.slot;
        }
    }
    return best;
}

// Зелье невидимости; чистое (без других эффектов) — в приоритете.
function invisPotionSlot() {
    var single = -1;
    var any = -1;
    var slots = container.slots();
    for (var i = 0; i < slots.length; i++) {
        var it = slots[i];
        if (!it.playerInventory || !isInvisPotion(it)) continue;
        if (it.effects.length === 1) {
            if (single === -1) single = it.slot;
        } else if (any === -1) {
            any = it.slot;
        }
    }
    return single !== -1 ? single : any;
}
