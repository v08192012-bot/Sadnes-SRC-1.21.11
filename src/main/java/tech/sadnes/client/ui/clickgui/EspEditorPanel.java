package tech.sadnes.client.ui.clickgui;

import com.google.common.collect.HashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.feature.impl.render.esp.BoxElement;
import tech.sadnes.client.feature.impl.render.esp.ChamsElement;
import tech.sadnes.client.feature.impl.render.esp.EspAnchor;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.feature.impl.render.esp.GlowElement;
import tech.sadnes.client.figura.FiguraAvatarManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.EspCategoryTab;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class EspEditorPanel implements IRenderable {
    private static final float PADDING = 4.0F;
    private static final float CATEGORY_BAR_HEIGHT = 14.0F;
    private static final double DRAG_THRESHOLD = 3.0;
    private static final float DRAG_MARGIN = 8.0F;
    private static final float TAB_SPACING = 12.0F;
    private static final float SNAP_DISTANCE = 4.5F;
    private static final float HIGHLIGHT_PADDING = 2.0F;
    private static final float MENU_PADDING = 6.0F;
    private static final float MENU_TOP_PADDING = 3.0F;
    private static final float MENU_ROW_HEIGHT = 14.0F;
    private static final float MENU_FONT_SIZE = 5.5F;
    private static final float MENU_ICON_SIZE = 7.0F;
    private static final float ROW_BUTTON_WIDTH = 12.0F;
    private static final float ROW_BUTTON_ICON_SIZE = 7.0F;
    private static final float ROW_ICON_AREA = 12.0F;
    private static final float CHEVRON_SIZE = 7.0F;
    private static final float ROW_TEXT_OFFSET = 14.0F;
    private final ClickGuiScreen clickGui;
    private final EspCategoryTab[] categoryTabs;
    private EspTargetType currentCategory = EspTargetType.FRIENDS;
    private EspTargetType previousCategory;
    private final Animation tabSlideAnimation = new Animation();
    private final Animation categorySwitchAnimation = new Animation();
    private final Animation leftArrowHoverAnimation = new Animation();
    private final Animation rightArrowHoverAnimation = new Animation();
    private int switchDirection;
    private float previewYaw;
    private float previewPitch;
    private final Map<String, Animation> resetHoverAnimations = new HashMap<>();
    private final Map<String, Animation> settingsHoverAnimations = new HashMap<>();
    private final Map<String, Animation> rowHoverAnimations = new HashMap<>();
    private final Animation hintAnimation = new Animation();
    private final Animation elementHoverAnimation = new Animation();
    private EspElement hoveredElement;
    private PigEntity previewAnimal;
    private ZombieEntity previewMob;
    private ItemEntity previewItem;
    private final Map<EspTargetType, OtherClientPlayerEntity> previewPlayers = new EnumMap<>(EspTargetType.class);
    private boolean pressPending;
    private double pressX;
    private double pressY;
    private double dragDistance;
    private boolean rotating;
    private EspElement pressedElement;
    private EspElement draggedElement;
    private double dragMouseX;
    private double dragMouseY;
    private double dragOffsetX;
    private double dragOffsetY;
    private float snapGuideX = Float.NaN;
    private float snapGuideY = Float.NaN;
    private EspElement snapTarget;
    private EspElement lastDraggedElement;
    private final Animation dragHighlightAnimation = new Animation();
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float previewX;
    private float previewY;
    private float previewWidth;
    private float previewHeight;
    private float categoryBarX;
    private float categoryBarY;
    private float categoryBarWidth;
    private float categoryBarHeight;
    private float entityBoxX;
    private float entityBoxY;
    private float entityBoxWidth;
    private float entityBoxHeight;
    private boolean menuOpen;
    private final Animation menuAnimation = new Animation();
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;

    public EspEditorPanel(ClickGuiScreen clickGuiScreen) {
        this.clickGui = clickGuiScreen;
        EspTargetType[] espTargetTypes = EspTargetType.values();
        this.categoryTabs = new EspCategoryTab[espTargetTypes.length];

        for (int i = 0; i < espTargetTypes.length; i++) {
            this.categoryTabs[i] = new EspCategoryTab(espTargetTypes[i]);
        }
    }

    public void setCategory(EspTargetType espTargetType) {
        if (espTargetType != this.currentCategory) {
            EspEditorPanel espEditorPanel;
            byte b;
            if (this.currentCategory.next() == espTargetType) {
                espEditorPanel = this;
                b = 1;
            } else {
                espEditorPanel = this;
                b = -1;
            }

            espEditorPanel.switchDirection = b;
            this.tabSlideAnimation.forceValue(1.0);
            this.tabSlideAnimation.animate(0.0, 0.3, Easings.QUART_OUT);
            this.previousCategory = this.currentCategory;
            this.categorySwitchAnimation.forceValue(0.0);
            this.categorySwitchAnimation.animate(1.0, 0.4, Easings.LINEAR);
            this.currentCategory = espTargetType;
            this.closeMenu();
        }
    }

    private List<EspElement> getCurrentElements() {
        Esp esp = Esp.getInstance();
        return esp == null ? List.of() : esp.getElements(this.currentCategory);
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        this.panelX = f;
        this.panelY = f2;
        this.panelWidth = f3;
        this.panelHeight = f4;
        this.menuAnimation.update();
        this.dragHighlightAnimation.update();
        boolean flag = this.draggedElement != null;
        if (flag) {
            this.lastDraggedElement = this.draggedElement;
        }

        this.dragHighlightAnimation.animate(flag ? 1.0 : 0.0, flag ? 0.18 : 0.22, Easings.QUAD_OUT);
        if (!flag && this.dragHighlightAnimation.get() < 0.01F) {
            this.lastDraggedElement = null;
        }

        float f6 = (float)Math.pow(this.clickGui.getOpenAnimation().get(), 3.0);
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, f4, f6 * 0.75F, 8.0F);
        this.categoryBarX = f + 4.0F;
        this.categoryBarWidth = f3 - 8.0F;
        this.categoryBarHeight = 14.0F;
        this.categoryBarY = f2 + f4 - 4.0F - this.categoryBarHeight;
        this.previewX = f + 4.0F;
        this.previewY = f2 + 4.0F;
        this.previewWidth = f3 - 8.0F;
        this.previewHeight = this.categoryBarY - this.previewY - 4.0F;
        this.renderPreview(drawContext, f6, i, i2, f5);
        this.renderCategoryBar(drawContext, f6, i, i2, f5);
        float f7 = this.menuAnimation.get();
        if (f7 > 0.001F) {
            float f8 = 0.85F + 0.15F * f7;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.menuX + this.menuWidth / 2.0F, this.menuY + this.menuHeight / 2.0F, f8);
            this.renderMenu(drawContext, f6 * Math.min(f7, 1.0F), i, i2);
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderPreview(DrawContext drawContext, float f, int i, int i2, float f2) {
        this.categorySwitchAnimation.update();
        if (!(this.previewHeight <= 0.0F) && !(this.previewWidth <= 0.0F)) {
            boolean flag = ScissorStack.push(drawContext, this.previewX, this.previewY, this.previewWidth, this.previewHeight);
            if (flag) {
                try {
                    EspTargetType espTargetType = this.currentCategory;
                    float f3 = 0.0F;
                    Easing easing = Easings.QUART_OUT;
                    float f4 = this.previewWidth;
                    float f5 = 0.0F;
                    if (this.categorySwitchAnimation.isAnimating() && this.previousCategory != null) {
                        float f6 = this.categorySwitchAnimation.get();
                        if (f6 < 0.5F) {
                            espTargetType = this.previousCategory;
                            f5 = -((float)easing.ease(f6 * 2.0F)) * f4 * this.switchDirection;
                        } else {
                            f5 = (1.0F - (float)easing.ease((f6 - 0.5F) * 2.0F)) * f4 * this.switchDirection;
                        }
                    } else if (this.previousCategory != null) {
                        this.previousCategory = null;
                    }

                    if (espTargetType == EspTargetType.ITEM) {
                        f3 = 0.0F + this.previewHeight * 0.11F;
                    }

                    Entity entity = this.getPreviewEntity(espTargetType);
                    if (entity != null) {
                        float f7 = espTargetType == EspTargetType.ITEM ? 2.5F : 1.0F;
                        float f8 = Math.max(10.0F, this.previewHeight * 0.3F * f7);
                        float f9 = espTargetType == EspTargetType.INVISIBLE ? f * 0.4F : f;
                        BoxElement boxElement = this.findActive3dBox(espTargetType);
                        Consumer consumer = boxElement != null && espTargetType != EspTargetType.ITEM ? this.createBox3dRenderer(boxElement, entity, f) : null;
                        GlowElement glowElement = this.findActiveGlow(espTargetType);
                        int i3 = glowElement != null ? glowElement.getColor() : 0;
                        float f10 = glowElement != null ? glowElement.radius.getValue() : 0.0F;
                        ChamsElement chamsElement = this.findActiveChams(espTargetType);
                        int i4 = chamsElement != null ? chamsElement.getColor() : 0;
                        int i5 = chamsElement != null ? chamsElement.getModeId() : 0;
                        float f11 = chamsElement != null ? chamsElement.speed.getValue() : 1.0F;
                        float f12 = chamsElement != null ? chamsElement.scale.getValue() : 1.0F;
                        boolean flag2 = chamsElement != null && !chamsElement.model.getValue();
                        int i6 = (int)(this.previewX + f5);
                        int i7 = (int)this.previewY;
                        int i8 = (int)(this.previewX + this.previewWidth + f5);
                        int i9 = (int)(this.previewY + this.previewHeight);
                        GuiRenderUtil.drawEntity(
                            drawContext,
                            i6,
                            i7,
                            i8,
                            i9,
                            f8,
                            this.previewYaw,
                            this.previewPitch,
                            entity,
                            f9,
                            f3,
                            consumer,
                            i3,
                            f10,
                            i4,
                            i5,
                            f11,
                            f12,
                            flag2
                        );
                        float f13 = this.previewX + this.previewWidth / 2.0F + f5;
                        float f14 = (i7 + i9) / 2.0F + f3;
                        if (espTargetType == EspTargetType.ITEM) {
                            this.entityBoxWidth = f8 * (0.55F + 2.0F * 0.1F);
                            this.entityBoxHeight = f8 * (0.55F + 2.0F * 0.1F);
                            this.entityBoxX = f13 - this.entityBoxWidth / 2.0F;
                            this.entityBoxY = f14 - this.entityBoxHeight / 2.0F;
                        } else {
                            float f15 = (float)Math.toRadians(this.previewPitch);
                            float f16 = Math.abs((float)Math.cos(f15));
                            float f17 = Math.abs((float)Math.sin(f15));
                            float f18 = Math.max(0.325F, entity.getWidth() / 2.0F * 1.05F) + 0.1F;
                            float f19 = Math.max(0.5F, entity.getHeight() / 2.0F) + 0.1F;
                            this.entityBoxWidth = 2.0F * f8 * f18;
                            this.entityBoxHeight = 2.0F * f8 * (f19 * f16 + f18 * f17);
                            this.entityBoxX = f13 - this.entityBoxWidth / 2.0F;
                            this.entityBoxY = f14 - this.entityBoxHeight / 2.0F;
                        }

                        float f20 = this.entityBoxY;
                        Esp esp = Esp.getInstance();
                        if (esp != null) {
                            List list = esp.getElements(espTargetType);
                            int i10 = list.size();
                            boolean[] booleans = new boolean[i10];

                            for (int i11 = 0; i11 < i10; i11++) {
                                EspElement espElement = (EspElement)list.get(i11);
                                espElement.invalidateLayout();
                                boolean flag3 = espElement.isVisible();
                                if (flag3 && espElement instanceof BoxElement && ((BoxElement)espElement).mode.is("3D")) {
                                    flag3 = false;
                                }

                                booleans[i11] = flag3;
                            }

                            for (int i12 = 0; i12 < i10; i12++) {
                                if (booleans[i12]) {
                                    ((EspElement)list.get(i12)).measure(entity, true);
                                }
                            }

                            for (int i13 = 0; i13 < i10; i13++) {
                                if (booleans[i13]) {
                                    ((EspElement)list.get(i13))
                                        .layout(this.entityBoxX, this.entityBoxY, this.entityBoxWidth, this.entityBoxHeight, false, list);
                                }
                            }

                            for (int i14 = 0; i14 < i10; i14++) {
                                if (booleans[i14]) {
                                    ((EspElement)list.get(i14))
                                        .renderForEntity(
                                            drawContext,
                                            entity,
                                            f13,
                                            f20,
                                            this.entityBoxX,
                                            this.entityBoxY,
                                            this.entityBoxWidth,
                                            this.entityBoxHeight,
                                            i,
                                            i2,
                                            f2,
                                            f
                                        );
                                }
                            }
                        }

                        if (this.lastDraggedElement != null && this.dragHighlightAnimation.get() > 0.01F && espTargetType == this.currentCategory) {
                            this.renderDragHighlight(drawContext, this.lastDraggedElement, f * this.dragHighlightAnimation.get());
                        }

                        this.renderHoverHighlight(drawContext, f, i, i2, espTargetType);
                        this.renderHint(drawContext, f, i, i2);
                        return;
                    }

                    this.entityBoxWidth = this.entityBoxHeight = 0.0F;
                } finally {
                    ScissorStack.pop(drawContext);
                }
            }
        }
    }

    private void renderHoverHighlight(DrawContext drawContext, float f, int i, int i2, EspTargetType espTargetType) {
        EspElement espElement = !this.menuOpen && this.draggedElement == null && espTargetType == this.currentCategory ? this.getElementAt(i, i2) : null;
        if (espElement != null) {
            this.hoveredElement = espElement;
        }

        this.elementHoverAnimation.update();
        this.elementHoverAnimation.animate(espElement != null ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f2 = this.elementHoverAnimation.get();
        if (!(f2 <= 0.01F) && this.hoveredElement != null && !(this.hoveredElement.size2.x <= 0.0F)) {
            if (!(this.hoveredElement.size2.y <= 0.0F)) {
                int i3 = Theme.getPrimary();
                new RenderUtil(
                        drawContext,
                        this.hoveredElement.position2.x - 2.0F,
                        this.hoveredElement.position2.y - 2.0F,
                        this.hoveredElement.size2.x + 2.0F * 2.0F,
                        this.hoveredElement.size2.y + 2.0F * 2.0F
                    )
                    .shadow(0.6F, ColorUtil.multiplyAlpha(i3, 0.55F * f2 * f), Radius.all(3.0F));
            }
        }
    }

    private void renderHint(DrawContext drawContext, float f, int i, int i2) {
        this.hintAnimation.update();
        boolean flag;
        if (!this.menuOpen && this.draggedElement == null) {
            flag = this.isHovered(i, i2, this.previewX, this.previewY, this.previewWidth, this.previewHeight);
        } else {
            flag = false;
        }

        this.hintAnimation.animate(flag ? 1.0 : 0.0, flag ? 0.2 : 0.15, Easings.QUAD_OUT);
        float f2 = this.hintAnimation.get();
        if (!(f2 <= 0.001F)) {
            int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.55F), f * f2);
            new RenderUtil(drawContext, this.previewX, this.previewY + this.previewHeight - 10.0F, this.previewWidth, 8.0F)
                .textCenter(Fonts.INTER_MEDIUM, Language.translate("esp.preview.hint", "ПКМ — элементы · ЛКМ — перетащить"), 0.0F, 0.0F, i3, 5.0F);
        }
    }

    private BoxElement findActive3dBox(EspTargetType espTargetType) {
        Esp esp = Esp.getInstance();
        if (esp == null) {
            return null;
        }

        for (EspElement espElement : esp.getElements(espTargetType)) {
            if (espElement instanceof BoxElement boxElement && boxElement.isActive() && boxElement.mode.is("3D")) {
                return boxElement;
            }
        }

        return null;
    }

    private GlowElement findActiveGlow(EspTargetType espTargetType) {
        Esp esp = Esp.getInstance();
        if (esp == null) {
            return null;
        }

        for (EspElement espElement : esp.getElements(espTargetType)) {
            if (espElement instanceof GlowElement glowElement && glowElement.isActive()) {
                return glowElement;
            }
        }

        return null;
    }

    private ChamsElement findActiveChams(EspTargetType espTargetType) {
        Esp esp = Esp.getInstance();
        if (esp == null) {
            return null;
        }

        for (EspElement espElement : esp.getElements(espTargetType)) {
            if (espElement instanceof ChamsElement chamsElement && chamsElement.isActive()) {
                return chamsElement;
            }
        }

        return null;
    }

    private Consumer<MatrixStack> createBox3dRenderer(BoxElement boxElement, Entity entity, float f) {
        boolean flag = boxElement.render.isSelected("Lines");
        boolean flag2 = boxElement.render.isSelected("Fill");
        if (!flag && !flag2) {
            return null;
        }

        float f2 = entity.getWidth() / 2.0F;
        float f3 = entity.getHeight();
        int i;
        if (boxElement.colorMode.is("Client")) {
            int i2 = Theme.getPrimary();
            i = ColorUtil.fade(10, 0, i2, ColorUtil.darken(i2, 0.25F));
        } else {
            i = boxElement.color.getValue();
        }

        int i3 = ColorUtil.multiplyAlpha(i, f);
        int i4 = ColorUtil.darken(ColorUtil.setAlphaf(i3, 0.15F), 0.5F);
        boolean flag3 = entity instanceof LivingEntity;
        float f4 = this.previewYaw;
        return matrixStack -> {
            if (flag3) {
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-f4));
            }

            Render3DUtil.drawBox(matrixStack, -f2, 0.0F, -f2, f2, f3, f2, i3, i4, flag, flag2);
        };
    }

    private Entity getPreviewEntity(EspTargetType espTargetType) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world == null) {
            return null;
        }

        switch (espTargetType) {
            case NAKED:
            case INVISIBLE:
            case FRIENDS:
            case OTHERS:
            case SELF:
                return this.getPreviewPlayer(minecraftClient, espTargetType);
            case ANIMAL:
                if (this.previewAnimal == null) {
                    this.previewAnimal = new PigEntity(EntityType.PIG, minecraftClient.world);
                }

                return this.previewAnimal;
            case MOB:
                if (this.previewMob == null) {
                    this.previewMob = new ZombieEntity(EntityType.ZOMBIE, minecraftClient.world);
                }

                return this.previewMob;
            case ITEM:
                if (this.previewItem == null) {
                    this.previewItem = new ItemEntity(minecraftClient.world, 0.0, 0.0, 0.0, new ItemStack(Items.TOTEM_OF_UNDYING));
                }

                return this.previewItem;
            default:
                throw new MatchException(null, null);
        }
    }

    private Entity getPreviewPlayer(MinecraftClient minecraftClient, EspTargetType espTargetType) {
        UUID uuid = FiguraAvatarManager.getCategoryPreviewUuid(espTargetType);
        UUID uuid2 = uuid != null ? uuid : getPreviewUuid(espTargetType);
        OtherClientPlayerEntity otherClientPlayerEntity = this.previewPlayers.get(espTargetType);
        if (otherClientPlayerEntity != null && otherClientPlayerEntity.getUuid().equals(uuid2) && otherClientPlayerEntity.getEntityWorld() == minecraftClient.world) {
            return otherClientPlayerEntity;
        }

        OtherClientPlayerEntity otherClientPlayerEntity2 = new OtherClientPlayerEntity(minecraftClient.world, new GameProfile(uuid2, getPreviewName(espTargetType)));
        if (espTargetType != EspTargetType.NAKED && espTargetType != EspTargetType.INVISIBLE) {
            this.equipPreviewGear(otherClientPlayerEntity2);
        }

        this.previewPlayers.put(espTargetType, otherClientPlayerEntity2);
        return otherClientPlayerEntity2;
    }

    private static UUID getPreviewUuid(EspTargetType espTargetType) {
        return UUID.nameUUIDFromBytes(("sadnes:esp-preview:" + espTargetType.name()).getBytes(StandardCharsets.UTF_8));
    }

    private static String getPreviewName(EspTargetType espTargetType) {
        switch (espTargetType) {
            case NAKED:
                return "SadnesNaked";
            case INVISIBLE:
                return "SadnesInvisible";
            case FRIENDS:
                return "SadnesFriend";
            case OTHERS:
            default:
                return "SadnesUser";
            case SELF:
                return "SadnesSelf";
        }
    }

    private void equipPreviewGear(OtherClientPlayerEntity otherClientPlayerEntity) {
        ItemStack itemStack = new ItemStack(Items.NETHERITE_HELMET);
        itemStack.setDamage(itemStack.getMaxDamage() / 4);
        otherClientPlayerEntity.equipStack(EquipmentSlot.HEAD, itemStack);
        ItemStack itemStack2 = new ItemStack(Items.NETHERITE_CHESTPLATE);
        itemStack2.setDamage(itemStack2.getMaxDamage() / 2);
        otherClientPlayerEntity.equipStack(EquipmentSlot.CHEST, itemStack2);
        ItemStack itemStack3 = new ItemStack(Items.NETHERITE_LEGGINGS);
        itemStack3.setDamage(itemStack3.getMaxDamage() * 3 / 4);
        otherClientPlayerEntity.equipStack(EquipmentSlot.LEGS, itemStack3);
        ItemStack itemStack4 = new ItemStack(Items.NETHERITE_BOOTS);
        itemStack4.setDamage(itemStack4.getMaxDamage() / 3);
        otherClientPlayerEntity.equipStack(EquipmentSlot.FEET, itemStack4);
        ItemStack itemStack5 = new ItemStack(Items.NETHERITE_SWORD);
        itemStack5.setDamage(itemStack5.getMaxDamage() / 5);
        otherClientPlayerEntity.equipStack(EquipmentSlot.MAINHAND, itemStack5);
        ItemStack itemStack6 = new ItemStack(Items.PLAYER_HEAD);
        HashMultimap hashMultimap = HashMultimap.create();
        hashMultimap.put(
            "textures",
            new Property(
                "textures",
                "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJTcGhlcmVBdGhlbmEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTNmOWVlZGEzYmEyM2ZlMTQyM2M0MDM2ZTdkZDBhNzQ0NjFkZmY5NmJhZGM1YjJmMmI5ZmFhN2NjMTZmMzgyZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                ""
            )
        );
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "", new PropertyMap(hashMultimap));
        itemStack6.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(gameProfile));
        otherClientPlayerEntity.equipStack(EquipmentSlot.OFFHAND, itemStack6);
    }

    private void renderCategoryBar(DrawContext drawContext, float f, int i, int i2, float f2) {
        this.tabSlideAnimation.update();
        this.leftArrowHoverAnimation.update();
        this.rightArrowHoverAnimation.update();
        GuiRenderUtil.drawOutlined(drawContext, this.categoryBarX, this.categoryBarY, this.categoryBarWidth, this.categoryBarHeight, f * 0.5F, 5.0F);
        boolean flag = this.isHovered(i, i2, this.categoryBarX, this.categoryBarY, 4.0F + 7.0F + 2.0F, this.categoryBarHeight);
        boolean flag2 = this.isHovered(
            i, i2, this.categoryBarX + this.categoryBarWidth - 4.0F - 7.0F - 2.0F, this.categoryBarY, 4.0F + 7.0F + 2.0F, this.categoryBarHeight
        );
        this.leftArrowHoverAnimation.animate(flag ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        this.rightArrowHoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f3 = 0.4F + 0.5F * this.leftArrowHoverAnimation.get();
        float f4 = 0.4F + 0.5F * this.rightArrowHoverAnimation.get();
        int i3 = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f * f3);
        int i4 = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f * f4);
        float f5 = this.categoryBarY + (this.categoryBarHeight - 7.0F) / 2.0F;
        RenderUtil.drawChevron(drawContext, this.categoryBarX + 4.0F, f5, 7.0F, 180.0F, i3);
        RenderUtil.drawChevron(drawContext, this.categoryBarX + this.categoryBarWidth - 4.0F - 7.0F, f5, 7.0F, 0.0F, i4);
        float f6 = 4.0F + 7.0F + 2.0F;
        float f7 = this.categoryBarWidth - 4.0F - 7.0F - 2.0F;
        float f8 = f7 - f6;
        EspTargetType espTargetType = this.currentCategory.previous();
        EspTargetType espTargetType2 = this.currentCategory.next();
        EspCategoryTab espCategoryTab = this.categoryTabs[this.currentCategory.ordinal()];
        EspCategoryTab espCategoryTab2 = this.categoryTabs[espTargetType.ordinal()];
        EspCategoryTab espCategoryTab3 = this.categoryTabs[espTargetType2.ordinal()];
        EspCategoryTab espCategoryTab4 = this.switchDirection >= 0 ? espCategoryTab2 : espCategoryTab3;
        float f9 = espCategoryTab.getLabelWidth() / 2.0F + 12.0F + espCategoryTab4.getLabelWidth() / 2.0F;
        float f10 = this.categoryBarX + f6 + f8 / 2.0F + this.tabSlideAnimation.get() * this.switchDirection * f9;
        new RenderUtil(drawContext, this.categoryBarX + f6, this.categoryBarY, f8, this.categoryBarHeight).scissor(() -> {
            float f11 = espCategoryTab.getLabelWidth();
            float f12 = f10 - f11 / 2.0F;
            this.renderTab(drawContext, espCategoryTab, f12, this.categoryBarY, f11, this.categoryBarHeight, i, i2, f2);
            float f13 = this.renderTabLeftOf(drawContext, espCategoryTab2, f12, this.categoryBarY, this.categoryBarHeight, i, i2, f2);
            float f14 = this.renderTabRightOf(drawContext, espCategoryTab3, f12 + f11, this.categoryBarY, this.categoryBarHeight, i, i2, f2);
            if (this.tabSlideAnimation.isAnimating()) {
                this.renderTabLeftOf(drawContext, this.categoryTabs[espTargetType.previous().ordinal()], f13, this.categoryBarY, this.categoryBarHeight, i, i2, f2);
                this.renderTabRightOf(drawContext, this.categoryTabs[espTargetType2.next().ordinal()], f14, this.categoryBarY, this.categoryBarHeight, i, i2, f2);
            }
        });
    }

    private float renderTabLeftOf(DrawContext drawContext, EspCategoryTab espCategoryTab, float f, float f2, float f3, int i, int i2, float f4) {
        float f5 = espCategoryTab.getLabelWidth();
        float f6 = f - 12.0F - f5;
        this.renderTab(drawContext, espCategoryTab, f6, f2, f5, f3, i, i2, f4);
        return f6;
    }

    private float renderTabRightOf(DrawContext drawContext, EspCategoryTab espCategoryTab, float f, float f2, float f3, int i, int i2, float f4) {
        float f5 = espCategoryTab.getLabelWidth();
        float f6 = f + 12.0F;
        this.renderTab(drawContext, espCategoryTab, f6, f2, f5, f3, i, i2, f4);
        return f6 + f5;
    }

    private void renderTab(DrawContext drawContext, EspCategoryTab espCategoryTab, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        espCategoryTab.position.set(f, f2);
        espCategoryTab.size.set(f3, f4);
        espCategoryTab.render(drawContext, i, i2, f5);
    }

    private String getElementName(EspElement espElement) {
        String string = espElement.getDisplayName();
        return Language.translate("esp.element." + string.toLowerCase(Locale.ROOT).replace(" ", ""), string);
    }

    private float computeMenuWidth(List<EspElement> espElements) {
        float f = 0.0F;

        for (EspElement espElement : espElements) {
            float f2 = Fonts.INTER_MEDIUM.getWidth(this.getElementName(espElement), 5.5F);
            if (f2 > f) {
                f = f2;
            }
        }

        return 20.0F + f + 12.0F + 12.0F + 6.0F;
    }

    private void openMenu(double d, double d2) {
        List<EspElement> espElements = this.getCurrentElements();
        if (!espElements.isEmpty()) {
            this.menuWidth = this.computeMenuWidth(espElements);
            this.menuHeight = 6.0F + espElements.size() * 14.0F;
            this.menuX = this.clampMenuX((float)d, this.menuWidth);
            this.menuY = this.clampMenuY((float)d2, this.menuHeight);
            if (!this.menuOpen) {
                this.menuOpen = true;
                this.clickGui.closeSettingsPopup();
                this.menuAnimation.animate(1.0, 0.25, Easings.BACK_OUT);
            }
        }
    }

    public void closeMenu() {
        if (this.menuOpen) {
            this.menuOpen = false;
            this.menuAnimation.animate(0.0, 0.18, Easings.QUAD_IN);
        }
    }

    private float clampMenuX(float f, float f2) {
        float f3 = this.panelX + 2.0F;
        float f4 = this.panelX + this.panelWidth - f2 - 2.0F;
        if (f4 < f3) {
            f4 = f3;
        }

        return Math.max(f3, Math.min(f, f4));
    }

    private float clampMenuY(float f, float f2) {
        float f3 = this.panelY + 2.0F;
        float f4 = this.panelY + this.panelHeight - f2 - 2.0F;
        if (f4 < f3) {
            f4 = f3;
        }

        return Math.max(f3, Math.min(f, f4));
    }

    private void renderMenu(DrawContext drawContext, float f, int i, int i2) {
        List<EspElement> espElements = this.getCurrentElements();
        if (!espElements.isEmpty()) {
            GuiRenderUtil.drawPopup(drawContext, this.menuX, this.menuY, this.menuWidth, this.menuHeight, f, 10.0F);
            float f2 = this.menuY + 3.0F;

            for (EspElement espElement : espElements) {
                this.renderMenuRow(drawContext, espElement, this.menuX + 6.0F, f2, this.menuWidth - 12.0F, 14.0F, f, i, i2);
                f2 += 14.0F;
            }
        }
    }

    private void renderMenuRow(DrawContext drawContext, EspElement espElement, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        boolean flag = espElement.enabled.getValue();
        espElement.enabled.valueAnimation.update();
        espElement.enabled.valueAnimation.animate(flag ? 1.0 : 0.0, 0.24, Easings.QUART_OUT);
        float f6 = espElement.enabled.valueAnimation.get();
        boolean flag2 = espElement.isResizable();
        boolean flag3 = this.isHovered(i, i2, f + f3 - 12.0F, f2, 12.0, f4);
        boolean flag4;
        if (flag2) {
            flag4 = this.isHovered(i, i2, f + f3 - 12.0F - 12.0F, f2, 12.0, f4);
        } else {
            flag4 = false;
        }

        boolean flag5 = this.isHovered(i, i2, f, f2, f3, f4);
        Animation animation = this.rowHoverAnimations.computeIfAbsent(espElement.getDisplayName(), string -> new Animation());
        animation.update();
        animation.animate(flag5 && !flag3 && !flag4 ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
        GuiRenderUtil.drawHighlight(drawContext, f, f2 + 1.0F, f3, f4 - 2.0F, animation.get() * f5, Radius.all(4.0F));
        int i3 = ColorUtil.setAlphaf(Theme.getTextColor(), (0.5F + 0.5F * f6) * f5);
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
        renderUtil.icon(
            getElementIcon(espElement), 2.0F, f4 / 2.0F - 3.5F, 7.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.35F + 0.55F * f6), f5)
        );
        float f7 = f3 - 14.0F - 12.0F - (flag2 ? 12.0F : 0.0F);
        renderUtil.scrollingTextMiddle(Fonts.INTER_MEDIUM, this.getElementName(espElement), 14.0F, 0.0F, f7, i3, 5.5F, flag5);
        if (flag2) {
            this.renderRowButton(
                drawContext,
                Icon.REFRESH,
                7.0F,
                this.resetHoverAnimations.computeIfAbsent(espElement.getDisplayName(), string2 -> new Animation()),
                f + f3 - 12.0F - 12.0F,
                f2,
                12.0F,
                f4,
                f5,
                i,
                i2
            );
        }

        this.renderRowButton(
            drawContext,
            Icon.SETTINGS,
            7.0F,
            this.settingsHoverAnimations.computeIfAbsent(espElement.getDisplayName(), string3 -> new Animation()),
            f + f3 - 12.0F,
            f2,
            12.0F,
            f4,
            f5,
            i,
            i2
        );
    }

    private void renderRowButton(
        DrawContext drawContext, Icon icon2, float f, Animation animation, float f2, float f3, float f4, float f5, float f6, int i, int i2
    ) {
        animation.update();
        animation.animate(this.isHovered(i, i2, f2, f3, f4, f5) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f7 = animation.get();
        float f8 = Math.min(f4, f5);
        if (f7 > 0.001F) {
            GuiRenderUtil.drawHighlight(
                drawContext, f2 + f4 / 2.0F - f8 / 2.0F, f3 + f5 / 2.0F - f8 / 2.0F, f8, f8, f6 * f7, Radius.all(f8 / 3.0F)
            );
        }

        int i3 = Theme.withAlpha(Theme.getSecondaryText(), (0.55F + 0.4F * f7) * f6);
        new RenderUtil(drawContext, f2, f3, f4, f5).icon(icon2, f4 / 2.0F - f / 2.0F, f5 / 2.0F - f / 2.0F, f, i3);
    }

    private static Icon getElementIcon(EspElement espElement) {
        Icon icon = espElement.getIcon();
        if (icon != null) {
            return icon;
        }

        String string = espElement.getAlias() != null ? espElement.getAlias() : espElement.getDisplayName();
        byte b = -1;
        switch (string.hashCode()) {
            case -2040527288:
                if (string.equals("Cooldowns")) {
                    b = 8;
                }
                break;
            case -908342001:
                if (string.equals("Nametag")) {
                    return Icon.ID_CARD;
                }
                break;
            case -315708619:
                if (string.equals("Held items")) {
                    return Icon.SWORDS;
                }
                break;
            case 66987:
                if (string.equals("Box")) {
                    return Icon.OBJECT_GROUP;
                }
                break;
            case 2222509:
                if (string.equals("Glow")) {
                    return Icon.LIGHTBULB;
                }
                break;
            case 63533343:
                if (string.equals("Armor")) {
                    return Icon.SHIELD;
                }
                break;
            case 65070882:
                if (string.equals("Chams")) {
                    return Icon.PALETTE;
                }
                break;
            case 1274052896:
                if (string.equals("Potions")) {
                    return Icon.PILL;
                }
                break;
            case 2033234935:
                if (string.equals("HealthBar")) {
                    return Icon.HEART;
                }
        }

        switch (b) {
            case 0:
                return Icon.ID_CARD;
            case 1:
                return Icon.OBJECT_GROUP;
            case 2:
                return Icon.LIGHTBULB;
            case 3:
                return Icon.PALETTE;
            case 4:
                return Icon.HEART;
            case 5:
                return Icon.PILL;
            case 6:
                return Icon.SHIELD;
            case 7:
                return Icon.SWORDS;
            case 8:
                return Icon.GAUGE;
            default:
                return Icon.CIRCLE_NODES;
        }
    }

    private void openElementSettings(EspElement espElement, double d) {
        this.clickGui.toggleSettingsPopupAt(espElement, this.menuX + this.menuWidth - this.panelWidth, (float)d, this.panelWidth);
    }

    private EspElement getMenuRowAt(double d, double d2) {
        if (!this.menuOpen) {
            return null;
        }

        List<EspElement> espElements = this.getCurrentElements();
        float f = this.menuX + 6.0F;
        float f2 = this.menuWidth - 12.0F;
        float f3 = this.menuY + 3.0F;

        for (EspElement espElement : espElements) {
            if (this.isHovered(d, d2, f - 2.0F, f3, f2 + 4.0F, 14.0)) {
                return espElement;
            }

            f3 += 14.0F;
        }

        return null;
    }

    private EspElement getResetButtonAt(double d, double d2) {
        return this.getRowButtonAt(d, d2, 24.0F, 12.0F, true);
    }

    private EspElement getSettingsButtonAt(double d, double d2) {
        return this.getRowButtonAt(d, d2, 12.0F, 12.0F, false);
    }

    private EspElement getRowButtonAt(double d, double d2, float f, float f2, boolean flag) {
        if (!this.menuOpen) {
            return null;
        }

        float f3 = this.menuX + 6.0F;
        float f4 = this.menuWidth - 12.0F;
        float f5 = this.menuY + 3.0F;

        for (EspElement espElement : this.getCurrentElements()) {
            if ((!flag || espElement.isResizable()) && this.isHovered(d, d2, f3 + f4 - f, f5, f2, 14.0)) {
                return espElement;
            }

            f5 += 14.0F;
        }

        return null;
    }

    private boolean isOverMenu(double d, double d2) {
        return this.menuOpen && this.isHovered(d, d2, this.menuX, this.menuY, this.menuWidth, this.menuHeight);
    }

    public boolean isMouseOver(double d, double d2) {
        return this.isHovered(d, d2, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
    }

    public void mouseMoved(double d, double d2) {
    }

    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.isMouseOver(click.x(), click.y())) {
            return false;
        }

        if (this.menuOpen) {
            if (!this.isOverMenu(click.x(), click.y())) {
                this.closeMenu();
                return true;
            }

            if (this.isLeftButton(click.button())) {
                EspElement espElement = this.getSettingsButtonAt(click.x(), click.y());
                if (espElement != null) {
                    this.openElementSettings(espElement, click.y());
                    return true;
                }

                EspElement espElement2 = this.getResetButtonAt(click.x(), click.y());
                if (espElement2 != null) {
                    espElement2.resetLayout();
                    return true;
                }
            }

            EspElement espElement3 = this.getMenuRowAt(click.x(), click.y());
            if (espElement3 == null) {
                return true;
            }

            if (this.isLeftButton(click.button())) {
                espElement3.enabled.toggle();
                return true;
            }

            if (this.isRightButton(click.button())) {
                this.openElementSettings(espElement3, click.y());
                return true;
            }
        } else if (this.isLeftButton(click.button())
            && this.isHovered(click.x(), click.y(), this.categoryBarX, this.categoryBarY, this.categoryBarWidth, this.categoryBarHeight)) {
            if (this.isHovered(click.x(), click.y(), this.categoryBarX, this.categoryBarY, 4.0F + 7.0F + 2.0F, this.categoryBarHeight)) {
                this.setCategory(this.currentCategory.previous());
                return true;
            }

            if (this.isHovered(
                click.x(),
                click.y(),
                this.categoryBarX + this.categoryBarWidth - 4.0F - 7.0F - 2.0F,
                this.categoryBarY,
                4.0F + 7.0F + 2.0F,
                this.categoryBarHeight
            )) {
                this.setCategory(this.currentCategory.next());
                return true;
            }

            for (EspCategoryTab espCategoryTab : this.categoryTabs) {
                if (espCategoryTab.mouseClicked(click, flag)) {
                    return true;
                }
            }
        } else {
            if (this.isHovered(click.x(), click.y(), this.previewX, this.previewY, this.previewWidth, this.previewHeight)) {
                if (this.isRightButton(click.button())) {
                    this.openMenu(click.x(), click.y());
                    return true;
                }

                if (this.isLeftButton(click.button())) {
                    this.pressPending = true;
                    this.pressX = click.x();
                    this.pressY = click.y();
                    this.dragMouseX = click.x();
                    this.dragMouseY = click.y();
                    this.dragDistance = 0.0;
                    this.rotating = false;
                    this.draggedElement = null;
                    this.pressedElement = this.getElementAt(click.x(), click.y());
                    if (this.pressedElement != null) {
                        this.dragOffsetX = click.x() - this.pressedElement.position2.x;
                        this.dragOffsetY = click.y() - this.pressedElement.position2.y;
                        return true;
                    }

                    return true;
                }
            }

            return false;
        }

        return true;
    }

    public boolean mouseReleased(Click click) {
        if (this.draggedElement != null) {
            this.stopDragging();
            return true;
        } else if (this.pressPending) {
            this.openMenu(this.pressX, this.pressY);
            this.pressPending = false;
            this.pressedElement = null;
            return true;
        } else if (this.rotating) {
            this.rotating = false;
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseDragged(Click click, double d, double d2) {
        this.dragMouseX += d;
        this.dragMouseY += d2;
        if (this.pressPending) {
            this.dragDistance = this.dragDistance + (Math.abs(d) + Math.abs(d2));
            if (this.dragDistance > 3.0) {
                this.pressPending = false;
                if (this.pressedElement != null) {
                    this.draggedElement = this.pressedElement;
                } else {
                    this.rotating = true;
                }
            }
        }

        if (this.draggedElement != null) {
            if (this.draggedElement.isAttachable()) {
                this.dragAttached(this.dragMouseX, this.dragMouseY);
                return true;
            } else {
                this.dragToSide(this.dragMouseX, this.dragMouseY);
                return true;
            }
        } else if (this.rotating) {
            this.previewYaw -= (float)d * 0.8F;
            this.previewPitch = MathHelper.wrapDegrees(this.previewPitch + (float)d2 * 0.3F);
            return true;
        } else {
            return false;
        }
    }

    private EspElement getElementAt(double d, double d2) {
        Esp esp = Esp.getInstance();
        if (esp == null) {
            return null;
        }

        List list = esp.getElements(this.currentCategory);

        for (int i = list.size() - 1; i >= 0; i--) {
            EspElement espElement = (EspElement)list.get(i);
            if (espElement.isResizable()
                && espElement.isActive()
                && !(espElement.size2.x <= 0.0F)
                && !(espElement.size2.y <= 0.0F)
                && this.isHovered(d, d2, espElement.position2.x - 3.0F, espElement.position2.y - 3.0F, espElement.size2.x + 3.0F * 2.0F, espElement.size2.y + 3.0F * 2.0F)) {
                return espElement;
            }
        }

        return null;
    }

    private void dragToSide(double d, double d2) {
        if (!(this.entityBoxWidth <= 0.0F) && !(this.entityBoxHeight <= 0.0F)) {
            EspAnchor espAnchor = this.findNearestSide(this.draggedElement, d, d2);
            if (espAnchor != null) {
                this.draggedElement.setSide(espAnchor);
            }
        }
    }

    private void stopDragging() {
        this.draggedElement = null;
        this.pressedElement = null;
        this.snapGuideX = Float.NaN;
        this.snapGuideY = Float.NaN;
        this.snapTarget = null;
    }

    private void dragAttached(double d, double d2) {
        EspElement espElement = this.draggedElement;
        float f = espElement.size2.x;
        float f2 = espElement.size2.y;
        if (!(f <= 0.0F) && !(f2 <= 0.0F) && !(this.entityBoxWidth <= 0.0F) && !(this.entityBoxHeight <= 0.0F)) {
            this.snapGuideX = Float.NaN;
            this.snapGuideY = Float.NaN;
            float f3 = Math.clamp((float)(d - this.dragOffsetX), this.previewX + 8.0F - f, this.previewX + this.previewWidth - 8.0F);
            float f4 = Math.clamp((float)(d2 - this.dragOffsetY), this.previewY + 8.0F - f2, this.previewY + this.previewHeight - 8.0F);
            EspElement espElement2 = this.isAltHeld() ? null : this.findAttachTarget(espElement, f3, f4, f, f2);
            EspEditorPanel.AxisAnchor axisAnchor = this.computeAxisAnchor(espElement2, true, f3, f);
            EspEditorPanel.AxisAnchor axisAnchor2 = this.computeAxisAnchor(espElement2, false, f4, f2);
            if (espElement2 != null) {
                this.snapToAttachTarget(espElement2, axisAnchor, axisAnchor2, f, f2);
            }

            this.snapTarget = espElement2;
            if (espElement2 == null) {
                espElement.setLayout(axisAnchor.position, axisAnchor2.position, axisAnchor.pivot, axisAnchor2.pivot, axisAnchor.offset, axisAnchor2.offset, null, axisAnchor.anchor, axisAnchor2.anchor);
            } else {
                espElement.setLayout(
                    axisAnchor.position, axisAnchor2.position, axisAnchor.pivot, axisAnchor2.pivot, axisAnchor.offset, axisAnchor2.offset, espElement2.getDisplayName(), axisAnchor.anchor, axisAnchor2.anchor
                );
            }
        }
    }

    private EspElement findAttachTarget(EspElement espElement, float f, float f2, float f3, float f4) {
        List<EspElement> espElements = this.getCurrentElements();
        EspElement espElement2 = null;
        float f5 = 6.5F;

        for (EspElement espElement3 : espElements) {
            if (espElement3 != espElement
                && espElement3.isAttachable()
                && espElement3.isActive()
                && !espElement3.isAttachedTo(espElement, espElements)
                && !(espElement3.size2.x <= 0.0F)
                && !(espElement3.size2.y <= 0.0F)) {
                float f6 = Math.max(espElement3.position2.x - (f + f3), f - (espElement3.position2.x + espElement3.size2.x));
                float f7 = Math.max(espElement3.position2.y - (f2 + f4), f2 - (espElement3.position2.y + espElement3.size2.y));
                float f8 = Math.max(f6, f7);
                if (!(f8 >= f5)) {
                    f5 = f8;
                    espElement2 = espElement3;
                }
            }
        }

        return espElement2;
    }

    private void snapToAttachTarget(EspElement espElement, EspEditorPanel.AxisAnchor axisAnchor, EspEditorPanel.AxisAnchor axisAnchor2, float f, float f2) {
        float f3 = espElement.position2.x;
        float f4 = espElement.position2.y;
        float f5 = espElement.size2.x;
        float f6 = espElement.size2.y;
        float f7 = f3 + axisAnchor.anchor * f5 - axisAnchor.pivot * f + axisAnchor.offset;
        float f8 = f4 + axisAnchor2.anchor * f6 - axisAnchor2.pivot * f2 + axisAnchor2.offset;
        float f9 = Math.min(f7 + f, f3 + f5) - Math.max(f7, f3);
        float f10 = Math.min(f8 + f2, f4 + f6) - Math.max(f8, f4);
        if (!(f9 <= 0.0F) && !(f10 <= 0.0F)) {
            boolean flag;
            EspEditorPanel.AxisAnchor axisAnchor3;
            boolean flag2;
            float f11;
            EspEditorPanel.AxisAnchor axisAnchor4;
            float f12;
            if (f9 < f10) {
                flag = true;
                axisAnchor3 = axisAnchor;
                if (f7 + f / 2.0F < f3 + f5 / 2.0F) {
                    flag2 = true;
                    f11 = f3;
                    axisAnchor4 = axisAnchor;
                    f12 = 0.0F;
                } else {
                    flag2 = false;
                    f11 = f3 + f5;
                    axisAnchor4 = axisAnchor;
                    f12 = 1.0F;
                }
            } else {
                flag = false;
                axisAnchor3 = axisAnchor2;
                if (f8 + f2 / 2.0F < f4 + f6 / 2.0F) {
                    flag2 = true;
                    f11 = f4;
                    axisAnchor4 = axisAnchor3;
                    f12 = 0.0F;
                } else {
                    flag2 = false;
                    float f13 = f4;
                    f11 = f13 + f6;
                    axisAnchor4 = axisAnchor3;
                    f12 = 1.0F;
                }
            }

            axisAnchor4.anchor = f12;
            axisAnchor3.pivot = flag2 ? 1.0F : 0.0F;
            axisAnchor3.offset = flag2 ? -2.0F : 2.0F;
            axisAnchor3.position = (f11 - (flag ? this.entityBoxX : this.entityBoxY)) / (flag ? this.entityBoxWidth : this.entityBoxHeight);
            if (flag) {
                this.snapGuideX = f11;
            } else {
                this.snapGuideY = f11;
            }
        }
    }

    private EspEditorPanel.AxisAnchor computeAxisAnchor(EspElement espElement, boolean flag, float f, float f2) {
        float f3 = flag ? this.entityBoxX : this.entityBoxY;
        float f4 = flag ? this.entityBoxWidth : this.entityBoxHeight;
        float f5 = f3;
        float f6 = f4;
        EspEditorPanel.AxisAnchor axisAnchor = new EspEditorPanel.AxisAnchor();
        axisAnchor.position = (f - f3) / f4;
        if (espElement != null) {
            f5 = flag ? espElement.position2.x : espElement.position2.y;
            f6 = flag ? espElement.size2.x : espElement.size2.y;
            float f7 = f5 + f6 / 2.0F;
            axisAnchor.anchor = 0.5F;
            axisAnchor.pivot = 0.5F;
            axisAnchor.offset = f + f2 / 2.0F - f7;
            axisAnchor.position = (f7 - f3) / f4;
        }

        if (this.isAltHeld()) {
            return axisAnchor;
        }

        float f8 = 4.5F;

        for (int i = 0; i < 3; i++) {
            for (int i2 = 0; i2 < 3; i2++) {
                float f9 = getCornerOffset(i2, i);
                float f10 = f5 + f6 * i2 * 0.5F;
                float f11 = Math.abs(f + f2 * i * 0.5F - f10 - f9);
                if (!(f11 >= f8)) {
                    f8 = f11;
                    axisAnchor.anchor = i2 * 0.5F;
                    axisAnchor.pivot = i * 0.5F;
                    axisAnchor.offset = f9;
                    axisAnchor.position = (f10 - f3) / f4;
                    if (flag) {
                        this.snapGuideX = f10;
                    } else {
                        this.snapGuideY = f10;
                    }
                }
            }
        }

        return axisAnchor;
    }

    private static float getCornerOffset(int i, int i2) {
        if (i == 2 && i2 == 0) {
            return 2.0F;
        } else {
            return i == 0 && i2 == 2 ? -2.0F : 0.0F;
        }
    }

    private boolean isAltHeld() {
        long l = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(l, 342) == 1;
    }

    private EspAnchor findNearestSide(EspElement espElement, double d, double d2) {
        EspAnchor[] espAnchors = espElement.getAllowedSides();
        if (espAnchors.length != 0 && !(this.entityBoxWidth <= 0.0F) && !(this.entityBoxHeight <= 0.0F)) {
            float f = this.entityBoxX + this.entityBoxWidth;
            float f2 = this.entityBoxY + this.entityBoxHeight;
            EspAnchor espAnchor = null;
            double d3 = Double.MAX_VALUE;

            for (EspAnchor espAnchor2 : espAnchors) {
                double d4 = espAnchor2.resolveX(this.entityBoxX, f) - d;
                double d5 = espAnchor2.resolveY(this.entityBoxY, f2) - d2;
                double d6 = d4 * d4 + d5 * d5;
                if (d6 < d3) {
                    d3 = d6;
                    espAnchor = espAnchor2;
                }
            }

            return espAnchor;
        } else {
            return null;
        }
    }

    private void renderDragHighlight(DrawContext drawContext, EspElement espElement, float f) {
        if (!(espElement.size2.x <= 0.0F) && !(espElement.size2.y <= 0.0F)) {
            float f2 = espElement.position2.x - 2.0F;
            float f3 = espElement.position2.y - 2.0F;
            float f4 = espElement.size2.x + 2.0F * 2.0F;
            float f5 = espElement.size2.y + 2.0F * 2.0F;
            int i = Theme.getPrimary();
            new RenderUtil(drawContext, f2, f3, f4, f5)
                .rect(ColorUtil.multiplyAlpha(i, 0.14F * f), Radius.all(3.0F))
                .shadow(0.8F, ColorUtil.multiplyAlpha(i, 0.9F * f), Radius.all(3.0F));
            if (espElement.isAttachable()) {
                this.renderSnapGuides(drawContext, i, f);
                return;
            }

            if (!(this.entityBoxWidth <= 0.0F)) {
                if (this.entityBoxHeight <= 0.0F) {
                    return;
                }

                float f6 = this.entityBoxX + this.entityBoxWidth;
                float f7 = this.entityBoxY + this.entityBoxHeight;
                EspAnchor espAnchor = espElement.getSide();

                for (EspAnchor espAnchor2 : espElement.getAllowedSides()) {
                    boolean flag;
                    float f8;
                    if (espAnchor2 == espAnchor) {
                        flag = true;
                        f8 = 2.0F;
                    } else {
                        flag = false;
                        f8 = 1.3F;
                    }

                    float f9 = f8;
                    float f10 = espAnchor2.resolveX(this.entityBoxX, f6);
                    float f11 = espAnchor2.resolveY(this.entityBoxY, f7);
                    int i2 = flag ? i : Theme.getSecondaryText();
                    new RenderUtil(drawContext, f10 - f9, f11 - f9, f9 * 2.0F, f9 * 2.0F)
                        .rect(ColorUtil.multiplyAlpha(i2, (flag ? 0.95F : 0.5F) * f), Radius.all(f9));
                }
            }
        }
    }

    private void renderSnapGuides(DrawContext drawContext, int i, float f) {
        int i2 = ColorUtil.multiplyAlpha(i, 0.7F * f);
        if (!Float.isNaN(this.snapGuideX)) {
            new RenderUtil(drawContext, this.snapGuideX - 0.25F, this.previewY, 0.5F, this.previewHeight).rect(i2, Radius.all(0.0F));
        }

        if (!Float.isNaN(this.snapGuideY)) {
            new RenderUtil(drawContext, this.previewX, this.snapGuideY - 0.25F, this.previewWidth, 0.5F).rect(i2, Radius.all(0.0F));
        }

        if (this.snapTarget != null && this.snapTarget.size2.x > 0.0F && this.snapTarget.size2.y > 0.0F) {
            new RenderUtil(
                    drawContext,
                    this.snapTarget.position2.x - 1.0F,
                    this.snapTarget.position2.y - 1.0F,
                    this.snapTarget.size2.x + 2.0F,
                    this.snapTarget.size2.y + 2.0F
                )
                .shadow(0.6F, ColorUtil.multiplyAlpha(i, 0.6F * f), Radius.all(3.0F));
        }
    }

    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (!this.isMouseOver(d, d2)) {
            return false;
        }

        if (this.menuOpen) {
            return true;
        }

        if (d4 > 0.0) {
            this.setCategory(this.currentCategory.previous());
            return true;
        }

        if (d4 < 0.0) {
            this.setCategory(this.currentCategory.next());
        }

        return true;
    }

    public boolean keyPressed(KeyInput keyInput) {
        if (this.menuOpen && keyInput.key() == 256) {
            this.closeMenu();
            return true;
        } else {
            return false;
        }
    }

    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    public boolean charTyped(CharInput charInput) {
        return false;
    }

    public EspCategoryTab[] getCategoryTabs() {
        return this.categoryTabs;
    }

    public EspTargetType getCurrentCategory() {
        return this.currentCategory;
    }

    static final class AxisAnchor {
        float position;
        float pivot;
        float offset;
        float anchor;
    }
}
