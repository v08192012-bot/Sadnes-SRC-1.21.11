package tech.sadnes.client.ui.screen.settings;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer.Pack;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GlowBorderRenderer;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.IconHoverAnimator;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class ResourcePacksPanel implements IRenderable {
    private static final Identifier UNKNOWN_PACK_ICON = Identifier.ofVanilla("textures/misc/unknown_pack.png");
    private static final float ROW_HEIGHT = 30.0F;
    private static final float PADDING = 12.0F;
    private static final float SIDE_PADDING = 12.0F;
    private static final float SECTION_HEADER_HEIGHT = 21.0F;
    private static final float ICON_SIZE = 22.0F;
    private final MinecraftClient client;
    private final ResourcePackManager packManager;
    private final ResourcePackOrganizer packOrganizer;
    private final Map<String, Identifier> packIcons = new HashMap<>();
    private final Map<String, Animation> highlightAnimations = new HashMap<>();
    private final SmoothScrollbar scrollbar = new SmoothScrollbar();
    private final List<ResourcePacksPanel.ClickRegion> clickRegions = new ArrayList<>();
    private List<Pack> enabledPacks = new ArrayList<>();
    private List<Pack> availablePacks = new ArrayList<>();
    private boolean dirty;
    private float clipTop;
    private float clipBottom;
    private final Map<Icon, IconHoverAnimator> iconAnimators = new EnumMap<>(Icon.class);

    public ResourcePacksPanel(MinecraftClient minecraftClient) {
        this.client = minecraftClient;
        this.packManager = minecraftClient.getResourcePackManager();
        this.packManager.scanPacks();
        this.packOrganizer = new ResourcePackOrganizer(abstractPack -> {}, this::getPackIcon, this.packManager, resourcePackManager -> minecraftClient.options.refreshResourcePacks(resourcePackManager));
        this.refreshPacks();
    }

    private void refreshPacks() {
        this.enabledPacks = this.packOrganizer.getEnabledPacks().collect(Collectors.toList());
        this.availablePacks = this.packOrganizer.getDisabledPacks().collect(Collectors.toList());
    }

    public void applyChanges() {
        if (this.dirty) {
            this.packOrganizer.apply();
            this.dirty = false;
        }
    }

    private Identifier getPackIcon(ResourcePackProfile resourcePackProfile) {
        return this.packIcons.computeIfAbsent(resourcePackProfile.getId(), string -> this.loadPackIcon(resourcePackProfile));
    }

    private Identifier loadPackIcon(ResourcePackProfile resourcePackProfile) {
        try (ResourcePack resourcePack = resourcePackProfile.createResourcePack()) {
            InputSupplier inputSupplier = resourcePack.openRoot("pack.png");
            if (inputSupplier == null) {
                return UNKNOWN_PACK_ICON;
            }

            Identifier identifier;
            try (InputStream inputStream = (InputStream)inputSupplier.get()) {
                NativeImage nativeImage = NativeImage.read(inputStream);

                try {
                    String string = resourcePackProfile.getId().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.\\-]", "_");
                    identifier = Identifier.ofVanilla("pack/" + string + "_" + Integer.toHexString(resourcePackProfile.getId().hashCode()) + "/icon");
                    this.client.getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(identifier::toString, nativeImage));
                } catch (Exception exception) {
                    nativeImage.close();
                    return UNKNOWN_PACK_ICON;
                }
            }

            return identifier;
        } catch (Exception exception2) {
            return UNKNOWN_PACK_ICON;
        }
    }

    public void releaseIcons() {
        for (Identifier identifier : this.packIcons.values()) {
            if (!identifier.equals(UNKNOWN_PACK_ICON)) {
                try {
                    this.client.getTextureManager().destroyTexture(identifier);
                } catch (Exception exception) {
                }
            }
        }

        this.packIcons.clear();
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5, String string) {
        this.clickRegions.clear();
        this.clipTop = f2;
        this.clipBottom = f2 + f4;
        List<Pack> packs = this.filterPacks(this.enabledPacks, string);
        List<Pack> packs2 = this.filterPacks(this.availablePacks, string);
        String string2 = this.enabledPacks.isEmpty() ? null : this.enabledPacks.get(0).getName();
        this.updateHighlights(this.enabledPacks, string2);
        this.updateHighlights(this.availablePacks, string2);
        float f6 = f + 12.0F;
        float f7 = f3 - 24.0F - 6.0F;
        this.renderOpenFolderButton(drawContext, f6, f2 + 2.0F, f7, 18.0F, i, i2, f5);
        float f8 = f2 + 2.0F + 18.0F + 8.0F;
        float f9 = Math.max(0.0F, f4 - (f8 - f2));
        this.clipTop = f8;
        this.clipBottom = f8 + f9;
        float f10 = 21.0F + packs.size() * 42.0F + 21.0F + packs2.size() * 42.0F + 10.0F;
        this.scrollbar.update(f10, f9);
        this.scrollbar.updateHover(i, i2);
        boolean flag = ScissorStack.push(drawContext, f, f8, f3, f9);

        try {
            float f11 = this.scrollbar.getScroll();
            float f12 = f8 + f11;
            int i3 = 0;
            float f13 = this.renderSectionHeader(drawContext, f6, f12, f7, "СПИСОК ВКЛЮЧЕННЫХ · " + packs.size(), f5);

            for (Pack pack : packs) {
                this.renderPackRow(drawContext, f6, f13, f7, f8, f9, i, i2, f5, pack, true, i3++);
                f13 += 42.0F;
            }

            float f14 = this.renderSectionHeader(drawContext, f6, f13, f7, "СПИСОК ВЫКЛЮЧЕННЫХ · " + packs2.size(), f5);

            for (Pack pack2 : packs2) {
                this.renderPackRow(drawContext, f6, f14, f7, f8, f9, i, i2, f5, pack2, false, i3++);
                f14 += 42.0F;
            }

            if (packs.isEmpty() && packs2.isEmpty()) {
                new RenderUtil(drawContext, f, f8, f3, f9)
                    .textCenter(Fonts.INTER_MEDIUM, "Ресурспаков не найдено", Theme.withAlpha(Theme.getSecondaryText(), 0.8F * f5), 7.0F);
            }
        } finally {
            if (flag) {
                ScissorStack.pop(drawContext);
            }
        }

        this.scrollbar.render(drawContext, f + f3 - 2.0F, f8, f9, f10, f5);
    }

    private void renderOpenFolderButton(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        boolean flag = this.isHovered(i, i2, f, f2, f3, f4);
        Radius radius = Radius.smoothAll(9.0F);
        new RenderUtil(drawContext, f, f2, f3, f4).rect(Theme.withAlpha(ColorUtil.BLACK, 0.55F * f5), radius);
        new RenderUtil(drawContext, f, f2, f3, f4).rect(Theme.withAlpha(Theme.getBase(), 0.9F * f5), radius);
        new RenderUtil(drawContext, f, f2, f3, f4).shadow(0.75F, Theme.withAlpha(Theme.getPrimary(), (flag ? 0.55F : 0.32F) * f5), radius);
        String string = "Открыть наборы";
        float f6 = Fonts.INTER_MEDIUM.getWidth(string, 6.5F);
        float f7 = f + (f3 - (8.0F + 5.0F + f6)) / 2.0F;
        new RenderUtil(drawContext, f, f2, f3, f4).icon(Icon.FOLDER, f7 - f, (f4 - 8.0F) / 2.0F, 8.0F, Theme.withAlpha(Theme.getPrimary(), f5));
        new RenderUtil(drawContext, f, f2, f3, f4)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, f7 - f + 8.0F + 5.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f5), 6.5F);
        this.clickRegions.add(new ResourcePacksPanel.ClickRegion(f, f2, f3, f4, this::openResourcePackFolder));
    }

    private void openResourcePackFolder() {
        try {
            Util.getOperatingSystem().open(this.client.getResourcePackDir().toUri());
        } catch (Exception exception) {
        }
    }

    private void updateHighlights(List<Pack> packs, String string) {
        for (Pack pack : packs) {
            Animation animation = this.highlightAnimations.computeIfAbsent(pack.getName(), string2 -> new Animation());
            animation.update();
            animation.animate(pack.getName().equals(string) ? 1.0 : 0.0, 0.35, Easings.QUAD_OUT);
        }
    }

    private float getHighlight(String string) {
        Animation animation = this.highlightAnimations.get(string);
        return animation == null ? 0.0F : animation.get();
    }

    private float renderSectionHeader(DrawContext drawContext, float f, float f2, float f3, String string, float f4) {
        new RenderUtil(drawContext, f + 2.0F, f2 + 3.0F, f3, 8.0F)
            .textTopLeft(Fonts.INTER_SEMIBOLD, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.9F * f4), 6.0F);
        return f2 + 21.0F;
    }

    private void renderPackRow(
        DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i, int i2, float f6, Pack pack, boolean flag, int i3
    ) {
        if (!(f2 + 30.0F < f4) && !(f2 > f4 + f5)) {
            this.isHovered(i, i2, f, f2, f3, 30.0);
            float f7 = this.getHighlight(pack.getName());
            if (f7 > 0.01F) {
                GlowBorderRenderer.drawGlow(drawContext, f, f2, f3, 30.0F, f7, f6, 10.0F, Theme.getPrimary(), 0.65F);
            }

            Radius radius = Radius.all(10.0F);
            new RenderUtil(drawContext, f, f2, f3, 30.0F).rect(Theme.withAlpha(ColorUtil.BLACK, 0.55F * f6), radius);
            new RenderUtil(drawContext, f, f2, f3, 30.0F).rect(Theme.withAlpha(Theme.getBase(), 0.92F * f6), radius);
            new RenderUtil(drawContext, f, f2, f3, 30.0F).shadow(0.75F, Theme.withAlpha(ColorUtil.WHITE, 0.05F * f6), radius);
            float f8 = f + 6.0F;
            float f9 = f2 + 4.0F;
            Identifier identifier = pack.getIconId();
            boolean flag2 = !identifier.equals(UNKNOWN_PACK_ICON);
            if (flag2) {
                GuiRenderUtil.bindTexture(identifier);
            }

            if (flag2 && GuiRenderUtil.getBoundTexture() != null) {
                new RenderUtil(drawContext, f8, f9, 22.0F, 22.0F).frostedGlass(flag ? f6 : 0.6F * f6, Radius.all(6.0F));
            } else {
                new RenderUtil(drawContext, f8, f9, 22.0F, 22.0F).rect(Theme.withAlpha(Theme.getBase(), f6), Radius.all(6.0F));
                new RenderUtil(drawContext, f8, f9, 22.0F, 22.0F).icon(Icon.PACKAGE, 5.5F, 5.5F, 11.0F, Theme.withAlpha(Theme.getSecondaryText(), f6));
            }

            new RenderUtil(drawContext, f8, f9, 22.0F, 22.0F).shadow(0.75F, Theme.withAlpha(ColorUtil.WHITE, 0.18F * f6), Radius.all(6.0F));
            float f10 = f8 + 22.0F + 8.0F;
            float f11 = flag ? 50.0F : 20.0F;
            float f12 = f + f3 - f11 - 6.0F - f10;
            float f13 = flag ? f6 : 0.75F * f6;
            new RenderUtil(drawContext, f10, f2 + 6.0F, f12, 8.0F)
                .textTopLeft(
                    Fonts.INTER_MEDIUM, truncate(pack.getDisplayName().getString(), f12), 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f13), 7.0F
                );
            String string = pack.getDescription().getString();
            if (!string.isEmpty()) {
                new RenderUtil(drawContext, f10, f2 + 16.0F, f12, 8.0F)
                    .textTopLeft(Fonts.INTER_REGULAR, truncate(string, f12), 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.75F * f6), 6.0F);
            }

            float f14 = f2 + (30.0F - 13.0F) / 2.0F;
            float f15 = f + f3 - 13.0F - 4.0F;
            if (!flag) {
                this.renderIconButton(drawContext, f15, f14, 13.0F, Icon.PLUS, Theme.getPrimary(), i, i2, f6, () -> {
                    pack.enable();
                    this.refreshPacks();
                    this.dirty = true;
                });
                return;
            }

            if (pack.canBeDisabled()) {
                this.renderIconButton(drawContext, f15, f14, 13.0F, Icon.CLOSE, Theme.getPrimary(), i, i2, f6, () -> {
                    pack.disable();
                    this.refreshPacks();
                    this.dirty = true;
                });
                f15 -= 13.0F + 3.0F;
            }

            if (pack.canMoveTowardEnd()) {
                this.renderIconButton(drawContext, f15, f14, 13.0F, Icon.SORT_DESC, Theme.getTextColor(), i, i2, f6, () -> {
                    pack.moveTowardEnd();
                    this.refreshPacks();
                    this.dirty = true;
                });
                f15 -= 13.0F + 3.0F;
            }

            if (pack.canMoveTowardStart()) {
                this.renderIconButton(drawContext, f15, f14, 13.0F, Icon.SORT_ASC, Theme.getTextColor(), i, i2, f6, () -> {
                    pack.moveTowardStart();
                    this.refreshPacks();
                    this.dirty = true;
                });
                return;
            }
        }
    }

    private void renderIconButton(DrawContext drawContext, float f, float f2, float f3, Icon icon, int i, int i2, int i3, float f4, Runnable runnable) {
        boolean flag = this.isHovered(i2, i3, f, f2, f3, f3);
        new RenderUtil(drawContext, f, f2, f3, f3).rect(Theme.withAlpha(i, (flag ? 0.3F : 0.14F) * f4), Radius.all(4.0F));
        this.iconAnimators
            .computeIfAbsent(icon, icon2 -> new IconHoverAnimator())
            .render(
                drawContext,
                icon,
                f + (f3 - 7.0F) / 2.0F,
                f2 + (f3 - 7.0F) / 2.0F,
                7.0F,
                Theme.withAlpha(ColorUtil.interpolate(i, ColorUtil.WHITE, flag ? 0.5F : 0.0F), f4),
                flag ? 1.0F : 0.0F
            );
        float f5 = f2 + f3 / 2.0F;
        if (f5 >= this.clipTop && f5 <= this.clipBottom) {
            this.clickRegions.add(new ResourcePacksPanel.ClickRegion(f, f2, f3, f3, runnable));
        }
    }

    private List<Pack> filterPacks(List<Pack> packs, String string) {
        if (string != null && !string.isBlank()) {
            String string2 = string.toLowerCase(Locale.ROOT);
            return packs.stream().filter(pack2 -> pack2.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(string2)).collect(Collectors.toList());
        } else {
            return packs;
        }
    }

    public boolean mouseClicked(double d, double d2) {
        if (this.scrollbar.mouseClicked(d, d2)) {
            return true;
        }

        for (ResourcePacksPanel.ClickRegion clickRegion : this.clickRegions) {
            if (this.isHovered(d, d2, clickRegion.x, clickRegion.y, clickRegion.w, clickRegion.h)) {
                clickRegion.action.run();
                return true;
            }
        }

        return false;
    }

    public boolean mouseDragged(double d, double d2) {
        return this.scrollbar.mouseDragged(d, d2);
    }

    public void mouseReleased() {
        this.scrollbar.mouseReleased();
    }

    public void mouseScrolled(double d) {
        this.scrollbar.onScroll(d);
    }

    private static String truncate(String string, float f) {
        if (f <= 0.0F) {
            return "";
        }

        if (Fonts.INTER_MEDIUM.getWidth(string, 6.5F) <= f) {
            return string;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < string.length() && !(Fonts.INTER_MEDIUM.getWidth(builder.toString() + string.charAt(i) + "…", 6.5F) > f); i++) {
            builder.append(string.charAt(i));
        }

        return builder + "…";
    }

    record ClickRegion(float x, float y, float w, float h, Runnable action) {
    }
}
