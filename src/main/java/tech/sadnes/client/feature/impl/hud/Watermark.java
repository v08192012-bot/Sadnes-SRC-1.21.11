package tech.sadnes.client.feature.impl.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Identifier;
import tech.sadnes.api.mixin.accessor.BossBarHudAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.ConfigEntry;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.HudSettingsPopupComponent;
import tech.sadnes.client.feature.component.KillDeathTrackerComponent;
import tech.sadnes.client.feature.component.NetworkStatsComponent;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.texture.AvatarTextureLoader;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.GradientColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.ui.hud.ScriptWatermarkChip;
import tech.sadnes.client.ui.hud.WatermarkChip;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.system.SystemStats;
import tech.sadnes.libs.rhino.Context;
import ua.mintantileak.profile.Profile;

@FeatureInfo(
    name = "Watermark",
    description = "feature.impl.hud.watermark.watermark",
    displayName = "feature.impl.hud.watermark.watermark",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_INFORMATION
)
public class Watermark extends Feature {
    private static final float HEIGHT = 15.0F;
    private static final float CHIP_GAP = 4.0F;
    private static final float PADDING = 4.0F;
    private static final float MARGIN = 5.0F;
    private static final float TRASH_SIZE = 15.0F;
    private static final float TRASH_GAP = 6.0F;
    private static final float TRASH_ICON_SIZE = 8.0F;
    private static final int TRASH_HOVER_COLOR = ColorUtil.rgb(255, 90, 90);
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final float LOGO_ICON_SIZE = 10.0F;
    private static final float AVATAR_ICON_SIZE = 12.0F;
    private static final double LOGO_ANIMATION_DURATION = 1.4;
    private static final int HORIZONTAL_PADDING = 8;
    private static final float BOSS_BAR_HEIGHT = 19.0F;
    private static final float BOSS_BAR_MARGIN = 4.0F;
    public final MultiSetting elements = new MultiSetting("watermark")
        .addOption("logo", true)
        .addOption("avatar", true)
        .addOption("user", true)
        .addOption("fps", true)
        .addOption("time", true)
        .addOption("coords")
        .addOption("speed")
        .addOption("ping")
        .addOption("jitter")
        .addOption("ip")
        .addOption("tps")
        .addOption("cpu")
        .addOption("ram")
        .addOption("gpu")
        .addOption("kd")
        .addOption("cpufreq")
        .addOption("cpucores")
        .addOption("var")
        .addOption("loss")
        .addOption("connectivity")
        .addOption("preset")
        .addOption("pvpcd");
    public final ModeSetting position = new ModeSetting("Position", "Left", new String[]{"Left", "Center", "Right", "BottomLeft", "BottomCenter", "BottomRight"});
    public final GradientColorSetting colors = new GradientColorSetting("watermark.colors", -1, -42406);
    private final TextSetting chipOrder = (TextSetting)new TextSetting("ChipOrder").setHidden(true);
    private final DragSetting draggable;
    private final Map<String, WatermarkChip> chips = new LinkedHashMap<>();
    private final List<String> chipOrderList = new ArrayList<>();
    private final Animation fadeAnimation = new Animation();
    private final Animation bossBarOffsetAnimation = new Animation();
    private WatermarkChip draggedChip;
    private float dragOffsetX;
    private float dragX;
    private final Animation dragLiftAnimation = new Animation();
    private final Animation trashAnimation = new Animation();
    private final Animation trashHoverAnimation = new Animation();
    private float trashX;
    private float trashY;
    volatile double cpuLoad;
    volatile double cpuFrequency;
    volatile long usedMemory;
    volatile long totalMemory;
    volatile double gpuLoad = -1.0;
    private volatile Thread pollThread;
    final Animation fpsAnimation = new Animation();
    final Animation logoAnimation = new Animation();
    private List<String> lastSelected = List.of();
    private float panelX;
    private float panelY;
    private float panelWidth;
    private boolean hasVisibleChips;

    public Watermark() {
        this.registerChip(new Watermark.LogoChip(this.elements.getOption("logo")));
        this.registerChip(new Watermark.AvatarChip(this.elements.getOption("avatar")));
        this.registerChip(new Watermark.UserChip(this.elements.getOption("user")));
        this.registerChip(new Watermark.FpsChip(this.elements.getOption("fps")));
        this.registerChip(new Watermark.TimeChip(this.elements.getOption("time")));
        this.registerChip(new Watermark.CoordsChip(this.elements.getOption("coords")));
        this.registerChip(new Watermark.SpeedChip(this.elements.getOption("speed")));
        this.registerChip(new Watermark.PingChip(this.elements.getOption("ping")));
        this.registerChip(new Watermark.JitterChip(this.elements.getOption("jitter")));
        this.registerChip(new Watermark.IpChip(this.elements.getOption("ip")));
        this.registerChip(new Watermark.TpsChip(this.elements.getOption("tps")));
        this.registerChip(new Watermark.CpuChip(this.elements.getOption("cpu")));
        this.registerChip(new Watermark.RamChip(this.elements.getOption("ram")));
        this.registerChip(new Watermark.GpuChip(this.elements.getOption("gpu")));
        this.registerChip(new Watermark.KdChip(this.elements.getOption("kd")));
        this.registerChip(new Watermark.CpuFreqChip(this.elements.getOption("cpufreq")));
        this.registerChip(new Watermark.CpuCoresChip(this.elements.getOption("cpucores")));
        this.registerChip(new Watermark.VarianceChip(this.elements.getOption("var")));
        this.registerChip(new Watermark.LossChip(this.elements.getOption("loss")));
        this.registerChip(new Watermark.ConnectivityChip(this.elements.getOption("connectivity")));
        this.registerChip(new Watermark.PresetChip(this.elements.getOption("preset")));
        this.registerChip(new Watermark.PvpCooldownChip(this.elements.getOption("pvpcd")));
        this.lastSelected = List.copyOf(this.elements.getSelected());
        this.elements.setOnChange(this::onElementsChanged);
        this.chipOrder.setValue(String.join(",", this.chipOrderList));
        this.chipOrder.setOnChange(() -> this.applyChipOrder(this.chipOrder.getValue()));
        this.draggable = new DragSetting(this.getInfo().name())
            .setDraggable(() -> false)
            .onInput(this)
            .setRenderCondition(() -> this.isEnabled() && this.hasVisibleChips ? true : false);
        DragComponent.register(this.draggable);
    }

    private void registerChip(WatermarkChip watermarkChip) {
        this.chips.put(watermarkChip.getId(), watermarkChip);
        this.chipOrderList.add(watermarkChip.getId());
        watermarkChip.setColors(this.colors::getFirstColor, this.colors::getSecondColor);
        watermarkChip.getSetting().setIconRenderer(watermarkChip::renderIcon);
    }

    private void applyChipOrder(String string) {
        if (string != null && !string.isEmpty()) {
            ArrayList arrayList = new ArrayList();

            for (String string2 : string.split(",")) {
                String string3 = string2.trim();
                if (this.chips.containsKey(string3) && !arrayList.contains(string3)) {
                    arrayList.add(string3);
                }
            }

            for (String string4 : this.chips.keySet()) {
                if (!arrayList.contains(string4)) {
                    arrayList.add(string4);
                }
            }

            this.chipOrderList.clear();
            this.chipOrderList.addAll(arrayList);
        }
    }

    private void saveChipOrder() {
        String string = String.join(",", this.chipOrderList);
        if (!string.equals(this.chipOrder.getValue())) {
            this.chipOrder.setValue(string);
        }
    }

    public ScriptWatermarkChip addScriptChip(Script script, String string, Icon icon, boolean flag) {
        WatermarkChip watermarkChip = this.chips.get(string);
        if (watermarkChip != null && !(watermarkChip instanceof ScriptWatermarkChip)) {
            throw Context.reportRuntimeError("watermark.chip: '" + string + "' is a built-in chip");
        }

        if (watermarkChip != null) {
            this.removeScriptChip(string);
        }

        this.elements.addOption(string, flag);
        ScriptWatermarkChip scriptWatermarkChip = new ScriptWatermarkChip(string, this.elements.getOption(string), script, icon);
        this.registerChip(scriptWatermarkChip);
        this.saveChipOrder();
        return scriptWatermarkChip;
    }

    public void removeScriptChips(Script script) {
        ArrayList<String> strings = new ArrayList<>();

        for (WatermarkChip watermarkChip : this.chips.values()) {
            if (watermarkChip instanceof ScriptWatermarkChip && ((ScriptWatermarkChip)watermarkChip).getScript() == script) {
                strings.add(watermarkChip.getId());
            }
        }

        for (String string : strings) {
            this.removeScriptChip(string);
        }
    }

    public void removeScriptChip(String string) {
        if (this.chips.get(string) instanceof ScriptWatermarkChip) {
            this.chips.remove(string);
            this.chipOrderList.remove(string);
            this.elements.removeOption(string);
            this.saveChipOrder();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.startSystemPolling();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.stopSystemPolling();
        this.hasVisibleChips = false;
        this.fadeAnimation.forceValue(0.0);
    }

    @EventHandler
    public void onRender(Render2DEvent render2DEvent) {
        this.ensureSelection();
        this.fpsAnimation.update();
        this.fpsAnimation.animate(mc.getCurrentFps(), 0.15, Easings.QUART_OUT, false);
        this.fadeAnimation.update();
        this.fadeAnimation.animate(1.0, 0.3, Easings.QUART_OUT);
        this.logoAnimation.update();

        for (WatermarkChip watermarkChip : this.chips.values()) {
            watermarkChip.visibilityAnimation.update();
            watermarkChip.visibilityAnimation.animate(watermarkChip.isVisible() ? 1.0 : 0.0, 0.15, Easings.QUART_OUT);
            watermarkChip.warningAnimation.update();
            watermarkChip.warningAnimation.animate(watermarkChip.getWarningLevel(), 0.5, Easings.QUART_OUT);
        }

        if (this.draggedChip != null && !this.isEditing()) {
            this.draggedChip = null;
            this.saveChipOrder();
        }

        if (this.draggedChip != null && this.draggedChip.isVisible()) {
            this.dragX = (float)render2DEvent.getMouseX() - this.dragOffsetX;
            this.draggedChip.xAnimation.forceValue(this.dragX);
            this.reorderDraggedChip(this.dragX);
            this.dragLiftAnimation.update();
            this.dragLiftAnimation.animate(1.0, 0.18, Easings.QUART_OUT);
        } else {
            this.dragLiftAnimation.update();
            this.dragLiftAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
        }

        float f = 0.0F;
        ArrayList<WatermarkChip> watermarkChips = new ArrayList<>();

        for (String string : this.chipOrderList) {
            WatermarkChip watermarkChip2 = this.chips.get(string);
            if (watermarkChip2 != null) {
                float f2 = watermarkChip2.getTargetWidth();
                if (!watermarkChip2.widthInitialized) {
                    watermarkChip2.widthAnimation.forceValue(f2);
                    watermarkChip2.widthInitialized = true;
                } else {
                    watermarkChip2.widthAnimation.update();
                    watermarkChip2.widthAnimation.animate(f2, 0.05, Easings.EXPO_OUT);
                }

                if (!(watermarkChip2.visibilityAnimation.get() <= 0.001F) || watermarkChip2.isVisible()) {
                    watermarkChips.add(watermarkChip2);
                    if (f > 0.0F) {
                        f += 4.0F * watermarkChip2.visibilityAnimation.get();
                    }

                    f += watermarkChip2.getWidth() * watermarkChip2.visibilityAnimation.get();
                }
            }
        }

        this.hasVisibleChips = !watermarkChips.isEmpty();
        if (!this.hasVisibleChips) {
            this.fadeAnimation.forceValue(0.0);
        } else {
            float f3 = f + 8.0F;
            float f4 = Math.max(f3, 1.0F);
            int i = mc.getWindow().getScaledWidth();
            String string2 = this.position.getValue();
            float f5;
            if (string2.endsWith("Center")) {
                f5 = i / 2.0F - f4 / 2.0F;
            } else if (string2.endsWith("Right")) {
                f5 = i - f4 - 5.0F;
            } else {
                f5 = 5.0F;
            }

            float f6 = f5;
            float f7 = 0.0F;
            if (string2.equals("Center")) {
                int i2 = 0;

                try {
                    Map map = ((BossBarHudAccessor)mc.inGameHud.getBossBarHud()).sadnes$getBossBars();
                    if (map != null) {
                        i2 = map.size();
                    }
                } catch (Throwable throwable) {
                }

                if (i2 > 0) {
                    f7 = i2 * 19.0F + 4.0F;
                }
            }

            this.bossBarOffsetAnimation.update();
            this.bossBarOffsetAnimation.animate(f7, 0.2, Easings.QUAD_OUT);
            float f8 = string2.startsWith("Bottom") ? mc.getWindow().getScaledHeight() - 15.0F - 5.0F : 5.0F + this.bossBarOffsetAnimation.get();
            this.panelX = f6;
            this.panelY = f8;
            this.panelWidth = f4;
            this.draggable.setSize(f4, 15.0F);
            this.draggable.setPosition(f6, f8);
            float f9 = f5 + 4.0F;

            for (WatermarkChip watermarkChip3 : watermarkChips) {
                float f10 = watermarkChip3.getWidth() * watermarkChip3.visibilityAnimation.get();
                if (!watermarkChip3.xInitialized) {
                    watermarkChip3.xAnimation.forceValue(f9);
                    watermarkChip3.xInitialized = true;
                } else if (watermarkChip3 != this.draggedChip) {
                    watermarkChip3.xAnimation.update();
                    watermarkChip3.xAnimation.animate(f9, 0.15, Easings.EXPO_OUT);
                }

                f9 += f10 + (f10 > 0.0F ? 4.0F * watermarkChip3.visibilityAnimation.get() : 0.0F);
            }

            boolean flag = this.draggedChip != null;
            this.trashAnimation.update();
            this.trashAnimation.animate(flag ? 1.0 : 0.0, 0.2, Easings.EXPO_OUT);
            float f11 = f6 + f4 + 6.0F;
            if (f11 + 15.0F > i - 5.0F) {
                f11 = f6 - 6.0F - 15.0F;
            }

            this.trashX = f11;
            this.trashY = f8;
            boolean flag2 = flag
                && render2DEvent.getMouseX() >= this.trashX
                && render2DEvent.getMouseX() <= this.trashX + 15.0F
                && render2DEvent.getMouseY() >= this.trashY
                && render2DEvent.getMouseY() <= this.trashY + 15.0F;
            this.trashHoverAnimation.update();
            this.trashHoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.EXPO_OUT);
            float f12 = this.fadeAnimation.get();
            if (!(f12 <= 0.001F)) {
                DrawContext drawContext = render2DEvent.getContext();
                float f13 = Float.MAX_VALUE;
                float f14 = -Float.MAX_VALUE;

                for (WatermarkChip watermarkChip4 : watermarkChips) {
                    if (watermarkChip4 != this.draggedChip) {
                        float f15 = watermarkChip4.xAnimation.get();
                        float f16 = watermarkChip4.getWidth() * watermarkChip4.visibilityAnimation.get();
                        if (f15 < f13) {
                            f13 = f15;
                        }

                        if (f15 + f16 > f14) {
                            f14 = f15 + f16;
                        }
                    }
                }

                float f17 = f6;
                float f18 = f4;
                if (f13 != Float.MAX_VALUE) {
                    f17 = f13 - 4.0F;
                    f18 = f14 - f13 + 8.0F;
                }

                HudPanelUtil.drawPanel(drawContext, f17, f8, f18, 15.0F, f12, 6.75F);
                int i3 = ScissorStack.depth();
                boolean flag3 = ScissorStack.push(drawContext, f17, f8, f18, 15.0F);

                try {
                    for (WatermarkChip watermarkChip5 : watermarkChips) {
                        if (watermarkChip5 != this.draggedChip) {
                            float f19 = watermarkChip5.visibilityAnimation.get() * f12;
                            if (!(f19 <= 0.001F)) {
                                watermarkChip5.render(drawContext, watermarkChip5.xAnimation.get() + watermarkChip5.getShakeOffset(), f8, 15.0F, f19);
                            }
                        }
                    }
                } finally {
                    if (flag3) {
                        ScissorStack.popTo(drawContext, i3);
                    }
                }

                float f20 = this.trashAnimation.get() * f12;
                if (f20 > 0.001F) {
                    HudPanelUtil.drawSmallPanel(drawContext, this.trashX, this.trashY, 15.0F, 15.0F, f20);
                    int i4 = ColorUtil.setAlphaf(Theme.getTextColor(), f20);
                    int i5 = ColorUtil.interpolate(i4, ColorUtil.setAlphaf(TRASH_HOVER_COLOR, f20), this.trashHoverAnimation.get());
                    new RenderUtil(drawContext, this.trashX + 3.5F, this.trashY + 3.5F, 8.0F, 8.0F).icon(Icon.TRASH, 0.0F, 0.0F, 8.0F, i5);
                }

                if (this.draggedChip != null) {
                    float f21 = this.draggedChip.visibilityAnimation.get() * f12;
                    float f22 = this.dragLiftAnimation.get();
                    float f23 = f8 - 3.0F * f22;
                    float f24 = f23 + (15.0F - 13.0F) / 2.0F;
                    GuiRenderUtil.drawHighlight(drawContext, this.dragX - 2.0F, f24, this.draggedChip.getWidth() + 4.0F, 13.0F, f22 * f21, Radius.all(4.0F));
                    this.draggedChip.render(drawContext, this.dragX, f23, 15.0F, f21);
                }
            }
        }
    }

    @EventHandler
    public void onMouseClick(ScreenKeyEvent screenKeyEvent) {
        if (this.isEditing()) {
            if (screenKeyEvent.getKey() == 0) {
                if (screenKeyEvent.isPress()) {
                    if (!HudSettingsPopupComponent.isAnyPopupHovered(screenKeyEvent.getMouseX(), screenKeyEvent.getMouseY())) {
                        WatermarkChip watermarkChip = this.getChipAt(screenKeyEvent.getMouseX(), screenKeyEvent.getMouseY());
                        if (watermarkChip instanceof Watermark.LogoChip) {
                            this.logoAnimation.forceValue(0.0);
                            this.logoAnimation.animate(1.0, 1.4, Easings.QUART_OUT);
                        }

                        if (watermarkChip != null) {
                            this.draggedChip = watermarkChip;
                            this.dragOffsetX = (float)screenKeyEvent.getMouseX() - watermarkChip.xAnimation.get();
                            this.dragX = watermarkChip.xAnimation.get();
                        }
                    }
                } else {
                    if (screenKeyEvent.isRelease() && this.draggedChip != null) {
                        boolean flag = this.trashAnimation.get() > 0.3F
                            && screenKeyEvent.getMouseX() >= this.trashX
                            && screenKeyEvent.getMouseX() <= this.trashX + 15.0F
                            && screenKeyEvent.getMouseY() >= this.trashY
                            && screenKeyEvent.getMouseY() <= this.trashY + 15.0F;
                        if (flag) {
                            this.draggedChip.getSetting().setValue(false);
                        }

                        this.draggedChip = null;
                        this.saveChipOrder();
                    }
                }
            }
        }
    }

    private void onElementsChanged() {
        List list = this.elements.getSelected();
        if (!list.isEmpty()) {
            this.lastSelected = List.copyOf(list);
        } else {
            ConfigManager configManager = Sadnes.getInstance().getConfigManager();
            if (configManager == null || !configManager.isApplying()) {
                if (!this.lastSelected.isEmpty()) {
                    WatermarkChip watermarkChip = this.chips.get(this.lastSelected.get(0));
                    if (watermarkChip != null) {
                        watermarkChip.shake();
                    }

                    this.elements.setValue(this.lastSelected);
                }
            }
        }
    }

    private void ensureSelection() {
        if (this.elements.getSelected().isEmpty() && !this.chipOrderList.isEmpty()) {
            this.elements.setSelected(this.chipOrderList.get(0), true);
        }
    }

    public static float getBottomOffset() {
        Watermark watermark = Sadnes.getInstance().getFeatureManager().getFeature(Watermark.class);
        if (watermark != null && watermark.isEnabled() && watermark.hasVisibleChips) {
            return watermark.position.getValue().equals("BottomCenter") ? 25.0F : 0.0F;
        } else {
            return 0.0F;
        }
    }

    public static float getTopOffset() {
        Watermark watermark = Sadnes.getInstance().getFeatureManager().getFeature(Watermark.class);
        if (watermark != null && watermark.isEnabled() && watermark.hasVisibleChips) {
            return watermark.position.getValue().startsWith("Bottom") ? 0.0F : watermark.panelY + 15.0F;
        } else {
            return 0.0F;
        }
    }

    private boolean isEditing() {
        return WatermarkChip.isChatOrGuiOpen();
    }

    private WatermarkChip getChipAt(double d, double d2) {
        if (!(d2 < this.panelY) && !(d2 > this.panelY + 15.0F)) {
            for (String string : this.chipOrderList) {
                WatermarkChip watermarkChip = this.chips.get(string);
                if (watermarkChip != null && watermarkChip.isVisible()) {
                    float f = watermarkChip.xAnimation.get();
                    if (d >= f && d <= f + watermarkChip.getWidth()) {
                        return watermarkChip;
                    }
                }
            }
        }

        return null;
    }

    private void reorderDraggedChip(float f) {
        if (this.draggedChip != null) {
            ArrayList arrayList = new ArrayList();

            for (String string : this.chipOrderList) {
                WatermarkChip watermarkChip = this.chips.get(string);
                if (watermarkChip != null && watermarkChip.isVisible() && watermarkChip != this.draggedChip) {
                    arrayList.add(string);
                }
            }

            float f2 = this.panelX + 4.0F;
            int i = arrayList.size();
            float f3 = f + this.draggedChip.getWidth() / 2.0F;

            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                WatermarkChip watermarkChip2 = this.chips.get(arrayList.get(i2));
                float f4 = f2 + watermarkChip2.getWidth() / 2.0F;
                if (f3 < f4) {
                    i = i2;
                    break;
                }

                f2 += watermarkChip2.getWidth() + 4.0F;
            }

            String string2 = i < arrayList.size() ? (String)arrayList.get(i) : null;
            this.chipOrderList.remove(this.draggedChip.getId());
            if (string2 == null) {
                this.chipOrderList.add(this.draggedChip.getId());
            } else {
                this.chipOrderList.add(this.chipOrderList.indexOf(string2), this.draggedChip.getId());
            }
        }
    }

    private void startSystemPolling() {
        if (this.pollThread == null) {
            Thread thread = new Thread(this::pollSystemLoop, "watermark-sys-poll");
            thread.setDaemon(true);
            this.pollThread = thread;
            thread.start();
        }
    }

    private void stopSystemPolling() {
        Thread thread = this.pollThread;
        this.pollThread = null;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void pollSystemLoop() {
        while (this.pollThread == Thread.currentThread()) {
            try {
                if (this.elements.isSelected("cpu")) {
                    double d = SystemStats.getCpuUsage();
                    if (d >= 0.0) {
                        this.cpuLoad = d;
                    }
                }

                if (this.elements.isSelected("cpufreq")) {
                    double d2 = SystemStats.getCpuFrequencyGhz();
                    if (d2 >= 0.0) {
                        this.cpuFrequency = d2;
                    }
                }

                if (this.elements.isSelected("ram")) {
                    long[] longs = SystemStats.getMemoryTotalAndUsed();
                    if (longs != null) {
                        this.totalMemory = longs[0];
                        this.usedMemory = longs[1];
                    }
                }

                if (this.elements.isSelected("gpu")) {
                    double d3 = SystemStats.getGpuUsage();
                    if (d3 >= 0.0) {
                        this.gpuLoad = d3;
                    }
                }
            } catch (Throwable throwable) {
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException interruptedException) {
                break;
            }
        }
    }

    String getServerAddress() {
        if (mc.isInSingleplayer()) {
            return "Singleplayer";
        }

        ServerInfo serverInfo = mc.getCurrentServerEntry();
        return serverInfo != null ? serverInfo.address : "—";
    }

    boolean isLocalServer() {
        if (mc.isInSingleplayer()) {
            return true;
        } else {
            ServerInfo serverInfo = mc.getCurrentServerEntry();
            if (serverInfo != null) {
                String string = serverInfo.address.toLowerCase();
                return string.contains("localhost") || string.startsWith("127.0.0.1");
            } else {
                return false;
            }
        }
    }

    String getCoordinates() {
        return mc.player == null ? "—" : mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
    }

    double getHorizontalSpeed() {
        if (mc.player == null) {
            return 0.0;
        }

        double d = mc.player.getX() - mc.player.lastX;
        double d2 = mc.player.getZ() - mc.player.lastZ;
        return Math.sqrt(d * d + d2 * d2) * 20.0;
    }

    static float warningLevel(float f, float f2, float f3) {
        return f3 > f2 ? clamp01((f - f2) / (f3 - f2)) : clamp01((f2 - f) / (f2 - f3));
    }

    private static float clamp01(float f) {
        if (f < 0.0F) {
            return 0.0F;
        } else {
            return f > 1.0F ? 1.0F : f;
        }
    }

    NetworkStatsComponent getNetworkStats() {
        return Sadnes.getInstance().getComponentManager().getComponent(NetworkStatsComponent.class);
    }

    String formatDecimal(double d, int i) {
        long l = i == 1 ? 10L : 100L;
        long l2 = Math.round(Math.abs(d) * l);
        long l3 = l2 / l;
        long l4 = l2 % l;
        StringBuilder builder = new StringBuilder(12);
        if (d < 0.0 && l2 != 0L) {
            builder.append('-');
        }

        builder.append(l3);
        if (l4 == 0L) {
            return builder.toString();
        }

        builder.append('.');
        if (i == 2 && l4 < 10L) {
            builder.append('0');
        }

        return builder.append(l4).toString();
    }

    final class AvatarChip extends WatermarkChip {
        AvatarChip(BooleanSetting booleanSetting) {
            super("avatar", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.USER;
        }

        @Override
        public String getDisplayText() {
            return "";
        }

        @Override
        public float getIconSize() {
            return 12.0F;
        }

        @Override
        public float getPadding() {
            return -2.0F;
        }

        @Override
        public void renderIcon(DrawContext drawContext, float f, float f2, float f3, float f4) {
            Identifier identifier = AvatarTextureLoader.getTexture(Profile.getAvatarUrl());
            if (identifier != null) {
                GuiRenderUtil.bindTexture(identifier);
                if (GuiRenderUtil.getBoundTexture() != null) {
                    new RenderUtil(drawContext, f, f2, f3, f3).frostedGlass(f4, Radius.all(f3 / 2.0F));
                    return;
                }
            }

            GuiRenderUtil.bindTexture(Texture.AVATAR_DEFAULT);
            if (GuiRenderUtil.getBoundTexture() != null) {
                new RenderUtil(drawContext, f, f2, f3, f3).frostedGlass(f4, Radius.all(f3 / 2.0F));
            } else {
                super.renderIcon(drawContext, f, f2, f3, f4);
            }
        }
    }

    final class ConnectivityChip extends WatermarkChip {
        ConnectivityChip(BooleanSetting booleanSetting) {
            super("connectivity", booleanSetting);
        }

        @Override
        public boolean isActive() {
            return !Watermark.this.isLocalServer() && this.hasConnectivityIssue();
        }

        @Override
        public Icon getIcon() {
            return Icon.LINK_SLASH;
        }

        @Override
        public String getLabel() {
            return this.hasConnectivityIssue() ? "Yes" : "No";
        }

        @Override
        public float getWarningLevel() {
            return this.hasConnectivityIssue() ? 1.0F : 0.0F;
        }

        private boolean hasConnectivityIssue() {
            return Watermark.this.getNetworkStats() != null && Watermark.this.getNetworkStats().isLagging();
        }
    }

    final class CoordsChip extends WatermarkChip {
        CoordsChip(BooleanSetting booleanSetting) {
            super("coords", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.LOCATION_CROSSHAIRS;
        }

        @Override
        public String getLabel() {
            return Watermark.this.getCoordinates();
        }
    }

    final class CpuChip extends WatermarkChip {
        CpuChip(BooleanSetting booleanSetting) {
            super("cpu", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.MICROCHIP;
        }

        @Override
        public String getLabel() {
            return Math.round(Watermark.this.cpuLoad * 100.0) + "%";
        }

        @Override
        public String getValueText() {
            return "cpu";
        }

        @Override
        public float getWarningLevel() {
            return Watermark.warningLevel((float)Watermark.this.cpuLoad, 0.6F, 0.9F);
        }
    }

    final class CpuCoresChip extends WatermarkChip {
        CpuCoresChip(BooleanSetting booleanSetting) {
            super("cpucores", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.CHART_SIMPLE;
        }

        @Override
        public String getLabel() {
            return String.valueOf(Runtime.getRuntime().availableProcessors());
        }
    }

    final class CpuFreqChip extends WatermarkChip {
        CpuFreqChip(BooleanSetting booleanSetting) {
            super("cpufreq", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.CHART_LINE;
        }

        @Override
        public String getLabel() {
            return Watermark.this.cpuFrequency <= 0.0 ? "—" : Watermark.this.formatDecimal(Watermark.this.cpuFrequency, 2);
        }

        @Override
        public String getValueText() {
            return "GHz";
        }
    }

    final class FpsChip extends WatermarkChip {
        FpsChip(BooleanSetting booleanSetting) {
            super("fps", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.GAUGE;
        }

        @Override
        public String getLabel() {
            return String.valueOf((int)Watermark.this.fpsAnimation.get());
        }

        @Override
        public String getValueText() {
            return "fps";
        }

        @Override
        public float getWarningLevel() {
            return Watermark.warningLevel(Watermark.this.fpsAnimation.get(), 60.0F, 25.0F);
        }
    }

    final class GpuChip extends WatermarkChip {
        GpuChip(BooleanSetting booleanSetting) {
            super("gpu", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.DISPLAY;
        }

        @Override
        public String getLabel() {
            return Watermark.this.gpuLoad < 0.0 ? "—" : Math.round(Watermark.this.gpuLoad) + "%";
        }

        @Override
        public String getValueText() {
            return "gpu";
        }

        @Override
        public float getWarningLevel() {
            return Watermark.this.gpuLoad < 0.0 ? 0.0F : Watermark.warningLevel((float)Watermark.this.gpuLoad, 70.0F, 95.0F);
        }
    }

    final class IpChip extends WatermarkChip {
        IpChip(BooleanSetting booleanSetting) {
            super("ip", booleanSetting);
        }

        @Override
        public boolean isActive() {
            return !Watermark.this.isLocalServer();
        }

        @Override
        public Icon getIcon() {
            return Icon.GLOBE;
        }

        @Override
        public String getLabel() {
            return Watermark.this.getServerAddress();
        }
    }

    final class JitterChip extends WatermarkChip {
        JitterChip(BooleanSetting booleanSetting) {
            super("jitter", booleanSetting);
        }

        @Override
        public boolean isActive() {
            return !Watermark.this.isLocalServer();
        }

        @Override
        public Icon getIcon() {
            return Icon.WAVE_SQUARE;
        }

        @Override
        public String getLabel() {
            return Watermark.this.getNetworkStats() != null ? String.valueOf(Watermark.this.getNetworkStats().getJitter()) : String.valueOf(0);
        }

        @Override
        public String getValueText() {
            return "ms";
        }

        @Override
        public float getWarningLevel() {
            return Watermark.this.getNetworkStats() != null
                ? Watermark.warningLevel(Watermark.this.getNetworkStats().getJitter(), 10.0F, 80.0F)
                : Watermark.warningLevel(0, 10.0F, 80.0F);
        }
    }

    final class KdChip extends WatermarkChip {
        KdChip(BooleanSetting booleanSetting) {
            super("kd", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.PERCENT;
        }

        @Override
        public String getLabel() {
            KillDeathTrackerComponent killDeathTrackerComponent = Sadnes.getInstance().getComponentManager().getComponent(KillDeathTrackerComponent.class);
            return killDeathTrackerComponent != null ? killDeathTrackerComponent.getKdRatioText() : "0.0";
        }
    }

    final class LogoChip extends WatermarkChip {
        LogoChip(BooleanSetting booleanSetting) {
            super("logo", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.MAIN;
        }

        @Override
        public String getDisplayText() {
            return "";
        }

        @Override
        public float getIconSize() {
            return 10.0F;
        }

        @Override
        public void renderIcon(DrawContext drawContext, float f, float f2, float f3, float f4) {
            float f5 = Watermark.this.logoAnimation.get();
            int i = ColorUtil.fade(8, 0, Theme.getPrimary(), Theme.getAccentLight());
            int i2 = ColorUtil.setAlphaf(i, f4);
            new RenderUtil(drawContext, f, f2, f3, f3).spriteAtProgress(AnimatedIcon.MAIN, -f3 / 2.0F, -f3 / 2.0F, f3 * 2.0F, f3 * 2.0F, i2, f5);
        }
    }

    final class LossChip extends WatermarkChip {
        LossChip(BooleanSetting booleanSetting) {
            super("loss", booleanSetting);
        }

        @Override
        public boolean isActive() {
            return !Watermark.this.isLocalServer();
        }

        @Override
        public Icon getIcon() {
            return Icon.TOWER_BROADCAST;
        }

        @Override
        public String getLabel() {
            Watermark watermark = Watermark.this;
            return Watermark.this.getNetworkStats() != null
                ? watermark.formatDecimal(Watermark.this.getNetworkStats().getPacketLoss(), 1)
                : watermark.formatDecimal(0.0, 1);
        }

        @Override
        public String getValueText() {
            return "LOSS";
        }

        @Override
        public float getWarningLevel() {
            return Watermark.this.getNetworkStats() != null
                ? Watermark.warningLevel((float)Watermark.this.getNetworkStats().getPacketLoss(), 0.0F, 25.0F)
                : Watermark.warningLevel((float)0.0, 0.0F, 25.0F);
        }
    }

    final class PingChip extends WatermarkChip {
        PingChip(BooleanSetting booleanSetting) {
            super("ping", booleanSetting);
        }

        @Override
        public boolean isActive() {
            return !Watermark.this.isLocalServer();
        }

        @Override
        public Icon getIcon() {
            return Icon.SATELLITE_DISH;
        }

        @Override
        public String getLabel() {
            return String.valueOf(ServerManager.getPing());
        }

        @Override
        public String getValueText() {
            return "ms";
        }

        @Override
        public float getWarningLevel() {
            return Watermark.this.getNetworkStats() != null
                ? Watermark.warningLevel(Watermark.this.getNetworkStats().getPing(), 70.0F, 200.0F)
                : Watermark.warningLevel(0, 70.0F, 200.0F);
        }
    }

    final class PresetChip extends WatermarkChip {
        PresetChip(BooleanSetting booleanSetting) {
            super("preset", booleanSetting);
        }

        private ConfigEntry getCurrentConfig() {
            ConfigManager configManager = Sadnes.getInstance().getConfigManager();
            return configManager != null ? configManager.getEntry(configManager.getActiveConfig()) : null;
        }

        @Override
        public Icon getIcon() {
            ConfigEntry configEntry = this.getCurrentConfig();
            return configEntry != null && configEntry.getIcon() != null ? configEntry.getIcon() : Icon.CLOUD;
        }

        @Override
        public int getIconColor() {
            ConfigEntry configEntry = this.getCurrentConfig();
            return configEntry != null ? configEntry.getColor() : 0;
        }

        @Override
        public String getLabel() {
            ConfigManager configManager = Sadnes.getInstance().getConfigManager();
            return configManager != null ? configManager.getActiveConfig() : "default";
        }
    }

    final class PvpCooldownChip extends WatermarkChip {
        PvpCooldownChip(BooleanSetting booleanSetting) {
            super("pvpcd", booleanSetting);
        }

        @Override
        public boolean isActive() {
            return ServerManager.getPvpTime() > 0;
        }

        @Override
        public Icon getIcon() {
            return Icon.SWORDS;
        }

        @Override
        public String getLabel() {
            int i = Math.max(0, ServerManager.getPvpTime());
            return i / 60 + ":" + (i % 60 < 10 ? "0" : "") + i % 60;
        }

        @Override
        public float getWarningLevel() {
            int i = ServerManager.getPvpTime();
            return i < 0 ? 0.0F : Watermark.warningLevel(i, 30.0F, 5.0F);
        }
    }

    final class RamChip extends WatermarkChip {
        RamChip(BooleanSetting booleanSetting) {
            super("ram", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.MEMORY;
        }

        @Override
        public String getLabel() {
            return Watermark.this.totalMemory <= 0L ? "—" : Watermark.this.usedMemory / 1048576L + "";
        }

        @Override
        public String getValueText() {
            return "mb";
        }

        @Override
        public float getWarningLevel() {
            if (Watermark.this.totalMemory <= 0L) {
                return 0.0F;
            }

            float f = (float)Watermark.this.usedMemory / (float)Watermark.this.totalMemory;
            return Watermark.warningLevel(f, 0.7F, 0.92F);
        }
    }

    final class SpeedChip extends WatermarkChip {
        SpeedChip(BooleanSetting booleanSetting) {
            super("speed", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.RUN;
        }

        @Override
        public String getLabel() {
            return Watermark.this.formatDecimal(Watermark.this.getHorizontalSpeed(), 2);
        }

        @Override
        public String getValueText() {
            return "bps";
        }
    }

    final class TimeChip extends WatermarkChip {
        TimeChip(BooleanSetting booleanSetting) {
            super("time", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.CLOCK_HOUR_4;
        }

        @Override
        public String getLabel() {
            return LocalTime.now().format(Watermark.TIME_FORMAT);
        }
    }

    final class TpsChip extends WatermarkChip {
        TpsChip(BooleanSetting booleanSetting) {
            super("tps", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.SERVER;
        }

        @Override
        public String getLabel() {
            Watermark watermark = Watermark.this;
            return Watermark.this.getNetworkStats() != null
                ? watermark.formatDecimal(Watermark.this.getNetworkStats().getTps(), 1)
                : watermark.formatDecimal(20.0, 1);
        }

        @Override
        public String getValueText() {
            return "tps";
        }

        @Override
        public float getWarningLevel() {
            return Watermark.this.getNetworkStats() != null
                ? Watermark.warningLevel((float)Watermark.this.getNetworkStats().getTps(), 19.0F, 14.0F)
                : Watermark.warningLevel((float)20.0, 19.0F, 14.0F);
        }
    }

    final class UserChip extends WatermarkChip {
        UserChip(BooleanSetting booleanSetting) {
            super("user", booleanSetting);
        }

        @Override
        public Icon getIcon() {
            return Icon.USER;
        }

        @Override
        public String getDisplayText() {
            return Profile.getUsername();
        }
    }

    final class VarianceChip extends WatermarkChip {
        VarianceChip(BooleanSetting booleanSetting) {
            super("var", booleanSetting);
        }

        @Override
        public boolean isActive() {
            return !Watermark.this.isLocalServer();
        }

        @Override
        public Icon getIcon() {
            return Icon.SQUARE_ROOT_VARIABLE;
        }

        @Override
        public String getLabel() {
            Watermark watermark = Watermark.this;
            return Watermark.this.getNetworkStats() != null
                ? watermark.formatDecimal(Watermark.this.getNetworkStats().getVariance(), 1)
                : watermark.formatDecimal(0.0, 1);
        }

        @Override
        public String getValueText() {
            return "VAR";
        }
    }
}
