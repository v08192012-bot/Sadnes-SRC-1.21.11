package tech.sadnes.client.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.feature.Beta;
import tech.sadnes.client.feature.Danger;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.StatusIconUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.MarkerLevel;
import tech.sadnes.client.server.MarkerServer;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.screen.ScreenElement;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import ua.mintantileak.profile.Profile;

public abstract class Setting<T> implements IInteractable, IRenderable, ScreenElement {
    public Vector2f position = new Vector2f();
    public Vector2f size = new Vector2f();
    public final Animation alphaAnimation = new Animation();
    public final Animation valueAnimation = new Animation();
    public final Animation hoverAnimation = new Animation();
    public float alphaMultiplier = 1.0F;
    public boolean ignoreGuiAlpha;
    private final String name;
    private String displayName;
    private List<String> aliases = List.of();
    private T value;
    private Supplier<Boolean> visibility = () -> true;
    private Runnable onChange;
    private Runnable onVisibilityChange;
    private boolean hidden;
    private boolean child;
    private Beta settingInfo;
    private Danger dependency;
    private Icon icon;
    private Setting.IconRenderer iconRenderer;

    public Setting(String string, T value2) {
        this.name = string;
        this.value = (T)value2;
    }

    public String getName() {
        return this.name;
    }

    public T getValue() {
        return this.value;
    }

    public Setting<T> setValue(T value2) {
        this.setValueInternal((T)value2);
        return this;
    }

    protected final void setValueInternal(T value2) {
        if (!Objects.equals(this.value, value2)) {
            this.value = (T)value2;
            this.runSafely(this.onChange);
        }
    }

    protected final void notifyChange() {
        this.runSafely(this.onChange);
    }

    public Setting<T> setOnChange(Runnable runnable) {
        this.onChange = runnable;
        return this;
    }

    public Setting<T> setOnVisibilityChange(Runnable runnable) {
        this.onVisibilityChange = runnable;
        return this;
    }

    public Setting<T> setVisibleWhen(Supplier<Boolean> supplier) {
        if (supplier != null) {
            this.visibility = supplier;
            this.runSafely(this.onVisibilityChange);
            return this;
        } else {
            this.visibility = () -> true;
            this.runSafely(this.onVisibilityChange);
            return this;
        }
    }

    public boolean isVisible() {
        return this.hasAccess() && this.visibility.get();
    }

    public Setting<T> setSettingInfo(Beta beta) {
        this.settingInfo = beta;
        return this;
    }

    public Beta getSettingInfo() {
        return this.settingInfo;
    }

    public Setting<T> setDependency(Danger danger) {
        this.dependency = danger;
        return this;
    }

    public Danger getDependency() {
        return this.dependency;
    }

    public Setting<T> setIcon(Icon icon2) {
        this.icon = icon2;
        return this;
    }

    public Icon getIcon() {
        return this.icon;
    }

    public Setting<T> setIconRenderer(Setting.IconRenderer iconRenderer2) {
        this.iconRenderer = iconRenderer2;
        return this;
    }

    public Setting.IconRenderer getIconRenderer() {
        return this.iconRenderer;
    }

    public boolean hasAccess() {
        return this.settingInfo == null || this.settingInfo.scope().isAllowed(Profile.getRole());
    }

    public boolean hasSettingInfo() {
        return this.settingInfo != null;
    }

    public MarkerLevel getDependencyStatus() {
        return this.dependency == null ? null : this.dependency.level();
    }

    public boolean isDependencyActive() {
        return this.dependency != null && MarkerServer.anyActive(this.dependency.reasons());
    }

    public Setting<T> setHidden(boolean flag) {
        this.hidden = flag;
        return this;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public Setting<T> setChild(boolean flag) {
        this.child = flag;
        return this;
    }

    public boolean isChild() {
        return this.child;
    }

    public Setting<T> setDisplayName(String string) {
        this.displayName = string;
        return this;
    }

    public String getDisplayName() {
        return this.displayName != null ? this.displayName : this.name;
    }

    public Setting<T> setAliases(String... strings) {
        if (strings != null && strings.length != 0) {
            this.aliases = List.of(strings);
            return this;
        } else {
            this.aliases = List.of();
            return this;
        }
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public float resolveHeight(float f) {
        return f;
    }

    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
    }

    public boolean isOverlayOpen() {
        return false;
    }

    public boolean isTyping() {
        return false;
    }

    public final void handleKey(int i, boolean flag) {
        this.onKey(i, flag);

        for (Setting setting : this.getChildren()) {
            setting.handleKey(i, flag);
        }
    }

    protected void onKey(int i, boolean flag) {
    }

    protected List<Setting<?>> getChildren() {
        return List.of();
    }

    public final List<Setting<?>> getAllChildren() {
        ArrayList arrayList = new ArrayList();
        this.collectChildren(arrayList, this.getChildren());
        return arrayList;
    }

    public final boolean hasBindsRecursive() {
        if (this.hasBinds()) {
            return true;
        }

        List list = this.getChildren();
        int i = list.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (((Setting)list.get(i2)).hasBindsRecursive()) {
                return true;
            }
        }

        return false;
    }

    public final boolean hasBindRecursive(int i) {
        if (this.hasBind(i)) {
            return true;
        }

        List list = this.getChildren();
        int i2 = list.size();

        for (int i3 = 0; i3 < i2; i3++) {
            if (((Setting)list.get(i3)).hasBindRecursive(i)) {
                return true;
            }
        }

        return false;
    }

    protected boolean hasBinds() {
        return false;
    }

    protected boolean hasBind(int i) {
        return false;
    }

    private void runSafely(Runnable runnable) {
        if (runnable != null) {
            try {
                runnable.run();
            } catch (Throwable throwable) {
            }
        }
    }

    public float getAlpha() {
        float f = (float)Math.max(0.0, Math.min(1.0, this.alphaAnimation.get()));
        if (this.ignoreGuiAlpha) {
            return 1.0F * f * this.alphaMultiplier;
        }

        float f2 = this.getClickGui().getOpenAnimation().get();
        return f2 * f * this.alphaMultiplier;
    }

    public int applyAlpha(int i) {
        return ColorUtil.multiplyAlpha(i, this.getAlpha());
    }

    public int getPrimaryColor() {
        return this.getPrimaryColor(1.0F);
    }

    public int getPrimaryColor(float f) {
        return ColorUtil.multiplyAlpha(Theme.getTextColor(), f * this.getAlpha());
    }

    public int getSecondaryColor() {
        return this.getSecondaryColor(1.0F);
    }

    public int getSecondaryColor(float f) {
        return ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f * this.getAlpha());
    }

    public float getPreferredWidth() {
        return this.getLabelWidth() + 6.0F + Math.min(this.getControlWidth(), 64.0F);
    }

    protected float getLabelWidth() {
        float f = this.icon == null ? 0.0F : 6.0F + 1.0F + 3.0F;
        return f + Fonts.INTER_MEDIUM.getWidth(Language.translate(this.getName()), 6.0F);
    }

    protected float getControlWidth() {
        return 14.0F;
    }

    protected void drawLabel(DrawContext drawContext, float f, float f2, float f3, float f4, boolean flag) {
        this.drawLabel(drawContext, f, f2, f3, f4, f3 / 2.0F, flag);
    }

    protected void drawLabel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, boolean flag) {
        float f6 = this.drawIcon(drawContext, f, f2, f4, 6.0F);
        boolean flag2 = this.hasSettingInfo();
        boolean flag3 = this.isDependencyActive();
        float f7 = f6 + StatusIconUtil.drawIcons(drawContext, f + f6, f2, f4, 6.0F, this.getAlpha(), flag2, flag3, this.getDependencyStatus());
        int i = StatusIconUtil.getNameColor(this.getPrimaryColor(), this.getAlpha(), flag2, flag3, this.getDependencyStatus());
        new RenderUtil(drawContext, f, f2, f3, f4)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, Language.translate(this.getName()), f7, 0.0F, Math.max(0.0F, f5 - f7), i, 6.0F, flag);
    }

    protected float drawIcon(DrawContext drawContext, float f, float f2, float f3, float f4) {
        if (this.icon == null) {
            return 0.0F;
        }

        float f5 = f4 + 1.0F;
        int i = ColorUtil.multiplyAlpha(Theme.getPrimary(), this.getAlpha());
        new RenderUtil(drawContext, f, f2, f5, f3).icon(this.icon, 0.0F, f3 / 2.0F - f5 / 2.0F, f5, i);
        return f5 + 3.0F;
    }

    public ClickGuiScreen getClickGui() {
        return Sadnes.getInstance().getClickGui();
    }

    private void collectChildren(List<Setting<?>> settings, List<Setting<?>> settings2) {
        int i = settings2.size();

        for (int i2 = 0; i2 < i; i2++) {
            Setting setting = (Setting)settings2.get(i2);
            settings.add(setting);
            this.collectChildren(settings, setting.getChildren());
        }
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

    @FunctionalInterface
    public interface IconRenderer {
        void render(DrawContext drawContext, float f, float f2, float f3, float f4);
    }
}
