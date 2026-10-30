package tech.sadnes.client.ui.clickgui.script;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.animation.StaggeredAppearAnimation;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptAuraHooks;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptModule;
import tech.sadnes.client.script.mcp.McpServer;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.ContextMenu;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.component.HoverTooltip;
import tech.sadnes.client.ui.clickgui.component.ScriptConsoleWindow;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;

public final class ScriptsPage implements IInteractable, IRenderable, IMinecraft {
    private static final float HEADER_HEIGHT = 40.0F;
    private static final float CARD_HEADER_HEIGHT = 30.0F;
    private static final float MODULE_ROW_HEIGHT = 16.0F;
    private static final float CARD_GAP = 4.0F;
    private static final float BUTTON_HEIGHT = 15.0F;
    private static final float FADE_SIZE = 12.0F;
    private static final int DANGER_COLOR = ColorUtil.rgb(255, 96, 96);
    private final ClickGuiScreen clickGui;
    private final StaggeredAppearAnimation appearAnimator = new StaggeredAppearAnimation();
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private final Map<String, Animation> animations = new HashMap<>();
    private final List<ScriptsPage.ClickRegion> clickRegions = new ArrayList<>();
    private final List<ScriptsPage.ModuleSlot> moduleSlots = new ArrayList<>();
    private final Map<String, HoverTooltip> tooltips = new HashMap<>();
    private final ContextMenu popupMenu = new ContextMenu();
    private final Consumer<Script> editorOpener;
    private Script pendingDelete;
    private final StopWatch deleteTimer = new StopWatch();
    private float listX;
    private float listY;
    private float listWidth;
    private float listHeight;

    public ScriptsPage(ClickGuiScreen clickGuiScreen, Consumer<Script> consumer) {
        this.clickGui = clickGuiScreen;
        this.editorOpener = consumer;
    }

    public void reset() {
        this.appearAnimator.reset();
        this.scroll.reset();
        this.animations.clear();
        Iterator iterator2 = this.tooltips.values().iterator();

        while (iterator2.hasNext()) {
            ((HoverTooltip)iterator2.next()).reset();
        }

        this.moduleSlots.clear();
        this.popupMenu.close();
        this.pendingDelete = null;
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.appearAnimator.update();
        this.clickRegions.clear();
        this.moduleSlots.clear();
        float f6 = this.getAlpha();
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null) {
            this.updateTooltips(scriptManager);
            this.renderHeader(drawContext, scriptManager, i, i2, f2, f3 + this.appearAnimator.getOffset(0), f4, f6 * this.appearAnimator.getProgress(0));
            this.renderScriptList(drawContext, scriptManager, i, i2, f2, f3, f4, f5, f6);
            this.popupMenu.render(drawContext, i, i2, f6);
        }
    }

    private void renderScriptList(DrawContext drawContext, ScriptManager scriptManager, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.listX = f;
        this.listY = f2 + 40.0F + 8.0F;
        this.listWidth = f3;
        this.listHeight = f2 + f4 - this.listY;
        if (!(this.listHeight <= 0.0F)) {
            List<Script> scripts = scriptManager.getScripts();
            if (scripts.isEmpty()) {
                this.renderEmpty(drawContext, f5);
            } else {
                float f6 = (this.listWidth - 4.0F) / 2.0F;
                float[] floats = new float[2];

                for (Script script2 : scripts) {
                    int i3 = floats[0] <= floats[1] ? 0 : 1;
                    floats[i3] += this.getCardHeight(script2) + 4.0F;
                }

                float f7 = Math.max(floats[0], floats[1]);
                this.scroll.update(f7, this.listHeight);
                if (ScissorStack.push(drawContext, this.listX, this.listY, this.listWidth, this.listHeight)) {
                    try {
                        this.renderScrollFade(f7);
                        floats[0] = floats[1] = 0.0F;
                        float f8 = this.listY + this.scroll.getScroll();
                        int i4 = 0;

                        for (int i5 = scripts.size(); i4 < i5; i4++) {
                            Script script3 = (Script)scripts.get(i4);
                            int i6 = floats[0] <= floats[1] ? 0 : 1;
                            float f9 = this.getCardHeight(script3);
                            float f10 = this.listX + i6 * (f6 + 4.0F);
                            float f11 = f8 + floats[i6] + this.appearAnimator.getOffset(1 + i4);
                            if (f11 + f9 >= this.listY && f11 <= this.listY + this.listHeight) {
                                this.renderScriptCard(drawContext, scriptManager, script3, i, i2, f10, f11, f6, f5 * this.appearAnimator.getProgress(1 + i4));
                            }

                            floats[i6] += f9 + 4.0F;
                        }
                    } finally {
                        VerticalFadeMask.end();
                        ScissorStack.pop(drawContext);
                    }

                    for (ScriptsPage.ModuleSlot moduleSlot : this.moduleSlots) {
                        this.getTooltip(moduleSlot.script(), moduleSlot.module()).render(drawContext, moduleSlot.x(), moduleSlot.y(), moduleSlot.w(), moduleSlot.h(), HoverTooltip.Side.RIGHT, f5);
                    }
                }
            }
        }
    }

    private List<ContextMenu.MenuItem> getPresetEntries(ScriptManager scriptManager, float f, float f2) {
        return List.of(
            ContextMenu.MenuItem.of(Icon.PLUS, Language.translate("scripts.preset.empty", "Пустой"), () -> this.createScript(scriptManager, null)),
            ContextMenu.MenuItem.of(
                Icon.LOCATION_CROSSHAIRS, Language.translate("scripts.preset.rotation", "Ротация"), () -> this.createScript(scriptManager, "rotation")
            ),
            ContextMenu.MenuItem.of(Icon.PALETTE, Language.translate("scripts.preset.render", "Рендер"), () -> this.createScript(scriptManager, "render")),
            ContextMenu.MenuItem.of(Icon.LAYOUT_DASHBOARD, Language.translate("scripts.preset.ui", "Свой UI"), () -> this.createScript(scriptManager, "ui")),
            ContextMenu.MenuItem.of(Icon.EYE, Language.translate("scripts.preset.esp", "ESP"), () -> this.createScript(scriptManager, "esp")),
            ContextMenu.MenuItem.of(Icon.OBJECT_GROUP, Language.translate("scripts.preset.clickgui", "ClickGui"), () -> this.createScript(scriptManager, "clickgui")),
            ContextMenu.MenuItem.of(Icon.CUBE, Language.translate("scripts.preset.scaffold", "Стройка"), () -> this.createScript(scriptManager, "scaffold")),
            ContextMenu.MenuItem.of(Category.COMBAT.getIcon(), Category.COMBAT.getName(), () -> this.popupMenu.show(this.getCombatPresets(scriptManager), f, f2)),
            ContextMenu.MenuItem.of(
                Category.MOVEMENT.getIcon(), Category.MOVEMENT.getName(), () -> this.popupMenu.show(this.getMovementPresets(scriptManager), f, f2)
            )
        );
    }

    private List<ContextMenu.MenuItem> getCombatPresets(ScriptManager scriptManager) {
        return List.of(
            this.presetEntry(scriptManager, Icon.HEART, "AutoSoup", "autosoup"),
            this.presetEntry(scriptManager, Icon.DRUMSTICK, "AutoUse", "autouse"),
            this.presetEntry(scriptManager, Icon.RUN, "WTap", "wtap"),
            this.presetEntry(scriptManager, Icon.USER, "SneakTap", "sneaktap"),
            this.presetEntry(scriptManager, Icon.SWORDS, "Uppercuts", "uppercuts")
        );
    }

    private List<ContextMenu.MenuItem> getMovementPresets(ScriptManager scriptManager) {
        return List.of(
            this.presetEntry(scriptManager, Icon.METEOR, "ElytraBooster", "elytrabooster"),
            this.presetEntry(scriptManager, Icon.CLOUD, "ElytraFly", "elytrafly"),
            this.presetEntry(scriptManager, Icon.GEYSER, "HighJump", "highjump"),
            this.presetEntry(scriptManager, Icon.PARACHUTE_BOX, "NoFall", "nofall"),
            this.presetEntry(scriptManager, Icon.DROPLET, "Jesus", "jesus"),
            this.presetEntry(scriptManager, Icon.SORT_ASC, "FastLadder", "fastladder"),
            this.presetEntry(scriptManager, Icon.KEYBOARD, "Nulls", "nulls"),
            this.presetEntry(scriptManager, Icon.GHOST, "AutoDodge", "autododge")
        );
    }

    private ContextMenu.MenuItem presetEntry(ScriptManager scriptManager, Icon icon, String string, String string2) {
        return ContextMenu.MenuItem.of(icon, string, () -> this.createScript(scriptManager, string2));
    }

    private void compileScript(ScriptManager scriptManager, Script script) {
        Path path = scriptManager.compileScript(script);
        if (path == null) {
            NotificationUtil.error(NotificationSources.scripts(), Language.translate("scripts.page.compile_failed", "Не удалось скомпилировать скрипт"));
        } else {
            NotificationUtil.success(
                NotificationSources.scripts(), String.format(Language.translate("scripts.page.compiled", "Собран: %s"), path.getFileName().toString())
            );
            Util.getOperatingSystem().open(path.getParent().toFile());
        }
    }

    private void decryptScript(ScriptManager scriptManager, Script script) {
        Script script2 = scriptManager.decryptScript(script);
        if (script2 == null) {
            NotificationUtil.error(NotificationSources.scripts(), Language.translate("scripts.page.decrypt_failed", "Не удалось декриптнуть скрипт"));
        } else {
            NotificationUtil.success(
                NotificationSources.scripts(), String.format(Language.translate("scripts.page.decrypted", "Декриптнут: %s"), script2.getDisplayName())
            );
            this.editorOpener.accept(script2);
        }
    }

    private void createScript(ScriptManager scriptManager, String string) {
        Script script = scriptManager.createScriptFromPreset(string == null ? "script" : string, string);
        if (script != null) {
            NotificationUtil.success(
                NotificationSources.scripts(), String.format(Language.translate("scripts.page.created", "%s создан"), script.getDisplayName())
            );
            this.editorOpener.accept(script);
        }
    }

    private void updateTooltips(ScriptManager scriptManager) {
        for (Script script : scriptManager.getScripts()) {
            for (ScriptModule scriptModule : script.getModules()) {
                this.getTooltip(script, scriptModule).update(scriptModule.getDisplayName(), scriptModule.getDescription(), List.of());
            }
        }
    }

    private HoverTooltip getTooltip(Script script, ScriptModule scriptModule) {
        return this.tooltips.computeIfAbsent(script.getDisplayName() + ":" + scriptModule.getDisplayName(), string -> new HoverTooltip());
    }

    private String getSubtitle(Script script) {
        String string = script.getMetadata().author().isEmpty() ? "" : String.format(Language.translate("scripts.page.by", "от %s"), script.getMetadata().author());
        if (script.hasError()) {
            return Language.translate("scripts.page.error", "Ошибка") + ": " + script.getErrorMessage();
        } else {
            return script.getMetadata().description().isEmpty() ? string : script.getMetadata().description();
        }
    }

    private void renderHeader(DrawContext drawContext, ScriptManager scriptManager, int i, int i2, float f, float f2, float f3, float f4) {
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, 40.0F, f4 * 0.7F, 8.0F);
        new RenderUtil(drawContext, f + 10.0F, f2 + 20.0F - 18.0F / 2.0F, 18.0F, 18.0F)
            .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.16F), f4), Radius.smoothAll(6.0F))
            .icon(Icon.CODE, 18.0F / 2.0F - 5.0F, 18.0F / 2.0F - 5.0F, 10.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f4));
        int i3 = 0;

        for (Script script : scriptManager.getScripts()) {
            if (script.isActive()) {
                i3++;
            }
        }

        String string = String.format(Language.translate("scripts.page.count", "Скриптов: %s, активно: %s"), scriptManager.getScripts().size(), i3);
        new RenderUtil(drawContext, f, f2, f3, 40.0F)
            .textTopLeft(
                Fonts.INTER_SEMIBOLD,
                Language.translate("scripts.page.title", "Скрипты"),
                34.0F,
                12.0F,
                ColorUtil.multiplyAlpha(Theme.getTextColor(), f4),
                7.5F
            )
            .textTopLeft(Fonts.INTER_REGULAR, string, 34.0F, 21.5F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.7F), f4), 6.0F);
        float f5 = f + f3 - 8.0F;
        float f6 = f2 + 20.0F + 7.5F + 3.0F;
        float f7 = f5
            - (
                this.renderButton(
                        drawContext,
                        "create",
                        Language.translate("scripts.page.create", "Создать"),
                        Icon.PLUS,
                        f5,
                        f2,
                        i,
                        i2,
                        f4,
                        true,
                        () -> this.popupMenu.show(this.getPresetEntries(scriptManager, f5, f6), f5, f6)
                    )
                    + 6.0F
            );
        McpServer mcpServer = scriptManager.getDevServer();
        if (mcpServer != null) {
            f7 -= this.renderButton(
                    drawContext,
                    "mcp",
                    Language.translate("scripts.page.mcp", "MCP"),
                    Icon.TOWER_BROADCAST,
                    f7,
                    f2,
                    i,
                    i2,
                    f4,
                    mcpServer.isRunning(),
                    () -> {
                        if (mcpServer.isRunning()) {
                            mcpServer.toggle();
                            NotificationUtil.info(NotificationSources.scripts(), Language.translate("scripts.mcp.off", "MCP сервер выключен"));
                        } else if (mcpServer.toggle()) {
                            NotificationUtil.success(
                                NotificationSources.scripts(), String.format(Language.translate("scripts.mcp.on", "MCP для нейросетей: %s"), mcpServer.getUrl())
                            );
                        } else {
                            NotificationUtil.error(NotificationSources.scripts(), Language.translate("scripts.mcp.failed", "Не удалось запустить MCP сервер"));
                        }
                    }
                )
                + 8.0F;
        }

        float f8 = f2 + 20.0F - 5.0F;
        float f9 = f7 - 12.0F;
        ScriptConsoleWindow scriptConsoleWindow = this.clickGui.getScriptWindowManager().getMainWindow();
        this.renderColoredIconButton(
            drawContext, "console", Icon.RECTANGLE_LIST, f9, f8, i, i2, f4, scriptConsoleWindow.isOpen() ? Theme.getPrimary() : Theme.getSecondaryText(), scriptConsoleWindow::toggle
        );
        if (scriptConsoleWindow.hasUnreadErrors()) {
            new RenderUtil(drawContext, f9 + 8.0F, f8 - 1.5F, 4.0F, 4.0F).rect(ColorUtil.multiplyAlpha(DANGER_COLOR, f4), Radius.all(2.0F));
        }

        float f10 = f9 - 17.0F;
        this.renderIconButton(drawContext, "reload", Icon.REFRESH, f10, f8, i, i2, f4, () -> {
            boolean flag = !scriptManager.getScripts().isEmpty();

            for (Script script2 : new ArrayList<>(scriptManager.getScripts())) {
                scriptManager.reloadScript(script2);
            }

            NotificationUtil.info(NotificationSources.scripts(), Language.translate("scripts.page.reloaded", "Перезагружены"));
            if (flag) {
                this.clickGui.reload();
            }
        });
        float f11 = f10 - 17.0F;
        this.renderIconButton(drawContext, "folder", Icon.FOLDER, f11, f8, i, i2, f4, () -> Util.getOperatingSystem().open(scriptManager.getScriptsDir().toFile()));
    }

    private float getCardHeight(Script script) {
        float f = 30.0F + script.getModules().size() * 16.0F;
        return script.getModules().isEmpty() ? f + 0.0F : f + 4.0F;
    }

    private void renderScriptCard(DrawContext drawContext, ScriptManager scriptManager, Script script, int i, int i2, float f, float f2, float f3, float f4) {
        float f5 = this.getCardHeight(script);
        if (script == this.pendingDelete && this.deleteTimer.hasPassed(4000.0)) {
            this.pendingDelete = null;
        }

        boolean flag = script == this.pendingDelete;
        Animation animation = this.getAnimation("del:" + script.getDisplayName());
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, flag ? 0.2 : 0.25, Easings.QUAD_OUT);
        float f6 = animation.get();
        int i3 = i;
        int i4 = i2;
        if (f6 > 0.5F) {
            i = Integer.MIN_VALUE;
            i2 = Integer.MIN_VALUE;
        }

        Animation animation2 = this.getAnimation("h:" + script.getDisplayName());
        animation2.update();
        animation2.animate(this.isHovered(i, i2, f, f2, f3, f5) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f7 = animation2.get();
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, f5, f4 * (0.6F + 0.2F * f7), 6.0F);
        boolean flag2;
        if (script.isEnabled()) {
            flag2 = !script.hasError();
        } else {
            flag2 = false;
        }

        this.renderSwitch(drawContext, "s:" + script.getDisplayName(), f + 8.0F, f2 + 15.0F - 4.5F, flag2, f4, () -> {
            boolean flag3 = ScriptAuraHooks.hasRegistrations(script);
            script.toggle();
            if (flag3) {
                this.clickGui.reload();
            }
        });
        float f8 = f + 8.0F + 14.0F + 8.0F;
        String string = script.getTitle();
        String string2 = script.getMetadata().version();
        String string3 = this.getSubtitle(script);
        int i5 = script.hasError()
            ? ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 96, 96), f4)
            : ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), flag2 ? 1.0F : 0.6F), f4);
        float f9 = (script.isPackaged() ? 2 : 1) * 16.0F + 8.0F;
        float f10 = f3 - (f8 - f) - f9;
        float f11 = string2.isEmpty() ? 0.0F : Fonts.INTER_REGULAR.getWidth("v" + string2, 5.5F) + 4.0F;
        float f12 = Math.max(0.0F, f10 - f11);
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, 30.0F)
            .scrollingTextTop(Fonts.INTER_MEDIUM, string, f8 - f, 5.0F, f12, i5, 7.0F)
            .scrollingTextTop(
                Fonts.INTER_REGULAR,
                string3,
                f8 - f,
                15.0F,
                f10,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), script.hasError() ? 0.9F : 0.65F), f4),
                5.5F
            );
        if (!string2.isEmpty()) {
            float f13 = Math.min(Fonts.INTER_MEDIUM.getWidth(string, 7.0F), f12);
            renderUtil.textTopLeft(
                Fonts.INTER_REGULAR,
                "v" + string2,
                f8 - f + f13 + 4.0F,
                6.5F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.8F), f4),
                5.5F
            );
        }

        float f14 = f + f3 - 20.0F;
        float f15 = f2 + 15.0F - 5.0F;
        this.renderIconButton(
            drawContext,
            "menu:" + script.getDisplayName(),
            Icon.ELLIPSIS,
            f14,
            f15,
            i,
            i2,
            f4,
            () -> this.popupMenu.show(this.getScriptMenuEntries(scriptManager, script, f14, f2), f14 + 12.0F, f15 + 15.0F)
        );
        if (script.isPackaged()) {
            new RenderUtil(drawContext, f14 - 16.0F, f15, 10.0F, 10.0F)
                .icon(Icon.SHIELD, 0.0F, 0.0F, 10.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.7F), f4));
        }

        if (!script.getModules().isEmpty()) {
            GuiRenderUtil.drawOverlay(drawContext, f + 8.0F, MathUtil.roundToStep(f2 + 30.0F - 4.25F, 0.5F), f3 - 16.0F, 0.5F, f4);
            float f16 = f2 + 30.0F;

            for (ScriptModule scriptModule : script.getModules()) {
                this.renderModuleRow(drawContext, script, scriptModule, i, i2, f, f16, f3, f4);
                f16 += 16.0F;
            }
        }

        this.renderDeleteConfirm(drawContext, scriptManager, script, i3, i4, f, f2, f3, f5, f4, f6);
    }

    private List<ContextMenu.MenuItem> getScriptMenuEntries(ScriptManager scriptManager, Script script, float f, float f2) {
        ArrayList arrayList = new ArrayList(7);
        if (!script.isPackaged()) {
            arrayList.add(ContextMenu.MenuItem.of(Icon.CODE, Language.translate("scripts.page.menu.edit", "Редактор"), () -> this.editorOpener.accept(script)));
            arrayList.add(
                ContextMenu.MenuItem.of(Icon.PACKAGE, Language.translate("scripts.page.menu.compile", "Собрать .ss"), () -> this.compileScript(scriptManager, script))
            );
        }

        if (script.isPackaged() && scriptManager.canDecrypt()) {
            arrayList.add(ContextMenu.MenuItem.of(Icon.BOX_OPEN, Language.translate("scripts.page.menu.decrypt", "Декрипт"), () -> this.decryptScript(scriptManager, script)));
        }

        arrayList.add(
            ContextMenu.MenuItem.of(
                Icon.RECTANGLE_LIST, Language.translate("scripts.page.menu.console", "Консоль"), () -> this.clickGui.getScriptWindowManager().openWindow(script)
            )
        );
        if (script.hasVisibleSettings()) {
            arrayList.add(
                ContextMenu.MenuItem.of(
                    Icon.SETTINGS,
                    Language.translate("scripts.page.menu.settings", "Настройки"),
                    () -> this.clickGui.toggleSettingsPopup(script, f, f2 + 30.0F)
                )
            );
        }

        arrayList.add(ContextMenu.MenuItem.of(Icon.REFRESH, Language.translate("scripts.page.menu.reload", "Перезагрузить"), () -> scriptManager.reloadScript(script)));
        Path path = script.isPackaged() ? script.getDirectory().getParent() : script.getDirectory();
        arrayList.add(
            ContextMenu.MenuItem.of(
                Icon.FOLDER, Language.translate("scripts.page.menu.folder", "Открыть папку"), () -> Util.getOperatingSystem().open(path.toFile())
            )
        );
        arrayList.add(ContextMenu.MenuItem.ofDanger(Icon.TRASH, Language.translate("scripts.page.menu.delete", "Удалить"), () -> {
            this.pendingDelete = script;
            this.deleteTimer.reset();
        }));
        return arrayList;
    }

    private void renderDeleteConfirm(
        DrawContext drawContext, ScriptManager scriptManager, Script script, int i, int i2, float f, float f2, float f3, float f4, float f5, float f6
    ) {
        if (!(f6 <= 0.001F)) {
            float f7 = f5 * f6;
            new RenderUtil(drawContext, f, f2, f3, f4)
                .blur(f7, Radius.all(6.0F), RenderUtil.BlurQuality.LOW, true)
                .rect(ColorUtil.multiplyAlpha(Theme.getBackground(), 0.3F * f7), Radius.all(6.0F));
            String string = Language.translate("scripts.page.menu.delete", "Удалить");
            float f8 = Fonts.INTER_MEDIUM.getWidth(string, 6.5F) + 24.0F;
            float f9 = f + (f3 - f8) / 2.0F;
            float f10 = f2 + (f4 - 15.0F) / 2.0F;
            Animation animation = this.getAnimation("delbtn:" + script.getDisplayName());
            animation.update();
            animation.animate(f6 > 0.5F && this.isHovered(i, i2, f9, f10, f8, 15.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
            float f11 = animation.get();
            int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f7);
            new RenderUtil(drawContext, f9, f10, f8, 15.0F)
                .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(DANGER_COLOR, 0.75F + 0.25F * f11), f7), Radius.all(5.0F))
                .icon(Icon.TRASH, 7.0F, 4.0F, 7.0F, i3)
                .textMiddleLeft(Fonts.INTER_MEDIUM, string, 17.0F, 0.0F, i3, 6.5F);
            if (!(f6 <= 0.5F)) {
                this.clickRegions.add(0, new ScriptsPage.ClickRegion(f, f2, f3, f4, () -> this.pendingDelete = null));
                this.clickRegions
                    .add(
                        0,
                        new ScriptsPage.ClickRegion(
                            f9,
                            f10,
                            f8,
                            15.0F,
                            () -> {
                                this.pendingDelete = null;
                                scriptManager.deleteScript(script);
                                NotificationUtil.warning(
                                    NotificationSources.scripts(),
                                    String.format(Language.translate("scripts.page.deleted", "%s удалён"), script.getDisplayName())
                                );
                            }
                        )
                    );
            }
        }
    }

    private void renderModuleRow(DrawContext drawContext, Script script, ScriptModule scriptModule, int i, int i2, float f, float f2, float f3, float f4) {
        float f5 = f + 8.0F;
        float f6 = f3 - 8.0F - 8.0F;
        boolean flag = scriptModule.isEnabled();
        boolean flag2 = this.isHovered(i, i2, f5, f2, f6, 16.0);
        this.moduleSlots.add(new ScriptsPage.ModuleSlot(f5, f2, f6, 16.0F, script, scriptModule));
        if (!scriptModule.getDescription().isEmpty() && flag2 && !this.clickGui.getSettingsPopup().isOpen() && !this.clickGui.isBindPopupActive()) {
            this.getTooltip(script, scriptModule).markHovered();
        }

        this.renderCheckbox(
            drawContext,
            "m:" + script.getDisplayName() + ":" + scriptModule.getDisplayName(),
            f5,
            f2 + 8.0F - 4.0F,
            Boolean.TRUE.equals(scriptModule.getEnabledSetting().getValue()),
            f4,
            scriptModule::toggle
        );
        float f7 = f5 + 8.0F + 7.0F;
        float f8 = Math.max(0.0F, f + f3 - 20.0F - 30.0F - f7);
        new RenderUtil(drawContext, f7, f2, f6, 16.0F)
            .scrollingTextMiddle(
                Fonts.INTER_MEDIUM,
                scriptModule.getDisplayName(),
                0.0F,
                0.0F,
                f8,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), flag ? 0.95F : 0.55F), f4),
                6.0F
            );
        float f9 = f2 + 8.0F - 4.0F;
        float f10 = f + f3 - 20.0F;
        boolean flag3 = scriptModule.getSettings().size() > 1;
        if (flag3) {
            this.renderSizedIconButton(
                drawContext,
                "mg:" + script.getDisplayName() + ":" + scriptModule.getDisplayName(),
                Icon.SETTINGS,
                f10,
                f9,
                8.0F,
                i,
                i2,
                f4,
                Theme.getSecondaryText(),
                () -> this.clickGui.toggleSettingsPopup(scriptModule, f10, f2 + 16.0F)
            );
        }

        Animation animation = this.getAnimation("mh:" + script.getDisplayName() + ":" + scriptModule.getDisplayName());
        animation.update();
        animation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f11 = animation.get();
        if (f11 > 0.001F) {
            float f12 = flag3 ? f10 - 13.0F : f10;
            this.renderSizedIconButton(
                drawContext,
                "mb:" + script.getDisplayName() + ":" + scriptModule.getDisplayName(),
                Icon.KEYBOARD,
                f12,
                f9,
                8.0F,
                i,
                i2,
                f4 * f11,
                Theme.getSecondaryText(),
                () -> this.clickGui.openSettingBind(scriptModule.getEnabledSetting(), f12 + 4.0F, f2 + 16.0F)
            );
        }
    }

    private void renderEmpty(DrawContext drawContext, float f) {
        new RenderUtil(drawContext, this.listX, this.listY, this.listWidth, this.listHeight)
            .icon(
                Icon.CODE,
                this.listWidth / 2.0F - 9.0F,
                this.listHeight / 2.0F - 24.0F,
                18.0F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.25F), f)
            )
            .textCenter(
                Fonts.INTER_MEDIUM,
                Language.translate("scripts.page.empty", "Скриптов пока нет"),
                0.0F,
                0.0F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.7F), f),
                7.0F
            )
            .textCenter(
                Fonts.INTER_REGULAR,
                Language.translate("scripts.page.empty.hint", "Нажми «Создать» или положи .js в папку скриптов"),
                0.0F,
                11.0F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F), f),
                6.0F
            );
    }

    private float renderButton(
        DrawContext drawContext, String string, String string2, Icon icon2, float f, float f2, int i, int i2, float f3, boolean flag, Runnable runnable
    ) {
        float f4 = Fonts.INTER_MEDIUM.getWidth(string2, 6.5F) + 24.0F;
        float f5 = f - f4;
        float f6 = f2 + 20.0F - 7.5F;
        Animation animation = this.getAnimation("b:" + string);
        animation.update();
        animation.animate(this.isHovered(i, i2, f5, f6, f4, 15.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f7 = animation.get();
        if (flag) {
            new RenderUtil(drawContext, f5, f6, f4, 15.0F)
                .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.75F + 0.25F * f7), f3), Radius.all(5.0F));
        } else {
            GuiRenderUtil.drawOutlined(drawContext, f5, f6, f4, 15.0F, f3 * (0.7F + 0.25F * f7), 5.0F);
        }

        int i3 = flag
            ? ColorUtil.multiplyAlpha(Theme.getTextColor(), f3)
            : ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.65F + 0.35F * f7), f3);
        new RenderUtil(drawContext, f5, f6, f4, 15.0F).icon(icon2, 7.0F, 4.0F, 7.0F, i3).textMiddleLeft(Fonts.INTER_MEDIUM, string2, 17.0F, 0.0F, i3, 6.5F);
        this.clickRegions.add(new ScriptsPage.ClickRegion(f5, f6, f4, 15.0F, runnable));
        return f4;
    }

    private void renderIconButton(DrawContext drawContext, String string, Icon icon, float f, float f2, int i, int i2, float f3, Runnable runnable) {
        this.renderColoredIconButton(drawContext, string, icon, f, f2, i, i2, f3, Theme.getSecondaryText(), runnable);
    }

    private void renderColoredIconButton(
        DrawContext drawContext, String string, Icon icon, float f, float f2, int i, int i2, float f3, int i3, Runnable runnable
    ) {
        this.renderSizedIconButton(drawContext, string, icon, f, f2, 10.0F, i, i2, f3, i3, runnable);
    }

    private void renderSizedIconButton(
        DrawContext drawContext, String string, Icon icon2, float f, float f2, float f3, int i, int i2, float f4, int i3, Runnable runnable
    ) {
        float f5 = f3 + 4.0F;
        Animation animation = this.getAnimation("i:" + string);
        animation.update();
        animation.animate(this.isHovered(i, i2, f - 2.0F, f2 - 2.0F, f5, f5) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f6 = animation.get();
        if (f6 > 0.001F) {
            GuiRenderUtil.drawHighlight(drawContext, f - 2.0F, f2 - 2.0F, f5, f5, f4 * f6, Radius.all(f5 / 3.0F));
        }

        new RenderUtil(drawContext, f, f2, f3, f3).icon(icon2, 0.0F, 0.0F, f3, ColorUtil.multiplyAlpha(Theme.withAlpha(i3, 0.55F + 0.4F * f6), f4));
        this.clickRegions.add(new ScriptsPage.ClickRegion(f - 2.0F, f2 - 2.0F, f5, f5, runnable));
    }

    private void renderSwitch(DrawContext drawContext, String string, float f, float f2, boolean flag, float f3, Runnable runnable) {
        Animation animation = this.getAnimation("sw:" + string);
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        SettingWidgets.drawDefaultSwitch(drawContext, f, f2, animation.get(), f3);
        this.clickRegions.add(new ScriptsPage.ClickRegion(f - 2.0F, f2 - 3.0F, 18.0F, 15.0F, runnable));
    }

    private void renderCheckbox(DrawContext drawContext, String string, float f, float f2, boolean flag, float f3, Runnable runnable) {
        Animation animation = this.getAnimation("sw:" + string);
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        SettingWidgets.drawCheckbox(drawContext, f, f2, 8.0F, animation.get(), f3);
        this.clickRegions.add(new ScriptsPage.ClickRegion(f - 2.0F, f2 - 2.0F, 12.0F, 12.0F, runnable));
    }

    private void renderScrollFade(float f) {
        float f2 = f - this.listHeight;
        if (!(f2 <= 0.5F) && !(this.listHeight <= 24.0F)) {
            float f3 = this.scroll.getScroll();
            float f4 = Math.clamp(-f3 / 12.0F, 0.0F, 1.0F);
            float f5 = Math.clamp((f2 + f3) / 12.0F, 0.0F, 1.0F);
            if (!(f4 <= 0.001F) || !(f5 <= 0.001F)) {
                VerticalFadeMask.begin(this.listY, this.listY + 12.0F, f4, this.listY + this.listHeight - 12.0F, this.listY + this.listHeight, f5);
            }
        }
    }

    private Animation getAnimation(String string) {
        return this.animations.computeIfAbsent(string, string2 -> new Animation());
    }

    private float getAlpha() {
        return (float)Math.pow(this.clickGui.getOpenAnimation().get(), 3.0);
    }

    public boolean mouseClicked(Click click, boolean flag) {
        if (this.popupMenu.mouseClicked(click.x(), click.y())) {
            return true;
        }

        if (this.isMiddleButton(click.button())) {
            for (ScriptsPage.ModuleSlot moduleSlot : this.moduleSlots) {
                if (this.isHovered(click.x(), click.y(), moduleSlot.x(), moduleSlot.y(), moduleSlot.w(), moduleSlot.h())) {
                    this.clickGui.openSettingBind(moduleSlot.module().getEnabledSetting(), click.x(), click.y());
                    return true;
                }
            }
        } else {
            if (!this.isLeftButton(click.button())) {
                return false;
            }

            for (ScriptsPage.ClickRegion clickRegion : this.clickRegions) {
                if (this.isHovered(click.x(), click.y(), clickRegion.x(), clickRegion.y(), clickRegion.w(), clickRegion.h())) {
                    clickRegion.action().run();
                    return true;
                }
            }
        }

        return false;
    }

    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        this.popupMenu.close();
        if (this.isHovered(d, d2, this.listX, this.listY, this.listWidth, this.listHeight)) {
            this.scroll.onScroll(d4);
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseReleased(Click click) {
        this.scroll.mouseReleased();
        return false;
    }

    record ClickRegion(float x, float y, float w, float h, Runnable action) {
    }

    record ModuleSlot(float x, float y, float w, float h, Script script, ScriptModule module) {
    }
}
