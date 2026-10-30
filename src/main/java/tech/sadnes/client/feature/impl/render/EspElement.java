package tech.sadnes.client.feature.impl.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.feature.impl.render.esp.EspAnchor;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingHolder;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.ui.clickgui.component.Component;

public abstract class EspElement extends Component implements SettingHolder {
    private final String name;
    private final EspTargetType target;
    public final BooleanSetting enabled;
    protected final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> hiddenSettings = new ArrayList<>();
    public final Animation visibilityAnimation = new Animation();
    private boolean dragging;
    public final Vector2f position2 = new Vector2f();
    public final Vector2f size2 = new Vector2f();
    private boolean suppressed;
    private static final float SMOOTH_SPEED = 16.0F;
    private float smoothAnchorX;
    private float smoothAnchorY;
    private float smoothPivotX;
    private float smoothPivotY;
    private float smoothOffsetX;
    private float smoothOffsetY;
    private boolean smoothInitialized;
    private long lastSmoothTime;
    private float layoutX;
    private float layoutY;
    protected float anchorX = 0.5F;
    protected float anchorY = 0.0F;
    protected float pivotX = 0.5F;
    protected float pivotY = 1.0F;
    protected float offsetX = 0.0F;
    protected float offsetY = -2.0F;
    private String parentName;
    private float parentAnchorX;
    private float parentAnchorY;
    private TextSetting layoutSetting;
    private String lastLayout;
    private EspAnchor defaultSide = EspAnchor.TOP;
    protected float width;
    protected float height;
    protected boolean measured;
    private boolean laidOut;
    private boolean layingOut;

    public EspElement(String string, boolean flag, EspTargetType espTargetType) {
        this.name = string;
        this.target = espTargetType;
        this.enabled = new BooleanSetting(string + " " + espTargetType.name(), flag);
        this.enabled.setHidden(true);
        this.settings.add(this.enabled);
    }

    public boolean isTarget(EspTargetType espTargetType) {
        return this.target == espTargetType;
    }

    public boolean isActive() {
        return !this.suppressed && this.enabled.getValue();
    }

    public void setSuppressed(boolean flag) {
        this.suppressed = flag;
    }

    public String getAlias() {
        return null;
    }

    public boolean matchesName(String string) {
        return this.name.equals(string) || string != null && string.equals(this.getAlias());
    }

    public void updateItems(List<ItemStack> itemStacks) {
    }

    public boolean usesItems() {
        return false;
    }

    protected boolean isFriendTarget() {
        return this.target == EspTargetType.FRIENDS;
    }

    protected static ItemStack prepareDisplayStack(ItemStack itemStack) {
        if (!ServerManager.isHolyWorld()) {
            return itemStack;
        }

        ItemStack itemStack2 = itemStack.copy();
        itemStack2.setDamage(0);
        Integer integer = itemStack.getItem().getComponents().get(DataComponentTypes.MAX_STACK_SIZE);
        if (integer != null && integer == 1) {
            itemStack2.setCount(1);
        }

        return itemStack2;
    }

    public boolean isVisible() {
        return this.isActive();
    }

    public Icon getIcon() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(this.settings);
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        return setting != this.enabled && !this.hiddenSettings.contains(setting);
    }

    public boolean isHovered(double d, double d2) {
        return !(this.size2.x <= 0.0F) && !(this.size2.y <= 0.0F) ? this.isHovered(d, d2, this.position2, this.size2) : false;
    }

    public boolean isResizable() {
        return false;
    }

    public EspAnchor[] getAllowedSides() {
        return new EspAnchor[0];
    }

    public EspAnchor getSide() {
        return EspAnchor.TOP;
    }

    public void setSide(EspAnchor espAnchor) {
    }

    protected void computeLayout(
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        float f9,
        float f10,
        float f11,
        float f12,
        boolean flag
    ) {
        float f13 = f7;
        float f14 = f8;
        float f15 = f9;
        float f16 = f10;
        float f17 = f11;
        float f18 = f12;
        if (flag) {
            if (!this.smoothInitialized) {
                this.smoothAnchorX = f7;
                this.smoothAnchorY = f8;
                this.smoothPivotX = f9;
                this.smoothPivotY = f10;
                this.smoothOffsetX = f11;
                this.smoothOffsetY = f12;
                this.smoothInitialized = true;
                this.lastSmoothTime = System.nanoTime();
            } else {
                long l = System.nanoTime();
                float f19 = this.lastSmoothTime == 0L ? 0.016666668F : (float)(l - this.lastSmoothTime) / 1.0E9F;
                this.lastSmoothTime = l;
                float f20 = Math.max(0.004166667F, Math.min(0.1F, f19));
                float f21 = 1.0F - (float)Math.exp(-16.0F * f20);
                this.smoothAnchorX = this.smoothAnchorX + (f7 - this.smoothAnchorX) * f21;
                this.smoothAnchorY = this.smoothAnchorY + (f8 - this.smoothAnchorY) * f21;
                this.smoothPivotX = this.smoothPivotX + (f9 - this.smoothPivotX) * f21;
                this.smoothPivotY = this.smoothPivotY + (f10 - this.smoothPivotY) * f21;
                this.smoothOffsetX = this.smoothOffsetX + (f11 - this.smoothOffsetX) * f21;
                this.smoothOffsetY = this.smoothOffsetY + (f12 - this.smoothOffsetY) * f21;
            }

            f13 = this.smoothAnchorX;
            f14 = this.smoothAnchorY;
            f15 = this.smoothPivotX;
            f16 = this.smoothPivotY;
            f17 = this.smoothOffsetX;
            f18 = this.smoothOffsetY;
        }

        this.layoutX = f + f13 * (f3 - f) - f15 * f5 + f17;
        this.layoutY = f2 + f14 * (f4 - f2) - f16 * f6 + f18;
    }

    protected float getLayoutX() {
        return this.layoutX;
    }

    protected float getLayoutY() {
        return this.layoutY;
    }

    public boolean isAttachable() {
        return false;
    }

    protected TextSetting createLayoutSetting(EspAnchor espAnchor) {
        return this.createLayoutSetting(espAnchor, null, 0.0F, 0.0F);
    }

    protected TextSetting createLayoutSetting(EspAnchor espAnchor, String string, float f, float f2) {
        this.defaultSide = espAnchor;
        this.applySide(espAnchor);
        this.parentName = string;
        this.parentAnchorX = f;
        this.parentAnchorY = f2;
        this.layoutSetting = this.addHiddenSetting(new TextSetting("Layout", this.serializeLayout()));
        this.lastLayout = this.layoutSetting.getValue();
        return this.layoutSetting;
    }

    public void resetLayout() {
        if (this.layoutSetting != null) {
            this.applySide(this.defaultSide);
            this.parentName = null;
            this.parentAnchorX = 0.0F;
            this.parentAnchorY = 0.0F;
            this.smoothInitialized = false;
            String string = this.serializeLayout();
            this.layoutSetting.setValue(string);
            this.lastLayout = string;
        }
    }

    private void applySide(EspAnchor espAnchor) {
        this.anchorX = espAnchor.getAnchorX();
        this.anchorY = espAnchor.getAnchorY();
        this.pivotX = espAnchor.getPivotX();
        this.pivotY = espAnchor.getPivotY();
        this.offsetX = espAnchor.getOffsetX() * 2.0F;
        this.offsetY = espAnchor.getOffsetY() * 2.0F;
    }

    private String serializeLayout() {
        String string = this.parentName == null ? "" : this.parentName;
        return this.anchorX
            + " "
            + this.anchorY
            + " "
            + this.pivotX
            + " "
            + this.pivotY
            + " "
            + this.offsetX
            + " "
            + this.offsetY
            + "|"
            + string
            + "|"
            + this.parentAnchorX
            + "|"
            + string
            + "|"
            + this.parentAnchorY;
    }

    private void syncLayout() {
        if (this.layoutSetting != null) {
            String string = this.layoutSetting.getValue();
            if (string != null && !string.equals(this.lastLayout)) {
                this.lastLayout = string;
                String[] strings = string.split("\\|", -1);
                String[] strings2 = strings[0].trim().split("\\s+");
                if (strings2.length >= 6) {
                    try {
                        this.anchorX = Float.parseFloat(strings2[0]);
                        this.anchorY = Float.parseFloat(strings2[1]);
                        this.pivotX = Float.parseFloat(strings2[2]);
                        this.pivotY = Float.parseFloat(strings2[3]);
                        this.offsetX = Float.parseFloat(strings2[4]);
                        this.offsetY = Float.parseFloat(strings2[5]);
                        if (strings.length < 5) {
                            this.parentName = null;
                            return;
                        }

                        String string2 = strings[3].isEmpty() ? strings[1] : strings[3];
                        this.parentName = string2.isEmpty() ? null : string2;
                        this.parentAnchorX = Float.parseFloat(strings[2]);
                        this.parentAnchorY = Float.parseFloat(strings[4]);
                        if (this.parentName == null) {
                            return;
                        }

                        boolean flag = !strings[1].equals(string2);
                        boolean flag2 = !strings[3].equals(string2);
                        if (flag) {
                            this.parentAnchorX = this.pivotX;
                            this.offsetX = 0.0F;
                        }

                        if (flag2) {
                            this.parentAnchorY = this.pivotY;
                            this.offsetY = 0.0F;
                        }
                    } catch (NumberFormatException numberFormatException) {
                    }
                }
            }
        }
    }

    public void setLayout(float f, float f2, float f3, float f4, float f5, float f6, String string, float f7, float f8) {
        this.anchorX = f;
        this.anchorY = f2;
        this.pivotX = f3;
        this.pivotY = f4;
        this.offsetX = f5;
        this.offsetY = f6;
        this.parentName = string;
        this.parentAnchorX = f7;
        this.parentAnchorY = f8;
        if (this.layoutSetting != null) {
            String string2 = this.serializeLayout();
            this.layoutSetting.setValue(string2);
            this.lastLayout = string2;
        }
    }

    public final void invalidateLayout() {
        this.laidOut = false;
    }

    public void measure(Entity entity, boolean flag) {
        this.measured = false;
    }

    public final void layout(float f, float f2, float f3, float f4, boolean flag, List<EspElement> espElements) {
        if (!this.laidOut) {
            this.syncLayout();
            if (!this.measured || this.width <= 0.0F || this.height <= 0.0F) {
                this.laidOut = true;
                this.size2.set(0.0F, 0.0F);
            } else if (this.layingOut) {
                this.applyLayout(f, f2, f3, f4, this.anchorX, this.anchorY, flag);
                this.laidOut = true;
            } else {
                this.layingOut = true;
                float f5 = this.anchorX;
                float f6 = this.anchorY;
                if (this.parentName != null) {
                    EspElement espElement = findElement(espElements, this.parentName);
                    if (espElement != null && espElement != this) {
                        espElement.layout(f, f2, f3, f4, flag, espElements);
                        boolean flag2 = espElement.measured && espElement.width > 0.0F && espElement.height > 0.0F && f3 != 0.0F && f4 != 0.0F;
                        if (flag2) {
                            f5 = (espElement.position2.x + this.parentAnchorX * espElement.size2.x - f) / f3;
                            f6 = (espElement.position2.y + this.parentAnchorY * espElement.size2.y - f2) / f4;
                        }
                    }
                }

                this.applyLayout(f, f2, f3, f4, f5, f6, flag);
                this.layingOut = false;
                this.laidOut = true;
            }
        }
    }

    public boolean isAttachedTo(EspElement espElement, List<EspElement> espElements) {
        EspElement espElement2 = this;
        int i = espElements.size();

        for (int i2 = 0; i2 < i; i2++) {
            espElement2 = espElement2.parentName == null ? null : findElement(espElements, espElement2.parentName);
            if (espElement2 == null) {
                return false;
            }

            if (espElement2 == espElement) {
                return true;
            }
        }

        return false;
    }

    private void applyLayout(float f, float f2, float f3, float f4, float f5, float f6, boolean flag) {
        this.computeLayout(
            f, f2, f + f3, f2 + f4, this.width, this.height, f5, f6, this.pivotX, this.pivotY, this.offsetX, this.offsetY, flag
        );
        this.position2.set(this.getLayoutX(), this.getLayoutY());
        this.size2.set(this.width, this.height);
    }

    private static EspElement findElement(List<EspElement> espElements, String string) {
        if (espElements == null) {
            return null;
        }

        for (EspElement espElement : espElements) {
            if (espElement.isActive() && espElement.matchesName(string)) {
                return espElement;
            }
        }

        return null;
    }

    protected <S extends Setting<?>> S addSetting(S value) {
        value.setHidden(true);
        value.setDisplayName(this.name + " " + this.target.name() + " " + value.getName());
        this.settings.add(value);
        return (S)value;
    }

    protected <S extends Setting<?>> S addHiddenSetting(S value) {
        this.addSetting((S)value);
        this.hiddenSettings.add(value);
        return (S)value;
    }

    public abstract void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    );

    public void renderBackground(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
    }

    public void renderOverlay(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
    }

    public void update(Entity entity, float f) {
    }

    public void renderPreview(DrawContext drawContext, Entity entity, float f, float f2) {
    }

    @Override
    public void init(int i, int i2) {
    }

    @Override
    public void resize(int i, int i2) {
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        return false;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public void removed() {
    }

    public String getName() {
        return this.name;
    }

    public EspTargetType getTarget() {
        return this.target;
    }

    public BooleanSetting getEnabled() {
        return this.enabled;
    }

    public List<Setting<?>> getSettingList() {
        return this.settings;
    }

    public List<Setting<?>> getHiddenSettings() {
        return this.hiddenSettings;
    }

    public Animation getVisibilityAnimation() {
        return this.visibilityAnimation;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public Vector2f getPosition() {
        return this.position2;
    }

    public Vector2f getSize() {
        return this.size2;
    }

    public boolean isSuppressed() {
        return this.suppressed;
    }

    public float getSmoothAnchorX() {
        return this.smoothAnchorX;
    }

    public float getSmoothAnchorY() {
        return this.smoothAnchorY;
    }

    public float getSmoothPivotX() {
        return this.smoothPivotX;
    }

    public float getSmoothPivotY() {
        return this.smoothPivotY;
    }

    public float getSmoothOffsetX() {
        return this.smoothOffsetX;
    }

    public float getSmoothOffsetY() {
        return this.smoothOffsetY;
    }

    public boolean isSmoothInitialized() {
        return this.smoothInitialized;
    }

    public long getLastSmoothTime() {
        return this.lastSmoothTime;
    }

    public float getComputedX() {
        return this.layoutX;
    }

    public float getComputedY() {
        return this.layoutY;
    }

    public float getAnchorX() {
        return this.anchorX;
    }

    public float getAnchorY() {
        return this.anchorY;
    }

    public float getPivotX() {
        return this.pivotX;
    }

    public float getPivotY() {
        return this.pivotY;
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    public float getOffsetY() {
        return this.offsetY;
    }

    public String getParentName() {
        return this.parentName;
    }

    public float getParentAnchorX() {
        return this.parentAnchorX;
    }

    public float getParentAnchorY() {
        return this.parentAnchorY;
    }

    public TextSetting getLayoutSetting() {
        return this.layoutSetting;
    }

    public String getLastLayout() {
        return this.lastLayout;
    }

    public EspAnchor getDefaultSide() {
        return this.defaultSide;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public boolean isMeasured() {
        return this.measured;
    }

    public boolean isLaidOut() {
        return this.laidOut;
    }

    public boolean isLayingOut() {
        return this.layingOut;
    }

    public void setDragging(boolean flag) {
        this.dragging = flag;
    }
}
