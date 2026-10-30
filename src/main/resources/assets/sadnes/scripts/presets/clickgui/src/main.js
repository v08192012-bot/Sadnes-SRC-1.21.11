// @name {name}
// @author {author}
// @version 1.0
// @description Свой ClickGui на скрипте: категории, модули, тоглы, настройки, поиск, перетаскивание

var mod = Module("{name}");
mod.description("Своё меню модулей вместо ClickGui — открывается по клавише");
var openKey = mod.bind("Open", 345);                  // Right Ctrl
var panelW = mod.slider("Width", 400, 320, 640, 10);
var panelH = mod.slider("Height", 280, 200, 440, 10);
var showDesc = mod.checkbox("Descriptions", true);

// ── Геометрия ──
var SIDEBAR = 92, HEADER = 26, ROW = 18, GAP = 3, PAD = 6, RADIUS = 10;

// ── Состояние меню ──
var cats = [], moduleCache = {}, settingsCache = {}, settingsH = {};
var selected = 0, expanded = null, query = "", searchFocused = false;
var scrollTarget = {}, px = -1, py = -1, dragging = false;
var rows = [], catRows = [], contentArea = null, searchBox = null, closeBox = null, hoverInfo = "";

// ── Анимации: экспоненциальное сглаживание по времени кадра ──
var anims = {}, lastFrame = Date.now(), frameDt = 0;
function anim(key, target, speed) {
    var v = anims[key];
    if (v === undefined) v = target;
    v += (target - v) * (1 - Math.exp(-speed * frameDt));
    if (Math.abs(target - v) < 0.0005) v = target;
    anims[key] = v;
    return v;
}
function inside(mx, my, x, y, w, h) {
    if (typeof x === "object") { var b = x; x = b.x; y = b.y; w = b.w; h = b.h; }
    return mx >= x && mx <= x + w && my >= y && my <= y + h;
}

// ── Экран ──
var gui = Screen({
    escClose: true,
    pause: false,
    open: function () {
        cats = client.categories();
        moduleCache = {}; settingsCache = {}; settingsH = {};
        expanded = null; searchFocused = false; dragging = false;
        if (selected >= cats.length) selected = 0;
    },
    render: function (ui) { renderGui(ui); },
    click: function (e) { return onClick(e); },
    release: function (e) { onRelease(e); },
    drag: function (e) { onDrag(e); },
    scroll: function (e) { return onScroll(e); },
    key: function (e) { return onKey(e); },
    char: function (e) { return onChar(e); }
});

// Открыть/закрыть по клавише. Пока открыт чужой экран (чат, инвентарь) — не реагируем.
events.key(function (e) {
    if (!mod.enabled() || !e.press || !openKey.isKey(e.key)) return;
    if (!e.screenOpen || gui.isOpen()) gui.toggle();
});

// ── Данные: хэндлы модулей и настроек кэшируем на время открытого меню ──
function catModules(c) {
    var list = moduleCache[c.name];
    if (!list) { list = c.modules(); moduleCache[c.name] = list; }
    return list;
}
function settingsOf(m) {
    var list = settingsCache[m.name];
    if (!list) { list = m.settings(); settingsCache[m.name] = list; }
    return list;
}
function expandedSettings() {
    return expanded ? settingsCache[expanded] || [] : [];
}
function overlayOpen() {
    var list = expandedSettings();
    for (var i = 0; i < list.length; i++) if (list[i].overlayOpen()) return true;
    return false;
}
function scrollKey() {
    return query.length ? "$search" : (cats[selected] ? cats[selected].name : "");
}
// Строки списка: модули выбранной категории по подкатегориям, при поиске — по всем категориям.
function listEntries() {
    var q = query.toLowerCase(), out = [];
    if (q.length) {
        for (var i = 0; i < cats.length; i++) {
            var ms = catModules(cats[i]);
            for (var j = 0; j < ms.length; j++) {
                if (ms[j].name.toLowerCase().indexOf(q) >= 0) out.push({m: ms[j], group: cats[i].name});
            }
        }
        return out;
    }
    var c = cats[selected];
    if (!c) return out;
    var list = catModules(c);
    for (var k = 0; k < list.length; k++) out.push({m: list[k], group: list[k].subcategory});
    return out;
}

// ── Рендер ──
function renderGui(ui) {
    var now = Date.now();
    frameDt = Math.min(0.1, (now - lastFrame) / 1000);
    lastFrame = now;
    var W = panelW.get(), H = panelH.get();
    if (px < 0) { px = (ui.w - W) / 2; py = (ui.h - H) / 2; }
    px = Math.max(0, Math.min(ui.w - W, px));
    py = Math.max(0, Math.min(ui.h - H, py));

    render.blur(0, 0, ui.w, ui.h, 0, 0.55);
    render.rect(0, 0, ui.w, ui.h, Color.alpha(Color.background(), 0.25));
    render.glass(px, py, W, H, 1.0, RADIUS);
    render.outline(px, py, W, H, 1, Color.alpha(Color.text(), 0.06), RADIUS);

    renderSidebar(ui, H);
    renderHeader(ui, W);
    renderContent(ui, W, H);
}

function renderSidebar(ui, H) {
    var x = px + PAD, y = py + PAD, w = SIDEBAR - PAD * 2;
    render.text("sadnes", x + 5, y + 5, Color.text(), 8);
    render.text("scripted gui", x + 5, y + 15, Color.alpha(Color.textSecondary(), 0.7), 5);
    y += 28;
    catRows = [];
    for (var i = 0; i < cats.length; i++) {
        var c = cats[i], h = 17;
        var hov = inside(ui.mouseX, ui.mouseY, x, y, w, h);
        var sel = anim("cat:" + c.name, i === selected && !query.length ? 1 : 0, 14);
        var hv = anim("cath:" + c.name, hov ? 1 : 0, 18);
        if (sel > 0.01) render.rect(x, y, w, h, Color.alpha(Color.accent(), 0.16 * sel), 6);
        if (hv > 0.01) render.hover(x, y, w, h, 0.6 * hv, 6);
        var col = Color.mix(Color.textSecondary(), Color.text(), Math.max(sel, hv * 0.5));
        render.icon(c.icon, x + 5, y + 4, 9, col);
        render.text(c.name, x + 18, y + (h - render.textHeight(6.5)) / 2, col, 6.5);
        catRows.push({x: x, y: y, w: w, h: h});
        y += h + 2;
    }
    render.text(client.username(), x + 5, py + H - 12, Color.alpha(Color.textSecondary(), 0.6), 5);
}

function renderHeader(ui, W) {
    var x = px + SIDEBAR, y = py + PAD, w = W - SIDEBAR - PAD, h = HEADER - PAD;
    var sw = w - 24;
    render.container(x, y, sw, h, searchFocused ? 1.0 : 0.7, 7);
    if (searchFocused) render.outline(x, y, sw, h, 1, Color.alpha(Color.accent(), 0.7), 7);
    render.icon("search", x + 6, y + (h - 8) / 2, 8, Color.textSecondary());
    var caret = searchFocused && Date.now() % 1000 < 500 ? "_" : "";
    var placeholder = !query.length && !searchFocused;
    var text = placeholder ? "Поиск модулей (Ctrl+F)" : query + caret;
    var col = placeholder ? Color.alpha(Color.textSecondary(), 0.8) : Color.text();
    render.text(text, x + 18, y + (h - render.textHeight(6.5)) / 2, col, 6.5);
    searchBox = {x: x, y: y, w: sw, h: h};

    var cx = x + w - 14, cy = y + (h - 10) / 2;
    closeBox = {x: cx - 3, y: cy - 3, w: 16, h: 16};
    var hv = anim("close", inside(ui.mouseX, ui.mouseY, closeBox) ? 1 : 0, 18);
    render.icon("close", cx, cy, 10, Color.mix(Color.textSecondary(), Color.text(), hv));
}

function renderContent(ui, W, H) {
    var x = px + SIDEBAR, y = py + HEADER + 2, w = W - SIDEBAR - PAD, h = H - HEADER - PAD - 10;
    var sc = anim("scroll:" + scrollKey(), scrollTarget[scrollKey()] || 0, 16);
    var entries = listEntries();
    // Под открытым дропдауном и вне зоны списка ховера у строк нет
    var blocked = overlayOpen() || !inside(ui.mouseX, ui.mouseY, x, y, w, h);
    var mx = blocked ? -9999 : ui.mouseX, my = blocked ? -9999 : ui.mouseY;
    rows = [];
    hoverInfo = "";
    var contentH = 0;
    render.clip(x, y, w, h, function () {
        var cy = y + sc, lastGroup = null, rw = w - 4;
        for (var i = 0; i < entries.length; i++) {
            var m = entries[i].m, group = entries[i].group;
            if (group !== lastGroup) {
                if (group) {
                    render.text(group, x + 4, cy + 2, Color.alpha(Color.textSecondary(), 0.7), 5.5);
                    cy += 11;
                }
                lastGroup = group;
            }
            var hov = inside(mx, my, x, cy, rw, ROW);
            var hv = anim("h:" + m.name, hov ? 1 : 0, 18);
            var on = anim("t:" + m.name, m.enabled() ? 1 : 0, 14);
            var isExp = expanded === m.name;
            var ex = anim("x:" + m.name, isExp ? 1 : 0, 12);
            var innerH = (settingsH[m.name] || 0) * ex;

            render.container(x, cy, rw, ROW + innerH, 0.75, 7);
            if (hv > 0.01) render.hover(x, cy, rw, ROW, 0.6 * hv, 7);
            var nameCol = Color.mix(Color.textSecondary(), Color.text(), Math.max(on, hv * 0.5));
            render.text(m.name, x + 7, cy + (ROW - render.textHeight(6.5)) / 2, nameCol, 6.5);

            var tw = 14, th = 9, tx = x + rw - 6 - tw, ty = cy + (ROW - th) / 2;
            render.toggle(tx, ty, tw, th, on, 1);
            var gear = null;
            if (m.hasSettings()) {
                var gx = tx - 14, gy = cy + (ROW - 8) / 2;
                gear = {x: gx - 2, y: gy - 2, w: 12, h: 12};
                var gh = anim("g:" + m.name, inside(mx, my, gear) ? 1 : 0, 18);
                render.icon("settings", gx, gy, 8, Color.alpha(Color.mix(Color.textSecondary(), Color.text(), Math.max(gh, ex)), 0.9));
            }
            if (hov) hoverInfo = m.description();
            rows.push({m: m, x: x, y: cy, w: rw, h: ROW, gear: gear});

            // Настройки раскрытого модуля — стандартные строки клиента, оверлеи откладываем (deferOverlay)
            if (ex > 0.01) {
                var list = settingsOf(m), measured = 0;
                render.clip(x, cy + ROW, rw, innerH, function () {
                    for (var k = 0; k < list.length; k++) {
                        measured += list[k].render(x + 8, cy + ROW + 2 + measured, rw - 16, ui.alpha * ex, true);
                    }
                });
                if (isExp) settingsH[m.name] = measured + 4;
            }
            cy += ROW + innerH + GAP;
        }
        contentH = cy - (y + sc);
    });
    // Дропдауны и колорпикеры — поверх списка, а не под следующими строками
    var open = expandedSettings();
    for (var o = 0; o < open.length; o++) open[o].overlay();
    contentArea = {x: x, y: y, w: w, h: h, max: Math.max(0, contentH - h)};

    if (showDesc.get() && hoverInfo) {
        var text = hoverInfo;
        while (text.length > 4 && render.textWidth(text, 5.5) > w - 4) text = text.slice(0, -4) + "...";
        render.text(text, x + 2, py + H - 12, Color.alpha(Color.textSecondary(), 0.8), 5.5);
    }
}

// ── Ввод ──
function onClick(e) {
    var open = expandedSettings();
    // Открытый дропдаун/колорпикер забирает клик первым
    for (var k = 0; k < open.length; k++) {
        if (open[k].overlayOpen()) { open[k].click(e.x, e.y, e.button); return true; }
    }
    if (e.button === 0 && closeBox && inside(e.x, e.y, closeBox)) { gui.close(); return true; }
    searchFocused = !!(searchBox && inside(e.x, e.y, searchBox));
    if (searchFocused) return true;
    for (var i = 0; i < catRows.length; i++) {
        if (inside(e.x, e.y, catRows[i])) {
            if (selected !== i || query.length) { selected = i; query = ""; expanded = null; }
            return true;
        }
    }
    if (contentArea && inside(e.x, e.y, contentArea)) {
        for (var r = 0; r < rows.length; r++) {
            var row = rows[r];
            if (!inside(e.x, e.y, row)) continue;
            if (row.gear && (inside(e.x, e.y, row.gear) || e.button === 1)) { toggleExpand(row.m); return true; }
            if (e.button === 0) row.m.toggle();
            return true;
        }
        for (var s = 0; s < open.length; s++) if (open[s].click(e.x, e.y, e.button)) return true;
        return true;
    }
    // Тянуть окно можно за шапку и за «лого» в сайдбаре
    if (e.button === 0 && (inside(e.x, e.y, px, py, panelW.get(), HEADER) || inside(e.x, e.y, px, py, SIDEBAR, 34))) dragging = true;
    return true;
}
function toggleExpand(m) {
    expanded = expanded === m.name ? null : m.name;
    if (expanded) settingsOf(m);
}
function onRelease(e) {
    dragging = false;
    var open = expandedSettings();
    for (var k = 0; k < open.length; k++) open[k].release(e.x, e.y, e.button);
}
function onDrag(e) {
    if (dragging) { px += e.dx; py += e.dy; return; }
    var open = expandedSettings();
    for (var k = 0; k < open.length; k++) open[k].drag();
}
function onScroll(e) {
    if (!contentArea || !inside(e.x, e.y, contentArea)) return false;
    var key = scrollKey();
    var t = (scrollTarget[key] || 0) + e.vertical * 18;
    scrollTarget[key] = Math.max(-contentArea.max, Math.min(0, t));
    return true;
}
function onKey(e) {
    if (!e.press) return false;
    var open = expandedSettings();
    for (var k = 0; k < open.length; k++) if (open[k].key(e.key, e.scanCode, e.modifiers)) return true;
    if (e.key === 70 && (e.modifiers & 2)) { searchFocused = !searchFocused; return true; } // Ctrl+F
    if (!searchFocused) return false;                                                      // ESC → закрыть меню
    if (e.key === 259) query = query.slice(0, -1);                                          // Backspace
    else if (e.key === 256 || e.key === 257) searchFocused = false;                         // Esc / Enter
    return true;
}
function onChar(e) {
    var open = expandedSettings();
    for (var k = 0; k < open.length; k++) if (open[k].char(e.code, e.modifiers)) return true;
    if (!searchFocused) return false;
    if (query.length < 24 && e.code >= 32) query += e.char;
    return true;
}
