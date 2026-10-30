// Утилита 2D-рендера: подключается из main.js через include("render_util.js").
// Панелька в стиле клиента: стекло, заголовок, значение и прогресс-бар.

var RenderUtil = {

    // Панель с подписью и горизонтальным баром (progress 0..1) — возвращает высоту.
    panelWithBar: function (x, y, width, label, progress, accent) {
        var height = 26;
        render.panel(x, y, width, height, 1.0, 6);
        render.text(label, x + 6, y + 5, Color.text(), 6.5);
        var value = Math.round(progress * 100) + "%";
        render.textRight(value, x + width - 6, y + 5, Color.textSecondary(), 6.0);

        var barY = y + height - 9;
        render.rect(x + 6, barY, width - 12, 3, Color.alpha(Color.textSecondary(), 0.25), 1.5);
        var fill = Math.max(0, Math.min(1, progress));
        if (fill > 0.01) {
            render.rect(x + 6, barY, (width - 12) * fill, 3, accent, 1.5);
        }
        return height;
    }
};
