package tech.sadnes.client.ui.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.menu.MenuBackgroundRenderer;
import tech.sadnes.client.ui.menu.MenuButton;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class LanguageScreen extends Screen implements IRenderable, IMinecraft {
    private static final float PADDING = 14.0F;
    private static final float ROW_GAP = 2.0F;
    private static final float ROW_HEIGHT = 14.0F;
    private final Screen parent;
    private final Animation openAnimation = new Animation();
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private final List<LanguageScreen.LanguageEntry> allLanguages = new ArrayList<>();
    private List<LanguageScreen.LanguageEntry> filteredLanguages = new ArrayList<>();
    private String selectedCode = "";
    private String searchQuery = "";
    private boolean searchFocused;
    private MenuButton doneButton;
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float titleY;
    private float searchX;
    private float searchY;
    private float searchWidth;
    private float searchHeight;
    private float listY;
    private float listHeight;
    private float footerY;

    public LanguageScreen(Screen screen) {
        super(Text.literal("Language"));
        this.parent = screen;
    }

    @Override
    public void init() {
        this.openAnimation.forceValue(0.0);
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
        this.scroll.reset();
        if (this.doneButton == null) {
            this.doneButton = new MenuButton(MenuButton.Style.GHOST, Icon.CHECK, "Готово", this::applySelectedLanguage)
                .withAccentColor(Theme.getPrimary())
                .withChevron(false);
        }

        this.loadLanguages();
        this.updateLayout();
        this.applyFilter();
    }

    private void loadLanguages() {
        this.allLanguages.clear();

        for (Entry entry : mc.getLanguageManager().getAllLanguages().entrySet()) {
            String string = ((LanguageDefinition)entry.getValue()).getDisplayText().getString();
            if (string != null && !string.replaceAll("[()\\s]", "").isEmpty()) {
                this.allLanguages.add(new LanguageScreen.LanguageEntry((String)entry.getKey(), string));
            }
        }

        if (this.allLanguages.stream().noneMatch(languageEntry -> languageEntry.code.equals("en_us"))) {
            this.allLanguages.add(new LanguageScreen.LanguageEntry("en_us", "English (US)"));
        }

        this.allLanguages.sort(Comparator.comparing(languageEntry2 -> languageEntry2.display, String.CASE_INSENSITIVE_ORDER));
        this.selectedCode = mc.getLanguageManager().getLanguage();
    }

    private void updateLayout() {
        float f = GuiScale.getScaledWidth();
        float f2 = GuiScale.getScaledHeight();
        this.panelWidth = Math.min(f * 0.5F, 360.0F);
        this.panelHeight = Math.min(f2 * 0.86F, 460.0F);
        this.panelX = (f - this.panelWidth) / 2.0F;
        this.panelY = (f2 - this.panelHeight) / 2.0F;
        this.titleY = this.panelY + 16.0F;
        this.searchX = this.panelX + 14.0F;
        this.searchY = this.titleY + 28.0F;
        this.searchWidth = this.panelWidth - 28.0F;
        this.searchHeight = 16.0F;
        this.listY = this.searchY + this.searchHeight + 8.0F;
        this.footerY = this.panelY + this.panelHeight - 24.0F;
        this.listHeight = this.footerY - 8.0F - this.listY;
        this.doneButton.setBounds(this.panelX + this.panelWidth - 14.0F - 96.0F, this.footerY + (24.0F - 16.0F) / 2.0F - 1.0F, 96.0F, 16.0F);
    }

    private void applyFilter() {
        if (this.searchQuery != null && !this.searchQuery.isBlank()) {
            String string = this.searchQuery.toLowerCase(Locale.ROOT);
            ArrayList arrayList = new ArrayList();

            for (LanguageScreen.LanguageEntry languageEntry : this.allLanguages) {
                if (languageEntry.display.toLowerCase(Locale.ROOT).contains(string) || languageEntry.code.contains(string)) {
                    arrayList.add(languageEntry);
                }
            }

            this.filteredLanguages = arrayList;
        } else {
            this.filteredLanguages = new ArrayList<>(this.allLanguages);
        }
    }

    private void applySelectedLanguage() {
        String string = mc.getLanguageManager().getLanguage();
        if (this.selectedCode != null && !this.selectedCode.isEmpty() && !this.selectedCode.equals(string)) {
            mc.getLanguageManager().setLanguage(this.selectedCode);
            mc.options.language = this.selectedCode;
            mc.options.write();
            mc.reloadResources();
        }

        mc.setScreen(this.parent);
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        Sadnes.getInstance().saveAll();
        if (Fonts.INTER_MEDIUM != null) {
            int i3 = (int)GuiScale.toScaled(i);
            int i4 = (int)GuiScale.toScaled(i2);
            this.openAnimation.update();
            float f2 = this.openAnimation.get();
            MenuBackgroundRenderer.renderBackground(drawContext, 1.0F);
            GuiRenderUtil.window(
                drawContext, RenderUtil.BlurQuality.MIDDLE, true, this.panelX, this.panelY, this.panelWidth, this.panelHeight, f2 * 0.85F, Radius.all(10.0F)
            );
            new RenderUtil(drawContext, this.panelX, this.titleY, this.panelWidth, 14.0F)
                .textTopCenter(Fonts.INTER_BOLD, "ЯЗЫК", 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f2), 13.0F);
            new RenderUtil(drawContext, this.panelX, this.titleY + 15.0F, this.panelWidth, 8.0F)
                .textTopCenter(
                    Fonts.INTER_REGULAR, "Переводы могут содержать ошибки", 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f2), 6.0F
                );
            this.renderSearchField(drawContext, f2);
            this.renderList(drawContext, i3, i4, f2);
            this.renderFooter(drawContext, i3, i4, f2);
        }
    }

    private void renderSearchField(DrawContext drawContext, float f) {
        GuiRenderUtil.drawOutlined(drawContext, this.searchX, this.searchY, this.searchWidth, this.searchHeight, f, 8.0F);
        if (this.searchFocused) {
            new RenderUtil(drawContext, this.searchX, this.searchY, this.searchWidth, this.searchHeight)
                .shadow(1.0F, Theme.withAlpha(Theme.getPrimary(), 0.7F * f), Radius.all(8.0F));
        }

        new RenderUtil(drawContext, this.searchX + 6.0F, this.searchY, 12.0F, this.searchHeight)
            .icon(Icon.SEARCH, 0.0F, (this.searchHeight - 9.0F) / 2.0F, 9.0F, Theme.withAlpha(Theme.getSecondaryText(), f));
        boolean flag;
        if (this.searchQuery.isEmpty()) {
            flag = !this.searchFocused;
        } else {
            flag = false;
        }

        String string = flag ? "Поиск языка" : this.searchQuery + (this.searchFocused && isCaretVisible() ? "_" : "");
        int i = flag ? Theme.withAlpha(Theme.getSecondaryText(), 0.8F * f) : Theme.withAlpha(Theme.getTextColor(), f);
        new RenderUtil(drawContext, this.searchX + 20.0F, this.searchY, this.searchWidth - 24.0F, this.searchHeight)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i, 6.5F);
    }

    private void renderList(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.filteredLanguages.isEmpty() ? 0.0F : this.filteredLanguages.size() * 16.0F;
        this.scroll.update(f2, this.listHeight);
        this.scroll.updateHover(i, i2);
        if (this.filteredLanguages.isEmpty()) {
            new RenderUtil(drawContext, this.panelX, this.listY, this.panelWidth, this.listHeight)
                .textCenter(Fonts.INTER_MEDIUM, "Ничего не найдено", Theme.withAlpha(Theme.getSecondaryText(), 0.8F * f), 7.0F);
        } else {
            boolean flag = ScissorStack.push(drawContext, this.panelX, this.listY, this.panelWidth, this.listHeight);

            try {
                float f3 = this.scroll.getScroll();
                float f4 = this.panelX + 14.0F;
                float f5 = this.panelWidth - 28.0F - 6.0F;

                for (int i3 = 0; i3 < this.filteredLanguages.size(); i3++) {
                    float f6 = this.listY + f3 + i3 * 16.0F;
                    if (!(f6 + 14.0F < this.listY) && !(f6 > this.listY + this.listHeight)) {
                        this.renderEntry(drawContext, f4, f6, f5, i, i2, f, this.filteredLanguages.get(i3));
                    }
                }
            } finally {
                if (flag) {
                    ScissorStack.pop(drawContext);
                }
            }

            this.scroll.render(drawContext, this.panelX + this.panelWidth - 14.0F + 1.0F, this.listY, this.listHeight, f2, f);
        }
    }

    private void renderEntry(DrawContext drawContext, float f, float f2, float f3, int i, int i2, float f4, LanguageScreen.LanguageEntry languageEntry) {
        boolean flag = languageEntry.code.equals(this.selectedCode);
        boolean flag2 = this.isHovered(i, i2, f, f2, f3, 14.0);
        if (flag) {
            new RenderUtil(drawContext, f, f2, f3, 14.0F).rect(Theme.withAlpha(Theme.getPrimary(), 0.2F * f4), Radius.all(5.0F));
            new RenderUtil(drawContext, f, f2, f3, 14.0F).border(4.0F, Theme.getPrimary(), 0.2F * f4, Radius.all(5.0F));
        } else if (flag2) {
            GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, 14.0F, f4, Radius.all(5.0F));
        }

        int i3 = Theme.withAlpha(flag ? ColorUtil.interpolate(Theme.getTextColor(), ColorUtil.WHITE, 0.4F) : Theme.getTextColor(), f4);
        new RenderUtil(drawContext, f, f2, f3, 14.0F).textCenter(Fonts.INTER_MEDIUM, languageEntry.display, i3, 7.0F);
        if (flag) {
            new RenderUtil(drawContext, f, f2, f3, 14.0F).icon(Icon.CHECK, f3 - 14.0F, 3.5F, 7.0F, Theme.withAlpha(Theme.getPrimary(), f4));
        }
    }

    private void renderFooter(DrawContext drawContext, int i, int i2, float f) {
        new RenderUtil(drawContext, this.panelX + 14.0F, this.footerY, this.panelWidth * 0.45F, 24.0F)
            .textMiddleLeft(Fonts.INTER_REGULAR, "Выбери язык интерфейса", 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.7F * f), 6.0F);
        this.doneButton.render(drawContext, i, i2, f);
    }

    private static boolean isCaretVisible() {
        return System.currentTimeMillis() % 1000L < 500L;
    }

    private Click scaleMouseInput(Click click) {
        return new Click(GuiScale.toScaled(click.x()), GuiScale.toScaled(click.y()), click.buttonInfo());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        Click click2 = this.scaleMouseInput(click);
        double d = click2.x();
        double d2 = click2.y();
        boolean flag2 = this.isHovered(d, d2, this.searchX, this.searchY, this.searchWidth, this.searchHeight);
        this.searchFocused = flag2;
        if (flag2) {
            return true;
        }

        if (this.doneButton.mouseClicked(d, d2)) {
            return true;
        }

        if (this.isHovered(d, d2, this.panelX, this.listY, this.panelWidth, this.listHeight)) {
            if (this.scroll.mouseClicked(d, d2)) {
                return true;
            }

            float f = this.scroll.getScroll();
            float f2 = this.panelX + 14.0F;
            float f3 = this.panelWidth - 28.0F - 6.0F;

            for (int i = 0; i < this.filteredLanguages.size(); i++) {
                float f4 = this.listY + f + i * 16.0F;
                if (!(f4 + 14.0F < this.listY) && !(f4 > this.listY + this.listHeight) && this.isHovered(d, d2, f2, f4, f3, 14.0)) {
                    this.selectedCode = this.filteredLanguages.get(i).code;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        Click click2 = this.scaleMouseInput(click);
        return this.scroll.mouseDragged(click2.x(), click2.y());
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.scroll.mouseReleased();
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        double d5 = GuiScale.toScaled(d);
        double d6 = GuiScale.toScaled(d2);
        if (this.isHovered(d5, d6, this.panelX, this.listY, this.panelWidth, this.listHeight)) {
            this.scroll.onScroll(d4);
            return true;
        } else {
            return super.mouseScrolled(d, d2, d3, d4);
        }
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int i = keyInput.key();
        if (this.searchFocused) {
            switch (i) {
                case 256:
                case 257:
                case 335:
                    this.searchFocused = false;
                    return true;
                case 259:
                    if (!this.searchQuery.isEmpty()) {
                        this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                        this.applyFilter();
                    }
                default:
                    return true;
            }
        } else if (i == 256) {
            this.applySelectedLanguage();
            return true;
        } else {
            return super.keyPressed(keyInput);
        }
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (this.searchFocused) {
            char c = (char)charInput.codepoint();
            if (this.searchQuery.length() < 32 && c >= ' ') {
                this.searchQuery = this.searchQuery + c;
                this.applyFilter();
                return true;
            } else {
                return false;
            }
        } else {
            return super.charTyped(charInput);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    record LanguageEntry(String code, String display) {
    }
}
