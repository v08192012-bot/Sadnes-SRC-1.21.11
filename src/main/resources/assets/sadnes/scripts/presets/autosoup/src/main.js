// @name AutoSoup
// @author {author}
// @version 1.0
// @description Съедает грибной суп, когда здоровье падает ниже порога, и выбрасывает пустую миску

var pack = Module("AutoSoup", true);
var feature = pack.feature("AutoSoup", "Combat", "COMBAT_INVENTORY")
    .description("feature.impl.combat.autosoup");
var health = feature.slider("autosoup.health", 10, 1, 20, 1);

var STEW = "minecraft:mushroom_stew";
var eatDelay = Timer();
var swapWatch = Timer();

var previousSlot = -1;  // слот хотбара, на который вернуться после супа из хотбара
var restoreSlot = -1;   // слот рюкзака, откуда суп пришёл в руку, — туда же уходит то, что было в руке
var ticksLeft = -1;     // 1 — съесть в этом тике, 0 — выбросить миску и вернуть руку
var waitingSwap = false;

function reset() {
    previousSlot = -1;
    restoreSlot = -1;
    ticksLeft = -1;
    waitingSwap = false;
}

events.disable(reset);

events.update(function () {
    if (!player.exists()) return;
    if (!feature.enabled()) {
        reset();
        return;
    }

    if (ticksLeft >= 0) {
        if (ticksLeft === 1) {
            player.interactItem("main");
        } else {
            player.dropHeld(true);
            if (restoreSlot !== -1) {
                player.inventory.swapHand(restoreSlot, "main");
                restoreSlot = -1;
            } else if (previousSlot !== -1) {
                player.slot(previousSlot);
            }
        }
        ticksLeft--;
        return;
    }

    // Суп из рюкзака едет в руку не мгновенно: ждём, пока он окажется в руке, но не дольше 2 секунд.
    if (waitingSwap) {
        if (player.heldItem() === STEW) {
            waitingSwap = false;
            ticksLeft = 1;
        } else if (swapWatch.passed(2000)) {
            waitingSwap = false;
            restoreSlot = -1;
        }
        return;
    }

    if (player.health() >= health.get() || !eatDelay.passed(300)) return;

    // find отдаёт первый слот: сперва хотбар 0–8, потом рюкзак 9–35 (броню и левую руку не трогаем).
    var soup = player.inventory.find(STEW);
    if (soup >= 0 && soup < 9) {
        previousSlot = player.inventory.selectedSlot();
        restoreSlot = -1;
        player.slot(soup);
        ticksLeft = 1;
        eatDelay.reset();
        return;
    }

    if (soup < 9 || soup >= 36 || player.inventory.busy()) return;

    previousSlot = -1;
    restoreSlot = soup;
    player.inventory.swapHand(soup, "main");
    waitingSwap = true;
    swapWatch.reset();
    eatDelay.reset();
});
