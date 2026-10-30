// @name {name}
// @author {author}
// @version 1.0
// @description Заготовка рендера: HUD-плашка, маркер в мире и свой шейдер

include("render_util.js"); // утилита: панелька с текстом и прогресс-баром

var mod = Module("{name}");
mod.description("HUD-плашка и светящийся маркер в мире");

var showHud = mod.checkbox("HUD", true);
var showWorld = mod.checkbox("World marker", true);
var glowSize = mod.slider("Glow size", 1.5, 0.5, 3.0, 0.1);

var fx = null;

// 2D: рисуем каждый кадр поверх игры (координаты в scale-пространстве клиента).
events.render2d(function (partial) {
    if (!mod.enabled() || !showHud.get()) return;
    var health = player.exists() ? player.health() / player.maxHealth() : 0;
    RenderUtil.panelWithBar(8, 40, 90, "HP", health, Color.accent());
});

// 3D: маркер под собой — шейдерное свечение из assets/glow.fsh + сфера.
events.render3d(function (partial) {
    if (!mod.enabled() || !showWorld.get() || !player.exists()) return;
    if (!fx) fx = render3d.shader("glow.fsh");
    var pos = player.renderPos();
    if (fx) fx.draw(pos.x, pos.y + 0.1, pos.z, glowSize.get(), Color.accent(), 1.0, 0, 0, 0);
    render3d.sphere(pos.x, pos.y + 1.0, pos.z, 0.6, Color.alpha(Color.accent(), 0.25), 3, true, 1.2);
});
