// @name {name}
// @author {author}
// @version 1.0
// @description Заготовка своего UI: панель модуля в ClickGui и свой экран по клавише

var mod = Module("{name}");
mod.description("Своя панель в ClickGui и свой экран по клавише");

// Модуль в категории Misc: шестерёнка его строки открывает НАШУ панель вместо стандартных настроек.
var feat = mod.feature("{name}", "Misc");
feat.description("Панель со своими виджетами и стандартными настройками");
var speed = feat.slider("Speed", 1.0, 0.1, 5.0, 0.1);
var glow = feat.checkbox("Glow", true);
var accent = feat.color("Accent", Color.accent());
var openKey = mod.bind("Open screen", 82); // R

var clicks = 0;

// Панель: ui = {x, y, w, h, mouseX, mouseY, alpha, partial}. Подложка, крестик и анимация — клиентские,
// всё внутри — наше. Строки настроек рисуют стандартные виджеты клиента: handle.render(x, y, w, alpha).
var panel = feat.panel({
    render: function (ui) {
        render.text("Панель {name}", ui.x + 10, ui.y + 30, Color.text(), 7);
        render.text("Кликов: " + clicks, ui.x + 10, ui.y + 42, Color.textSecondary(), 6);
        var y = ui.y + 56;
        y += speed.render(ui.x + 10, y, 150, ui.alpha);
        y += glow.render(ui.x + 10, y, 150, ui.alpha);
        y += accent.render(ui.x + 10, y, 150, ui.alpha);

        // Своя кнопка с подсветкой по ховеру — прямоугольник запоминаем в this для click.
        var bx = ui.x + 10, by = y + 6, bw = 80, bh = 14;
        var hover = inside(ui.mouseX, ui.mouseY, bx, by, bw, bh);
        render.container(bx, by, bw, bh, hover ? 1.0 : 0.7, 6);
        render.textCenter("Открыть экран", bx + bw / 2, by + 4, Color.text(), 6);
        this.button = {x: bx, y: by, w: bw, h: bh};
    },
    click: function (e) {
        if (speed.click(e.x, e.y, e.button) || glow.click(e.x, e.y, e.button) || accent.click(e.x, e.y, e.button)) return true;
        var b = this.button;
        if (b && inside(e.x, e.y, b.x, b.y, b.w, b.h)) {
            screen.open();
            return true;
        }
    },
    release: function (e) {
        speed.release(e.x, e.y, e.button);
        glow.release(e.x, e.y, e.button);
        accent.release(e.x, e.y, e.button);
    },
    drag: function (e) {
        speed.drag(); // ползунок тянется, пока кнопка зажата
    }
});

// Свой экран (как ClickGui): пустой холст на весь экран в scale-пространстве клиента.
// escClose: true — ESC закрывает; pause: true — ставить игру на паузу (в одиночке).
var screen = Screen({
    escClose: true,
    pause: false,
    render: function (ui) {
        render.blur(0, 0, ui.w, ui.h, 0, 0.6);
        render.rect(0, 0, ui.w, ui.h, Color.alpha(Color.background(), 0.35));
        var w = 200, h = 110, x = (ui.w - w) / 2, y = (ui.h - h) / 2;
        render.container(x, y, w, h, 1.0, 10);
        render.textCenter("Свой экран", ui.w / 2, y + 10, Color.text(), 9);
        render.textCenter("ЛКМ — счётчик, Space — сброс, ESC — закрыть", ui.w / 2, y + 26, Color.textSecondary(), 5.5);
        render.textCenter("" + clicks, ui.w / 2, y + 46, accent.get(), 14);
        render.textCenter("Speed " + speed.get().toFixed(1) + (glow.get() ? " · glow" : ""), ui.w / 2, y + 86, Color.textSecondary(), 6);
    },
    click: function (e) {
        clicks++;
        return true;
    },
    key: function (e) {
        if (e.press && e.key === 32) { // Space
            clicks = 0;
            return true;
        }
    }
});

// Экран по клавише (по умолчанию R, меняется в настройках модуля). Пока какой-то экран открыт,
// e.screenOpen = true — реагируем только если открыт именно наш, чтобы не дёргать его из чата/инвентаря.
events.key(function (e) {
    if (!mod.enabled() || !e.press || !openKey.isKey(e.key)) return;
    if (!e.screenOpen || screen.isOpen()) screen.toggle();
});

function inside(px, py, x, y, w, h) {
    return px >= x && px <= x + w && py >= y && py <= y + h;
}
