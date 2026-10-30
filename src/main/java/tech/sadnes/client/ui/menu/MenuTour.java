package tech.sadnes.client.ui.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.WelcomePrefs;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.ShimmerText;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class MenuTour implements IRenderable {
    private static final int STEP_COUNT = MenuTour.Step.values().length - 1;
    private static final float POPUP_WIDTH = 196.0F;
    private static final float PADDING = 10.0F;
    private static final float BODY_TEXT_SIZE = 6.0F;
    private static final float LINE_HEIGHT = 8.0F;
    private final Animation showAnimation = new Animation();
    private final ShimmerText gradientText = new ShimmerText();
    private boolean active;
    private MenuTour.BlockReason blockReason = MenuTour.BlockReason.NONE;
    private MenuTour.Step step = MenuTour.Step.ASK;
    private boolean changelogSeen;
    private int lastAccountCount = -1;
    private int lastFavoriteCount = -1;
    private float popupX;
    private float popupY;
    private float popupWidth;
    private float popupHeight;
    private float nextButtonX;
    private float nextButtonY;
    private float nextButtonWidth;
    private float nextButtonHeight;
    private float skipButtonX;
    private float skipButtonY;
    private float skipButtonWidth;
    private float skipButtonHeight;

    public void start() {
        if (!this.active && !WelcomePrefs.isTourCompleted()) {
            this.active = true;
            this.step = MenuTour.Step.ASK;
            this.changelogSeen = false;
            this.showAnimation.forceValue(0.0);
            this.showAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
        }
    }

    public boolean isActive() {
        return this.active;
    }

    public MenuTour.Step getStep() {
        return this.step;
    }

    public MenuTour.BlockReason getBlockReason() {
        return this.blockReason;
    }

    public void update(MenuTour.State state) {
        if (this.active) {
            if (state.changelogOpen() && this.step != MenuTour.Step.CHANGELOG) {
                this.blockReason = MenuTour.BlockReason.CLOSE_CHANGELOG;
            } else if (this.step.requiresAccountsClosed() && state.accountsOpen()) {
                this.blockReason = MenuTour.BlockReason.CLOSE_ACCOUNTS;
            } else if (this.step.isAccountsStep() && !state.accountsOpen()) {
                this.blockReason = MenuTour.BlockReason.OPEN_ACCOUNTS;
            } else {
                this.blockReason = MenuTour.BlockReason.NONE;
            }

            if (this.blockReason == MenuTour.BlockReason.NONE) {
                switch (this.step) {
                    case ASK:
                    case SETTINGS:
                    default:
                        break;
                    case CHANGELOG:
                        if (state.changelogOpen()) {
                            this.changelogSeen = true;
                            return;
                        }

                        if (this.changelogSeen) {
                            this.advance(state);
                            return;
                        }
                        break;
                    case ACCOUNTS_OPEN:
                        if (state.accountsOpen()) {
                            this.advance(state);
                            return;
                        }
                        break;
                    case ACCOUNTS_ADD:
                        if (this.lastAccountCount >= 0 && state.accounts() > this.lastAccountCount) {
                            this.advance(state);
                            return;
                        }
                        break;
                    case ACCOUNTS_FAV:
                        if (this.lastFavoriteCount >= 0 && state.favorites() != this.lastFavoriteCount) {
                            this.advance(state);
                            return;
                        }
                        break;
                    case ACCOUNTS_DEL:
                        if (this.lastAccountCount >= 0 && state.accounts() < this.lastAccountCount) {
                            this.advance(state);
                            return;
                        }
                        break;
                    case ACCOUNTS_CLOSE:
                        if (!state.accountsOpen()) {
                            this.advance(state);
                        }
                }
            }
        }
    }

    private void advance(MenuTour.State state) {
        this.changelogSeen = false;
        MenuTour.Step[] steps = MenuTour.Step.values();
        int i = this.step.ordinal() + 1;
        if (i >= steps.length) {
            this.finish();
        } else {
            this.step = steps[i];
            this.lastAccountCount = state == null ? -1 : state.accounts();
            if (state == null) {
                this.lastFavoriteCount = -1;
            } else {
                this.lastFavoriteCount = state.favorites();
            }
        }
    }

    public void finish() {
        if (this.active) {
            this.active = false;
            WelcomePrefs.markTourCompleted();
            this.showAnimation.animate(0.0, 0.25, Easings.QUAD_OUT);
        }
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float[] floats, float f2, MenuTour.Placement placement, float[] floats2) {
        this.showAnimation.update();
        float f3 = this.showAnimation.get();
        if (this.active || !(f3 <= 0.01F)) {
            float f4 = f * f3;
            float f5 = GuiScale.getScaledWidth();
            float f6 = GuiScale.getScaledHeight();
            boolean flag = floats != null && floats[2] > 0.0F;
            if (flag) {
                float f7 = 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 320.0);
                float f8 = 4.0F + 1.5F * f7;
                float f9 = floats[0] - f8;
                float f10 = floats[1] - f8;
                float f11 = floats[2] + f8 * 2.0F;
                float f12 = floats[3] + f8 * 2.0F;
                Radius radius = Radius.all(Math.min(f11, f12) / 2.0F);
                new RenderUtil(drawContext, f9, f10, f11, f12).border(7.0F, Theme.getPrimary(), (0.2F + 0.16F * f7) * f4, radius);
                new RenderUtil(drawContext, f9, f10, f11, f12).shadow(1.0F, Theme.withAlpha(Theme.getPrimary(), (0.5F + 0.3F * f7) * f4), radius);
            }

            List<String> strings = this.wrapText(this.blockReason == MenuTour.BlockReason.NONE ? this.getStepBody() : this.getBlockedBody(), 176.0F);
            float f13 = 21.0F + strings.size() * 8.0F + 8.0F + 18.0F + 10.0F - 6.0F;
            float[] floats3 = floats2 != null ? floats2 : floats;
            float f14 = flag ? Math.min(floats3[0] + floats3[2] + 12.0F, f5 - 6.0F - 196.0F) : 0.0F;
            boolean flag2 = flag && placement == MenuTour.Placement.RIGHT && f14 >= floats3[0] + floats3[2] + 4.0F;
            float f15;
            float f16;
            if (flag2) {
                f15 = f14;
                f16 = Math.max(6.0F, Math.min(floats[1] - 4.0F, f6 - 6.0F - f13));
            } else if (flag) {
                f15 = Math.max(6.0F, Math.min(floats[0] + floats[2] / 2.0F - 98.0F, f5 - 6.0F - 196.0F));
                f16 = floats[1] + floats[3] + 10.0F;
                if (f16 + f13 > f6 - 6.0F) {
                    f16 = Math.max(6.0F, floats[1] - 10.0F - f13);
                }
            } else {
                f15 = (f5 - 196.0F) / 2.0F;
                f16 = Math.max(6.0F, Math.min(f2 - f13, f6 - 6.0F - f13));
            }

            this.popupX = f15;
            this.popupY = f16;
            this.popupWidth = 196.0F;
            this.popupHeight = f13;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f15 + 98.0F, f16 + f13 / 2.0F, 0.95F + 0.05F * f3);
            Radius radius2 = Radius.all(10.0F);
            new RenderUtil(drawContext, f15, f16, 196.0F, f13).border(22.0F, ColorUtil.BLACK, 0.3F * f4, radius2);
            GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MAX, true, f15, f16, 196.0F, f13, f4 * 0.9F, 10.0F);
            new RenderUtil(drawContext, f15, f16, 196.0F, f13).rect(Theme.withAlpha(ColorUtil.BLACK, 0.4F * f4), radius2);
            int i3 = Theme.withAlpha(Theme.getTextColor(), f4);
            String string = this.blockReason == MenuTour.BlockReason.NONE ? this.getStepTitle() : this.getBlockedTitle();
            new RenderUtil(drawContext, f15 + 10.0F, f16 + 10.0F, 196.0F, 10.0F)
                .textTopLeft(
                    Fonts.INTER_SEMIBOLD,
                    this.gradientText.applyThemed(Fonts.INTER_SEMIBOLD, string, 7.5F, Theme.getPrimary(), i3, 1.0F),
                    0.0F,
                    0.0F,
                    i3,
                    7.5F
                );
            if (this.step != MenuTour.Step.ASK && this.blockReason == MenuTour.BlockReason.NONE) {
                new RenderUtil(drawContext, f15, f16 + 10.0F, 186.0F, 10.0F)
                    .textTopRight(
                        Fonts.INTER_REGULAR, this.step.ordinal() + "/" + STEP_COUNT, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.6F * f4), 6.0F
                    );
            }

            float f17 = f16 + 10.0F + 13.0F;

            for (String string2 : strings) {
                new RenderUtil(drawContext, f15 + 10.0F, f17, 196.0F, 8.0F)
                    .textTopLeft(Fonts.INTER_REGULAR, string2, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.9F * f4), 6.0F);
                f17 += 8.0F;
            }

            this.nextButtonHeight = 18.0F;
            this.nextButtonWidth = this.blockReason == MenuTour.BlockReason.NONE ? 68.0F : 0.0F;
            this.nextButtonX = f15 + 196.0F - 10.0F - Math.max(this.nextButtonWidth, 0.0F);
            this.nextButtonY = f16 + f13 - 10.0F - this.nextButtonHeight + 4.0F;
            boolean flag3;
            if (this.nextButtonWidth > 0.0F) {
                flag3 = this.isHovered(i, i2, this.nextButtonX, this.nextButtonY, this.nextButtonWidth, this.nextButtonHeight);
            } else {
                flag3 = false;
            }

            if (this.nextButtonWidth > 0.0F) {
                new RenderUtil(drawContext, this.nextButtonX, this.nextButtonY, this.nextButtonWidth, this.nextButtonHeight)
                    .rect(
                        Theme.withAlpha(
                            this.step == MenuTour.Step.ASK ? Theme.getPrimary() : ColorUtil.WHITE,
                            (flag3 ? (this.step == MenuTour.Step.ASK ? 0.3F : 0.13F) : (this.step == MenuTour.Step.ASK ? 0.18F : 0.07F)) * f4
                        ),
                        Radius.all(7.0F)
                    )
                    .textCenter(
                        Fonts.INTER_MEDIUM,
                        this.getNextButtonLabel(),
                        Theme.withAlpha(!flag3 && this.step != MenuTour.Step.ASK ? Theme.getSecondaryText() : Theme.getTextColor(), f4),
                        6.5F
                    );
            }

            String string3 = this.step == MenuTour.Step.ASK ? Language.translate("tour.decline", "Не сейчас") : Language.translate("tour.skip", "Пропустить");
            this.skipButtonWidth = Fonts.INTER_REGULAR.getWidth(string3, 6.0F) + 6.0F;
            this.skipButtonHeight = this.nextButtonHeight;
            this.skipButtonX = f15 + 10.0F;
            this.skipButtonY = this.nextButtonY;
            boolean flag4 = this.isHovered(i, i2, this.skipButtonX, this.skipButtonY, this.skipButtonWidth, this.skipButtonHeight);
            new RenderUtil(drawContext, this.skipButtonX, this.skipButtonY, this.skipButtonWidth, this.skipButtonHeight)
                .textMiddleLeft(Fonts.INTER_REGULAR, string3, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), (flag4 ? 0.9F : 0.55F) * f4), 6.0F);
            MatrixUtil.pop(drawContext);
        }
    }

    public boolean mouseClicked(double d, double d2, MenuTour.State state) {
        if (!this.active) {
            return false;
        }

        if (this.isHovered(d, d2, this.skipButtonX, this.skipButtonY, this.skipButtonWidth, this.skipButtonHeight)) {
            this.finish();
            return true;
        }

        if (this.isHovered(d, d2, this.nextButtonX, this.nextButtonY, this.nextButtonWidth, this.nextButtonHeight)) {
            if (this.step == MenuTour.Step.SETTINGS) {
                this.finish();
                return true;
            } else {
                this.advance(state);
                return true;
            }
        } else {
            return this.isHovered(d, d2, this.popupX, this.popupY, this.popupWidth, this.popupHeight);
        }
    }

    private String getBlockedTitle() {
        return this.blockReason == MenuTour.BlockReason.OPEN_ACCOUNTS
            ? Language.translate("tour.hold.open.title", "Вернитесь в аккаунты")
            : Language.translate("tour.close.title", "Возврат в меню");
    }

    private String getBlockedBody() {
        switch (this.blockReason) {
            case CLOSE_CHANGELOG:
                return Language.translate("tour.blocked.changelog.body", "Сверните панель язычком, Esc или кликом мимо — продолжим обучение.");
            case OPEN_ACCOUNTS:
                return Language.translate("tour.hold.open.body", "Откройте «Аккаунты» — продолжим с того же места.");
            default:
                return Language.translate("tour.blocked.body", "Закройте панель крестиком или Esc — продолжим обучение.");
        }
    }

    private String getNextButtonLabel() {
        switch (this.step) {
            case ASK:
                return Language.translate("tour.accept", "Провести");
            case SETTINGS:
                return Language.translate("tour.done", "Готово");
            default:
                return Language.translate("tour.next", "Понятно");
        }
    }

    private String getStepTitle() {
        switch (this.step) {
            case ASK:
                return Language.translate("tour.ask.title", "Показать клиент?");
            case CHANGELOG:
                return Language.translate("tour.changelog.title", "Список изменений");
            case ACCOUNTS_OPEN:
                return Language.translate("tour.accounts.title", "Менеджер аккаунтов");
            case ACCOUNTS_ADD:
                return Language.translate("tour.add.title", "Добавьте аккаунт");
            case ACCOUNTS_FAV:
                return Language.translate("tour.fav.title", "Избранное");
            case ACCOUNTS_DEL:
                return Language.translate("tour.del.title", "Удаление");
            case ACCOUNTS_CLOSE:
                return Language.translate("tour.close.title", "Возврат в меню");
            case SETTINGS:
                return Language.translate("tour.settings.title", "Настройки");
            default:
                throw new MatchException(null, null);
        }
    }

    private String getStepBody() {
        switch (this.step) {
            case ASK:
                return Language.translate("tour.ask.body", "Коротко покажем главное: список изменений, менеджер аккаунтов и настройки. Займёт минуту.");
            case CHANGELOG:
                return Language.translate(
                    "tour.changelog.body", "Здесь мы пишем, что нового в клиенте. Потяните за язычок слева — свернёте тем же язычком, Esc или кликом мимо."
                );
            case ACCOUNTS_OPEN:
                return Language.translate("tour.accounts.body", "Нажмите «Аккаунты» — покажем, как ими пользоваться.");
            case ACCOUNTS_ADD:
                return Language.translate(
                    "tour.add.body", "Введите ник и нажмите «+». Кубик рядом придумает случайный ник, а квадрат Microsoft добавит лицензионный аккаунт."
                );
            case ACCOUNTS_FAV:
                return Language.translate(
                    "tour.fav.body", "Звезда добавляет аккаунт в избранное — он появится вверху панели для быстрого входа. Повторный клик уберёт его оттуда."
                );
            case ACCOUNTS_DEL:
                return Language.translate("tour.del.body", "Мусорка удаляет аккаунт. Кнопка вверху справа чистит список целиком, но избранные не трогает.");
            case ACCOUNTS_CLOSE:
                return Language.translate("tour.close.body", "Готово. Закройте панель крестиком — остался последний шаг.");
            case SETTINGS:
                return Language.translate(
                    "tour.settings.body", "Настройки Minecraft прямо в клиенте: слева категории, сверху поиск. Нужны ванильные — кнопка справа снизу."
                );
            default:
                throw new MatchException(null, null);
        }
    }

    private List<String> wrapText(String string, float f) {
        ArrayList arrayList = new ArrayList();
        StringBuilder builder = new StringBuilder();

        for (String string2 : string.split(" ")) {
            String string3 = builder.isEmpty() ? string2 : builder + " " + string2;
            if (!builder.isEmpty() && Fonts.INTER_REGULAR.getWidth(string3, 6.0F) > f) {
                arrayList.add(builder.toString());
                builder.setLength(0);
                builder.append(string2);
            } else {
                builder.setLength(0);
                builder.append(string3);
            }
        }

        if (!builder.isEmpty()) {
            arrayList.add(builder.toString());
        }

        return arrayList;
    }

    public enum BlockReason {
        NONE,
        CLOSE_CHANGELOG,
        CLOSE_ACCOUNTS,
        OPEN_ACCOUNTS;
    }

    public enum Placement {
        BELOW,
        RIGHT;
    }

    public record State(boolean changelogOpen, boolean accountsOpen, int accounts, int favorites) {
    }

    public enum Step {
        ASK,
        CHANGELOG,
        ACCOUNTS_OPEN,
        ACCOUNTS_ADD,
        ACCOUNTS_FAV,
        ACCOUNTS_DEL,
        ACCOUNTS_CLOSE,
        SETTINGS;

        boolean isAccountsStep() {
            return this == ACCOUNTS_ADD || this == ACCOUNTS_FAV || this == ACCOUNTS_DEL;
        }

        boolean requiresAccountsClosed() {
            return this == CHANGELOG || this == SETTINGS;
        }
    }
}
