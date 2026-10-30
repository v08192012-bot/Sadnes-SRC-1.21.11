# sadnes scripts — agent guide

This folder holds JavaScript scripts of the sadnes Minecraft client. While the game is running, the client serves an MCP server at `%URL%` — connect to it and use its tools instead of editing files blindly: they hot-reload scripts, report compile/lint errors, show logs and can run JS in a live scope.

## Connect

- **Claude Code**: start `claude` in this folder — `.mcp.json` here is picked up automatically.
- **Codex CLI**: add to `~/.codex/config.toml`:
  ```toml
  [mcp_servers.sadnes]
  url = "%URL%"
  ```
- **Any MCP client (streamable HTTP)**: `%URL%`.
- **stdio-only clients**: `npx mcp-remote %URL%`.

The port may shift by +1..9 if several clients run at once — this file is rewritten with the current URL each time the MCP server starts.

## Workflow

Scripts run in an isolated JavaScript environment. Use the documented API: Java packages,
reflection, raw game objects, native libraries and process execution are unavailable.
`include()` reads only `.js` files under the script's `src/`; asset paths stay inside the
script's directory and cannot traverse links or junctions. A disabled script loads metadata
only; enable it before running its body or using `run_js` in its scope. Existing scripts that
used `raw()`, `assets.exec`, `assets.library` or `assets.loadNative` must migrate to the API.

1. `get_api_docs` — read the API first, it is custom (also duplicated below and in API.md).
2. `list_scripts` — what exists, what is enabled, what failed.
3. `read_file` / `write_file` — edit code; write hot-reloads immediately and returns errors, always check its result. `check_syntax` validates code without writing.
4. `set_script_enabled` / `set_module_enabled`, then `get_logs` to see print() and errors.
5. `run_js` — execute a snippet in the live scope of a script for debugging.

`create_script` accepts an optional `preset`: `rotation` (custom aura rotation with a math helper) `render` (module + HUD + world render + custom shader) `ui` (custom panel in ClickGui + own screen) `esp` (own ESP element and nametag segments) `clickgui` (a complete module menu) or `scaffold` (bridge building with legit rotation and placement). Presets are working examples of the idioms — prefer starting from one.

## World actions

Turn the head for the server with `rotation.lookAt(x, y, z, {speed})` inside `events.pre_update`, find where to click with `world.placeTarget(x, y, z)`, check `world.raycast()` hits that face, then `player.place(hit)`. Breaking — `player.breakBlock(x, y, z)` every tick; hitting — `player.attack(entity)`; keys — `input.press(name, ticks)`. Every handler runs on the main thread, actions are sent before the tick's movement packet. See the `scaffold` preset.

## Script structure

```
my_script/
  src/main.js     — entry point (other .js files are attached via include("utils.js"))
  assets/         — data and render assets: json, png, glsl
```

Header meta in the first comment lines of main.js: `// @name`, `// @author`, `// @version`, `// @description`.

Minimal script:
```js
// @name Example
// @description What it does

var mod = Module("Example");        // module shown in the GUI scripts page
mod.description("Visible hint");
var speed = mod.slider("Speed", 1.0, 0.1, 5.0, 0.1);

events.tick(function () {
    if (!mod.enabled()) return;
    // runs every game tick
});
```

## Aura hooks (rotation and target ESP modes)

`aura.rotation("Name", fn)` and `aura.esp("Name", fn)` register a custom mode: the name appears in the client's own mode lists (aura rotation modes / Target ESP styles) while the script is enabled, and the client calls the script instead of its built-in implementation.

- rotation fn gets `{target, yaw, pitch, distance, range, cooldown}` and returns `{yaw, pitch, yawSpeed, pitchSpeed, returnSpeed[, pitchReturnSpeed]}` (return nothing to skip the tick). Combat geometry helpers: `aura.closestPoint`, `aura.bestVisiblePoint`, `aura.angleToPoint`, `aura.canSeePoint`, `aura.rayTraceYaw/rayTracePitch`, `aura.lookVec`, `aura.aimDistance`.
- esp fn gets `{target, color, alpha, partial, x, y, z, width, height}` (x/y/z already interpolated) and draws via `render3d.*` — the world render context is active during the call.

## Custom shaders (render3d.shader)

`render3d.shader("name.fsh")` compiles a fragment shader from the script's assets/ and returns a handle: `fx.draw(x, y, z, size[, color, p0, p1, p2, p3])` renders a camera-facing quad in the world. Blending is ADDITIVE without depth write, so output premultiplied color: `fragColor = vec4(color * alpha, alpha)`. The fragment interface is fixed — start from this template:

```glsl
#version 330
in vec2 uv;            // 0..1 inside the quad
in vec4 vertexColor;   // color passed from the script
layout(std140) uniform SadnesScriptFx {
    float uTime;   // seconds since client start
    float uSize;   // quad size in blocks
    float uP0; float uP1; float uP2; float uP3; // p0..p3 from fx.draw
    float uPad0; float uPad1;
};
out vec4 fragColor;
void main() {
    vec2 c = uv * 2.0 - 1.0;
    float d = length(c);
    if (d > 1.0) discard;
    float glow = exp(-d * d * 12.0);
    float alpha = glow * vertexColor.a;
    if (alpha <= 0.004) discard;
    fragColor = vec4(vertexColor.rgb * alpha, alpha);
}
```

The .fsh recompiles on script reload — edit and reload to iterate.

## Custom UI (panel in ClickGui, own screen)

Both take one description object; every callback is optional and receives one argument. Drawing goes through `render.*` and setting handles (`setting.render(x, y, w, ui.alpha)` draws the standard row, `setting.click/release/drag` feed it input); `render.*` colors are multiplied by the host alpha automatically, so fades come for free.

- `render(ui)` — `ui = {x, y, w, h, mouseX, mouseY, alpha, partial}` (screen: x = y = 0, w/h = screen size).
- `click(e)` / `release(e)` / `drag(e)` — `e = {x, y, button, dx, dy}`; `scroll(e)` — `{x, y, vertical, horizontal}`; `key(e)` — `{key, scanCode, modifiers, press}`; `char(e)` — `{char, code, modifiers}`. Return `true` to consume the event.
- `open()` / `close()` — lifecycle hooks. Screen only: `pause: true` pauses the game, `escClose: false` keeps ESC for the script.

```js
var feat = mod.feature("Panel Demo", "Misc");
var speed = feat.slider("Speed", 1.0, 0.1, 5.0, 0.1);
var panel = feat.panel({                       // the gear of the module row opens it instead of settings
    render: function (ui) {
        render.text("Custom panel", ui.x + 10, ui.y + 8, Color.accent(), 7);
        speed.render(ui.x + 10, ui.y + 24, 150, ui.alpha);
    },
    click: function (e) { return speed.click(e.x, e.y, e.button); },
    release: function (e) { speed.release(e.x, e.y, e.button); },
    drag: function (e) { speed.drag(); }
});

var screen = Screen({                          // standalone screen, like the client's ClickGui
    escClose: true, pause: false,
    render: function (ui) {
        render.blur(0, 0, ui.w, ui.h);
        render.textCenter("Hello", ui.w / 2, ui.h / 2, 0xFFFFFFFF, 10);
    },
    key: function (e) { if (e.press && e.key === 32) { screen.close(); return true; } }
});
var openKey = settings.bind("Open screen", 82);   // R
events.key(function (e) {
    if (e.press && openKey.isKey(e.key) && (!e.screenOpen || screen.isOpen())) screen.toggle();
});
```

A click outside the panel or ESC closes it; `panel.open()` also opens ClickGui when it is closed. The screen and the panel close by themselves when the script is disabled, fails or hot-reloads.

### Your own module menu

`client.categories()` returns the ClickGui tabs (`{name, icon, description, modules()}`); every module handle exposes `enabled()/toggle()`, `description()`, `hasSettings()` and `settings()` — handles of the client's real settings, drawn with the standard widgets via `s.render(x, y, w, alpha, true)` (returns the row height; pass `true` to defer dropdowns/color pickers and draw them after the list with `s.overlay()`) and fed with `s.click/release/drag/key/char`. Check `s.overlayOpen()` first and route the click there. `render.toggle(x, y, w, h, t)` draws the client's switch. The `clickgui` preset (`create_script` with `preset: "clickgui"`) is a complete menu: sidebar, search, accordion settings, drag, scroll — start from it.

## Events

Subscribe with `events.<name>(fn)` or `events.on(name, fn)`. The handler gets one argument `e` with the fields listed below (setters change the event); events marked *cancellable* are cancelled by `e.cancel()` or a truthy return. Background hooks (`*_bg`) draw with `render.*` and return `false` to fall back to the client's default drawing. Available events:

%EVENTS%

## API reference

%API%
