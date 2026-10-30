package tech.sadnes.client.feature.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.AutoCrystal;
import tech.sadnes.client.feature.impl.misc.ClientSpoof;
import tech.sadnes.client.render.GuiParticle;
import tech.sadnes.client.render.GuiParticleSystem;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.TexturedShapeRenderer;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.ItemColorUtil;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.script.ScriptAuraHooks;
import tech.sadnes.client.script.ScriptTargetHudStyles;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.hud.Draggable;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "TargetHud",
    description = "feature.impl.hud.targethud",
    displayName = "feature.impl.hud.targethud",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_STATUS
)
public class TargetHud extends Feature implements Draggable {
    private static final float WIDTH = 100.0F;
    private static final float HEIGHT = 34.0F;
    private static final float PADDING2 = 5.0F;
    private static final float AVATAR_SIZE = 24.0F;
    private static final float CONTENT_OFFSET_X = 34.0F;
    private static final float BAR_WIDTH = 61.0F;
    private static final float BAR_HEIGHT = 5.0F;
    private static final float LINE_SPACING = 3.0F;
    private static final float TOP_SPACING = 5.0F;
    private static final float ITEM_SIZE = 8.0F;
    private static final float AVATAR_RADIUS = 4.0F;
    private static final float PARTICLE_INSET = 2.0F;
    private static final long TARGET_TIMEOUT_MS = 800L;
    private static final float FACE_UV_MIN = 0.125F;
    private static final float FACE_UV_MAX = 0.25F;
    private static final float HAT_U_MIN = 0.625F;
    private static final float HAT_U_MAX = 0.75F;
    private static final int ABSORPTION_COLOR_DARK = ColorUtil.rgb(130, 79, 4);
    static final int ABSORPTION_COLOR = ColorUtil.rgb(235, 161, 52);
    private static final float HEART_SIZE = 7.0F;
    private static final float HEART_GAP = 2.0F;
    private static final float COPY_ICON_SIZE = 7.5F;
    private static final float NAME_HEIGHT = 8.0F;
    private static final float ITEM_GAP = 0.5F;
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final float COPY_ICON_OFFSET = 9.5F;
    private static final long COPY_HOVER_DELAY_MS = 500L;
    private static final long COPIED_DISPLAY_MS = 1000L;
    private static final int COPIED_COLOR = ColorUtil.rgb(95, 220, 130);
    static final int LOW_HEALTH_COLOR = ColorUtil.rgb(235, 75, 75);
    private static final int HIGH_HEALTH_COLOR = ColorUtil.rgb(110, 215, 120);
    private final DragSetting draggable;
    private static final String[] STYLES = new String[]{"classic", "ring"};
    private final ModeSetting style = new ModeSetting("targethud.style", STYLES);
    private final BooleanSetting showArmor = createBooleanSetting("targethud.showArmor", true, "Показывать броню");
    private final BooleanSetting showOnLook = createBooleanSetting("targethud.showOnLook", true, "Показывать при наводке");
    private final BooleanSetting hitParticles = new BooleanSetting("targethud.hitParticles", true);
    private final BooleanSetting eatParticles = new BooleanSetting("targethud.eatParticles", true).setVisibleWhen(() -> this.style.is("classic"));
    private final TargetUsingItemIndicator avatarOverlay = new TargetUsingItemIndicator();
    private final TargetHudRingRenderer ringRenderer = new TargetHudRingRenderer(this.hitParticles::getValue, this.showArmor::getValue, this.avatarOverlay);
    private final Animation visibilityAnimation = new Animation();
    private final Animation healthAnimation = new Animation();
    private final Animation absorptionAnimation = new Animation();
    private final Animation healthTrailAnimation = new Animation();
    private final Animation absorptionTrailAnimation = new Animation();
    private final Animation itemsAnimation = new Animation();
    private final Animation copyHoverAnimation = new Animation();
    private final Animation copiedAnimation = new Animation();
    private final StopWatch copyHoverTimer = new StopWatch();
    private LivingEntity target;
    private long lastTargetTime;
    private final GuiParticleSystem hitParticleSystem = new GuiParticleSystem();
    private final GuiParticleSystem eatParticleSystem = new GuiParticleSystem();
    private final StopWatch eatParticleTimer = new StopWatch();
    private int mouseX;
    private int mouseY;
    private long copiedUntil;
    private float nameX;
    private float nameY;
    private float nameWidth;
    private float nameHeight;
    private String targetName = "";
    private boolean hasAbsorption;

    private static BooleanSetting createBooleanSetting(String string, boolean flag, String string2) {
        BooleanSetting booleanSetting = new BooleanSetting(string, flag);
        booleanSetting.setDisplayName(string2);
        return booleanSetting;
    }

    public TargetHud() {
        this.draggable = new DragSetting(this.getInfo().name(), 10.0F, 90.0F);
        this.draggable.setSize(100.0F, 34.0F);
        this.draggable.setOnTop(true);
        this.draggable.onInput((drawContext, i, i2, f) -> {
            this.mouseX = i;
            this.mouseY = i2;
            this.render(drawContext);
        });
        this.draggable.onInput(this);
        this.draggable.setRenderCondition(() -> this.isEnabled() && this.target != null && this.visibilityAnimation.get() > 0.01F ? true : false);
        DragComponent.register(this.draggable);
    }

    public DragSetting getDraggable() {
        return this.draggable;
    }

    public void updateStyles() {
        this.style.setModes(STYLES, ScriptTargetHudStyles.getStyleNames());
    }

    @Override
    public void render(DrawContext drawContext) {
        this.draggable.setDraggable(this::isEnabled);
        if (this.isEnabled()) {
            String string = ScriptTargetHudStyles.isAvailable(this.style.getValue()) ? this.style.getValue() : null;
            boolean flag = this.style.is("ring");
            if (string != null) {
                this.draggable.setSize(ScriptTargetHudStyles.getWidth(string), ScriptTargetHudStyles.getHeight(string));
            } else {
                this.draggable.setSize(flag ? 114.0F : 100.0F, flag ? 28.0F : 34.0F);
            }

            LivingEntity livingEntity = this.findTarget();
            if (livingEntity != null) {
                this.target = livingEntity;
                this.lastTargetTime = System.currentTimeMillis();
            }

            boolean flag2 = this.target == null || !this.target.isAlive() || this.target.isRemoved() || System.currentTimeMillis() - this.lastTargetTime > 800L;
            this.visibilityAnimation.update();
            this.visibilityAnimation.animate(flag2 ? 0.0 : 1.0, 0.18, Easings.QUART_OUT);
            float f = this.visibilityAnimation.get();
            if (!(f <= 0.01F) && this.target != null) {
                float f2 = this.getHealth(this.target);
                float f3 = this.getMaxHealth(this.target, f2);
                float f4 = MathHelper.clamp(f2, 0.0F, f3);
                float f5 = MathHelper.clamp(this.target.getAbsorptionAmount(), 0.0F, f3);
                if (string != null) {
                    this.renderCustomStyle(drawContext, string, this.target, f4, f3, f5, f);
                } else if (flag) {
                    this.ringRenderer.render(drawContext, this.target, this.draggable.position2.x, this.draggable.position2.y, f4, f3, f5, f);
                } else {
                    this.hasAbsorption = f5 > 0.01F;
                    float f6 = f4 / f3;
                    float f7 = this.hasAbsorption ? f5 / f3 : 0.0F;
                    this.healthAnimation.update();
                    this.healthAnimation.animate(f6, 0.5, Easings.EXPO_OUT);
                    this.healthTrailAnimation.update();
                    this.healthTrailAnimation.animate(f6, 2.5, Easings.EXPO_OUT);
                    if (this.hasAbsorption) {
                        this.absorptionAnimation.update();
                        this.absorptionAnimation.animate(f7, 0.5, Easings.EXPO_OUT);
                        this.absorptionTrailAnimation.update();
                        this.absorptionTrailAnimation.animate(f7, 2.5, Easings.EXPO_OUT);
                        this.renderClassic(drawContext, this.target, f4, f);
                    } else {
                        this.absorptionAnimation.forceValue(0.0);
                        this.absorptionTrailAnimation.forceValue(0.0);
                        this.renderClassic(drawContext, this.target, f4, f);
                    }
                }
            } else {
                if (flag2 && f <= 0.01F) {
                    this.target = null;
                }

                this.hitParticleSystem.clear();
                this.eatParticleSystem.clear();
                if (flag) {
                    this.ringRenderer.renderParticles(drawContext);
                }

                if (string != null) {
                    this.renderCustomStyle(drawContext, string, null, 0.0F, 0.0F, 0.0F, 0.0F);
                }
            }
        }
    }

    private void renderCustomStyle(DrawContext drawContext, String string, LivingEntity livingEntity, float f, float f2, float f3, float f4) {
        String string2 = livingEntity == null ? "" : ClientSpoof.replaceNames(livingEntity.getName()).getString();
        ScriptTargetHudStyles.render(
            string,
            drawContext,
            new ScriptTargetHudStyles.TargetHudContext(
                livingEntity,
                string2,
                this.draggable.position2.x,
                this.draggable.position2.y,
                f,
                f2,
                f3,
                f4,
                this.showArmor.getValue(),
                this.hitParticles.getValue(),
                (f5, f6, f7) -> {
                    if (livingEntity != null) {
                        this.avatarOverlay.render(drawContext, livingEntity, f5, f6, f7);
                    }
                }
            )
        );
    }

    private void renderClassic(DrawContext drawContext, LivingEntity livingEntity, float f, float f2) {
        float f3 = this.draggable.position2.x;
        float f4 = this.draggable.position2.y;
        HudPanelUtil.drawPanel(drawContext, f3, f4, 100.0F, 34.0F, f2, Radius.smoothAll(16.0F));
        this.renderHitParticles(drawContext, f3, f4, f2);
        this.renderAvatar(drawContext, livingEntity, f3 + 5.0F, f4 + 5.0F, 24.0F, f2);
        this.avatarOverlay.render(drawContext, livingEntity, f3 + 28.2F, f4 + 28.2F, f2);
        this.renderEatParticles(drawContext, livingEntity, f3 + 5.0F, f4 + 5.0F, 24.0F, f2);
        boolean flag = this.shouldShowItems(livingEntity);
        this.itemsAnimation.update();
        this.itemsAnimation.animate(flag ? 1.0 : 0.0, 0.35, Easings.QUAD_OUT);
        float f5 = this.itemsAnimation.get();
        float f6 = f4 + 5.0F;
        float f7 = f6 + 6.0F + 3.0F;
        float f8 = f7 + 8.0F + 3.0F;
        float f9 = f4 + 9.0F;
        float f10 = f9 + 6.0F + 5.0F;
        float f11 = MathHelper.lerp(f5, f9, f6);
        float f12 = MathHelper.lerp(f5, f9 + 6.0F + 3.0F, f7);
        float f13 = MathHelper.lerp(f5, f10, f8);
        float f14 = f3 + 34.0F;
        float f15 = f11 + 3.0F;
        String string = formatHealth(f);
        float f16 = BODY_FONT.getWidth(string, 6.0F);
        float f17 = f3 + 100.0F - 5.0F - 7.0F;
        float f18 = f17 - 2.0F - f16;
        int i = ColorUtil.setAlphaf(
            ColorUtil.interpolate(LOW_HEALTH_COLOR, HIGH_HEALTH_COLOR, MathHelper.clamp(this.healthAnimation.get(), 0.0F, 1.0F)), f2
        );
        new RenderUtil(drawContext, f18, f11 + 0.0F, f16, 6.0F).textTopLeft(BODY_FONT, string, 0.0F, 0.0F, i, 6.0F);
        this.renderHeartIcon(drawContext, f17, f15, i);
        this.renderName(drawContext, livingEntity, f14, f11, f18 - f14 - 3.0F, f2);
        if (flag) {
            this.renderItems(drawContext, livingEntity, f14, f12, 61.0F, f2);
        }

        this.renderHealthBar(drawContext, f14, f13, 61.0F, f2);
    }

    private void renderHitParticles(DrawContext drawContext, float f, float f2, float f3) {
        if (!this.hitParticles.getValue()) {
            this.hitParticleSystem.clear();
        } else {
            this.hitParticleSystem.applyForce(f + 5.0F + 12.0F, f2 + 5.0F + 12.0F);
            this.hitParticleSystem.update();
            this.hitParticleSystem.render(drawContext, f3);
        }
    }

    @EventHandler
    public void onEntityDamage(AttackEntityEvent attackEntityEvent) {
        if (this.isEnabled() && attackEntityEvent.getStage() == AttackEntityEvent.Stage.POST) {
            if (this.target != null && attackEntityEvent.getTarget() == this.target) {
                if (ScriptTargetHudStyles.isAvailable(this.style.getValue())) {
                    ScriptTargetHudStyles.onHit(this.style.getValue(), this.target);
                } else if (this.style.is("ring")) {
                    this.ringRenderer.onHit();
                } else if (this.hitParticles.getValue()) {
                    this.hitParticleSystem
                        .spawn(
                            16,
                            () -> GuiParticle.builder()
                                .texture(Texture.BLOOM)
                                .color(Theme.getPrimary())
                                .size(MathUtil.randomBetween(1.5F, 3.0F))
                                .lifetime((long)MathUtil.randomBetween(900.0F, 1500.0F))
                                .friction(1.6F)
                                .randomVelocity(35.0F, 75.0F)
                                .build()
                        );
                }
            }
        }
    }

    private void renderHeartIcon(DrawContext drawContext, float f, float f2, int i) {
        new RenderUtil(drawContext, f, f2 - 3.5F, 7.0F, 7.0F).icon(Icon.HEART, 0.0F, 0.0F, 7.0F, i);
    }

    private void renderName(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3, float f4) {
        Text text = livingEntity.getName();
        this.targetName = text.getString();
        String string = ClientSpoof.replaceNames(text).getString();
        this.nameX = f;
        this.nameY = f2 - 1.0F;
        this.nameWidth = Math.max(0.0F, f3);
        this.nameHeight = 8.0F;
        boolean flag;
        if (this.isChatOrGuiOpen()
            && this.mouseX >= this.nameX
            && this.mouseX <= this.nameX + this.nameWidth
            && this.mouseY >= this.nameY
            && this.mouseY <= this.nameY + this.nameHeight) {
            flag = true;
        } else {
            flag = false;
            this.copyHoverTimer.reset();
        }

        boolean flag2;
        if (flag) {
            flag2 = this.copyHoverTimer.isElapsed(500L);
        } else {
            flag2 = false;
        }

        this.copyHoverAnimation.update();
        this.copyHoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.3, Easings.QUAD_OUT);
        this.copiedAnimation.update();
        this.copiedAnimation.animate(System.currentTimeMillis() < this.copiedUntil ? 1.0 : 0.0, 0.25, Easings.EXPO_OUT);
        float f5 = this.copyHoverAnimation.get();
        float f6 = this.copiedAnimation.get();
        float f7 = 9.5F * f5;
        float f8 = f2 + -0.75F;
        float f9 = f5 * (1.0F - f6) * f4;
        float f10 = f5 * f6 * f4;
        if (f9 > 0.001F) {
            new RenderUtil(drawContext, f, f8, 7.5F, 7.5F).icon(Icon.COPY, 0.0F, 0.0F, 7.5F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f9));
        }

        if (f10 > 0.001F) {
            new RenderUtil(drawContext, f, f8, 7.5F, 7.5F).icon(Icon.CHECK, 0.0F, 0.0F, 7.5F, ColorUtil.setAlphaf(COPIED_COLOR, f10));
        }

        float f11 = Math.max(0.0F, f3 - f7);
        new RenderUtil(drawContext, f + f7, f2 + 0.0F, f11, 6.0F)
            .scrollingTextTop(HEADER_FONT, string, 0.0F, 0.0F, f11, ColorUtil.setAlphaf(Theme.getTextColor(), f4), 6.0F);
    }

    private boolean isChatOrGuiOpen() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGuiScreen;
    }

    @EventHandler
    public void onMouseClick(ScreenKeyEvent screenKeyEvent) {
        if (this.isEnabled() && screenKeyEvent.isPress() && screenKeyEvent.isKey(0) && this.isChatOrGuiOpen() && this.style.is("classic")) {
            if (!this.targetName.isEmpty() && this.copyHoverTimer.isElapsed(500L)) {
                double d = screenKeyEvent.getMouseX();
                double d2 = screenKeyEvent.getMouseY();
                if (d >= this.nameX && d <= this.nameX + this.nameWidth && d2 >= this.nameY && d2 <= this.nameY + this.nameHeight) {
                    mc.keyboard.setClipboard(this.targetName);
                    this.copiedUntil = System.currentTimeMillis() + 1000L;
                }
            }
        }
    }

    private void renderHealthBar(DrawContext drawContext, float f, float f2, float f3, float f4) {
        Radius radius = Radius.all(2.5F);
        int i = Theme.getPrimary();
        int i2 = ColorUtil.darken(Theme.getPrimary(), 0.65F);
        int i3 = ColorUtil.multiplyAlpha(i, f4 * 0.35F);
        int i4 = ColorUtil.multiplyAlpha(i2, f4 * 0.35F);
        int i5 = ColorUtil.multiplyAlpha(i, f4 * 0.15F);
        int i6 = ColorUtil.multiplyAlpha(i2, f4 * 0.15F);
        float f5 = f3 * MathHelper.clamp(this.healthTrailAnimation.get(), 0.0F, 1.0F);
        if (f5 > 0.5F) {
            new RenderUtil(drawContext, f, f2, f5, 5.0F).rect(i3, i3, i4, i4, radius);
        }

        new RenderUtil(drawContext, f, f2, f3, 5.0F).rect(i5, i5, i6, i6, radius);
        int i7 = ColorUtil.multiplyAlpha(i, f4);
        int i8 = ColorUtil.multiplyAlpha(i2, f4);
        float f6 = f3 * MathHelper.clamp(this.healthAnimation.get(), 0.0F, 1.0F);
        if (f6 > 0.5F) {
            new RenderUtil(drawContext, f, f2, f6, 5.0F).rect(i7, i7, i8, i8, radius);
        }

        int i9 = ColorUtil.multiplyAlpha(ABSORPTION_COLOR_DARK, f4 * 0.9F);
        int i10 = ColorUtil.multiplyAlpha(ABSORPTION_COLOR, f4 * 0.9F);
        int i11 = ColorUtil.multiplyAlpha(ABSORPTION_COLOR_DARK, f4 * 0.7F);
        int i12 = ColorUtil.multiplyAlpha(ABSORPTION_COLOR, f4 * 0.7F);
        if (!ServerManager.isFunTime()) {
            float f7 = this.hasAbsorption ? f3 * MathHelper.clamp(this.absorptionTrailAnimation.get(), 0.0F, 1.0F) : 0.0F;
            if (f7 > 0.5F) {
                new RenderUtil(drawContext, f + f3 - f7, f2, f7, 5.0F).rect(i11, i11, i12, i12, radius);
            }

            float f8 = this.hasAbsorption ? f3 * MathHelper.clamp(this.absorptionAnimation.get(), 0.0F, 1.0F) : 0.0F;
            if (f8 > 0.5F) {
                new RenderUtil(drawContext, f + f3 - f8, f2, f8, 5.0F).rect(i9, i9, i10, i10, radius);
            }
        }
    }

    private boolean shouldShowItems(LivingEntity livingEntity) {
        return this.hasHandItems(livingEntity) || this.showArmor.getValue() && this.hasArmor(livingEntity);
    }

    private float getHealth(LivingEntity livingEntity) {
        return livingEntity instanceof PlayerEntity ? ServerManager.getHealth((PlayerEntity)livingEntity) : livingEntity.getHealth();
    }

    private float getMaxHealth(LivingEntity livingEntity, float f) {
        return Math.max(1.0F, Math.max(livingEntity.getMaxHealth(), f));
    }

    private boolean hasArmor(LivingEntity livingEntity) {
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            if (!livingEntity.getEquippedStack(equipmentSlot).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasHandItems(LivingEntity livingEntity) {
        return !livingEntity.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty() || !livingEntity.getEquippedStack(EquipmentSlot.OFFHAND).isEmpty();
    }

    private void renderItems(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3, float f4) {
        float f5 = MathUtil.roundToStep(f, 0.5F);
        float f6 = MathUtil.roundToStep(f2, 0.5F);
        if (this.showArmor.getValue()) {
            float f7 = f5;

            for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
                f7 = this.renderItemSlot(drawContext, livingEntity.getEquippedStack(equipmentSlot), f7, f6, f4);
            }
        }

        float f8 = this.getItemsWidth(2);
        float f9 = f5 + f3 - f8;
        float f10 = this.renderItemSlot(drawContext, livingEntity.getEquippedStack(EquipmentSlot.MAINHAND), f9, f6, f4);
        this.renderItemSlot(drawContext, livingEntity.getEquippedStack(EquipmentSlot.OFFHAND), f10, f6, f4);
    }

    private float getItemsWidth(int i) {
        return i <= 0 ? 0.0F : i * 8.0F + (i - 1) * 0.5F;
    }

    private float renderItemSlot(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3) {
        if (itemStack.isEmpty()) {
            this.renderEmptySlot(drawContext, f, f2, f3);
            return f + 8.0F + 0.5F;
        } else {
            drawItemStack(drawContext, itemStack, f, f2, 8.0F, f3);
            return f + 8.0F + 0.5F;
        }
    }

    public static void drawItemStack(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3, float f4) {
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f, f2, f3 / 16.0F);
        GuiRenderUtil.drawItem(drawContext, itemStack, f, f2, f4);
        if (itemStack.isItemBarVisible()) {
            int i = itemStack.getItemBarStep();
            int i2 = itemStack.getItemBarColor();
            MatrixUtil.push(drawContext);
            MatrixUtil.translate(drawContext, f, f2);
            new RenderUtil(drawContext, 2.0F, 13.0F, 13.0F, 2.0F).rect(ColorUtil.setAlphaf(ColorUtil.BLACK, f4));
            new RenderUtil(drawContext, 2.0F, 13.0F, i, 1.0F).rect(ColorUtil.setAlphaf(i2, f4));
            MatrixUtil.pop(drawContext);
            MatrixUtil.pop(drawContext);
        } else {
            if (itemStack.getCount() > 1) {
                String string = String.valueOf(itemStack.getCount());
                drawContext.drawText(
                    mc.textRenderer, string, (int)f + 17 - mc.textRenderer.getWidth(string), (int)f2 + 9, ColorUtil.setAlphaf(ColorUtil.WHITE, f4), true
                );
            }

            MatrixUtil.pop(drawContext);
        }
    }

    private void renderEmptySlot(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = (8.0F - 3.0F) / 2.0F;
        new RenderUtil(drawContext, f + f4, f2 + f4, 3.0F, 3.0F).rect(ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.45F), Radius.all(3.0F / 2.0F));
    }

    private void renderEatParticles(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3, float f4) {
        if (!this.eatParticles.getValue()) {
            this.eatParticleSystem.clear();
        } else {
            float f5 = f + f3 / 2.0F;
            float f6 = f2 + f3 * 0.82F;
            this.eatParticleSystem.applyForce(f5, f6);
            if (livingEntity.isUsingItem() && TargetUsingItemIndicator.isEatOrDrink(livingEntity.getActiveItem()) && this.eatParticleTimer.hasPassedAndReset(90.0)) {
                int i = ItemColorUtil.getAverageColor(livingEntity.getActiveItem());
                this.eatParticleSystem
                    .spawn(
                        2,
                        () -> GuiParticle.builder()
                            .texture(Texture.BLOOM)
                            .color(i)
                            .position(f5, f6)
                            .size(MathUtil.randomBetween(1.5F, 2.8F))
                            .lifetime((long)MathUtil.randomBetween(700.0F, 1200.0F))
                            .motionX(MathUtil.randomBetween(-28.0F, 28.0F))
                            .motionY(MathUtil.randomBetween(-12.0F, 22.0F))
                            .gravity(150.0F)
                            .friction(0.8F)
                            .build()
                    );
            }

            float f7 = this.draggable.position2.x;
            float f8 = this.draggable.position2.y;
            this.eatParticleSystem.updateInBounds(f7 + 2.0F, f8 + 2.0F, f7 + 100.0F - 2.0F, f8 + 34.0F - 2.0F);
            this.eatParticleSystem.render(drawContext, f4);
        }
    }

    private void renderAvatar(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3, float f4) {
        Radius radius = Radius.smoothAll(8.0F);
        new RenderUtil(drawContext, f, f2, f3, f3).outline(4.0F, ColorUtil.BLACK, f4 * 0.2F, radius);
        TextureSetup textureSetup = getSkinTextureSetup(livingEntity);
        if (textureSetup == null) {
            new RenderUtil(drawContext, f, f2, f3, f3)
                .rect(ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getBackground(), Theme.getWhite(), 0.15F), f4 * 0.6F), radius)
                .icon(Icon.USER, f3 * 0.25F, f3 * 0.25F, f3 * 0.5F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f4));
        } else {
            drawPlayerFace(drawContext, textureSetup, f, f2, f3, 4.0F, f4);
            float f5 = livingEntity.maxHurtTime > 0 ? (float)livingEntity.hurtTime / livingEntity.maxHurtTime : 0.0F;
            if (f5 > 0.0F) {
                new RenderUtil(drawContext, f, f2, f3, f3).rect(ColorUtil.setAlphaf(ColorUtil.RED, f4 * f5 * 0.55F), radius);
            }
        }
    }

    static Identifier getSkinTexture(LivingEntity livingEntity) {
        return livingEntity instanceof AbstractClientPlayerEntity ? ((AbstractClientPlayerEntity)livingEntity).getSkin().body().texturePath() : null;
    }

    static void drawPlayerFace(DrawContext drawContext, TextureSetup textureSetup, float f, float f2, float f3, float f4, float f5) {
        drawPlayerHead(drawContext, textureSetup, f, f2, f3, Radius.all(f4), f5);
    }

    public static void drawPlayerHead(DrawContext drawContext, TextureSetup textureSetup, float f, float f2, float f3, Radius radius, float f4) {
        Matrix3x2f matrix3x2f = new Matrix3x2f(drawContext.getMatrices());
        TexturedShapeRenderer.drawOutline(drawContext, textureSetup, matrix3x2f, f, f2, f3, f3, 0.125F, 0.125F, 0.25F, 0.25F, radius, f4, ColorUtil.WHITE);
        TexturedShapeRenderer.drawOutline(drawContext, textureSetup, matrix3x2f, f, f2, f3, f3, 0.625F, 0.125F, 0.75F, 0.25F, radius, f4, ColorUtil.WHITE);
    }

    public static TextureSetup getSkinTextureSetup(LivingEntity livingEntity) {
        Identifier identifier = getSkinTexture(livingEntity);
        if (identifier == null) {
            return null;
        }

        AbstractTexture abstractTexture = mc.getTextureManager().getTexture(identifier);
        if (abstractTexture == null) {
            return null;
        }

        GpuTextureView gpuTextureView = abstractTexture.getGlTextureView();
        return gpuTextureView != null && !gpuTextureView.isClosed() ? TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST)) : null;
    }

    private LivingEntity findTarget() {
        if (mc.player == null || !(mc.currentScreen instanceof ClickGuiScreen) && !(mc.currentScreen instanceof ChatScreen)) {
            AttackAura attackAura = AttackAura.getInstance();
            if (attackAura != null && attackAura.isEnabled() && attackAura.getCurrentTarget() != null) {
                return attackAura.getCurrentTarget();
            }

            AutoCrystal autoCrystal = AutoCrystal.getInstance();
            if (autoCrystal != null) {
                LivingEntity livingEntity = autoCrystal.getActiveTarget();
                if (livingEntity != null) {
                    return livingEntity;
                }
            }

            LivingEntity livingEntity2 = ScriptAuraHooks.getTarget();
            if (livingEntity2 != null) {
                return livingEntity2;
            } else {
                return this.showOnLook.getValue() && mc.targetedEntity instanceof LivingEntity livingEntity3 ? livingEntity3 : null;
            }
        } else {
            return mc.player;
        }
    }

    static String formatHealth(float f) {
        int i = (int)f;
        int i2 = Math.round((f - i) * 10.0F);
        if (i2 >= 10) {
            return Integer.toString(i + 1);
        } else {
            return i2 <= 0 ? Integer.toString(i) : i + "." + i2;
        }
    }

    public static TargetHud getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(TargetHud.class);
    }
}
