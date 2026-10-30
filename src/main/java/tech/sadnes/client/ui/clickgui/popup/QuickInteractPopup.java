package tech.sadnes.client.ui.clickgui.popup;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.feature.impl.misc.QuickInteract;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class QuickInteractPopup extends Component {
    private static final float HEADER_HEIGHT = 24.0F;
    private static final float ROW_HEIGHT = 20.0F;
    private static final float PADDING = 8.0F;
    private static final float TITLE_SIZE = 7.0F;
    private static final float CLOSE_SIZE = 8.0F;
    private static final float WIDTH = 126.0F;
    private static final float SCREEN_MARGIN = 4.0F;
    private static final float CORNER_RADIUS = 8.0F;
    private static final float BOTTOM_PADDING = 2.0F;
    private final QuickInteract chatActions;
    private final Animation openAnimation = new Animation();
    private final List<QuickInteractPopup.InteractAction> actions = new ArrayList<>(QuickInteractPopup.InteractAction.values().length);
    private final Map<QuickInteractPopup.InteractAction, Animation> hoverAnimations = new EnumMap<>(QuickInteractPopup.InteractAction.class);
    private String playerName;
    private boolean closing;
    private int screenWidth = Integer.MAX_VALUE;
    private int screenHeight = Integer.MAX_VALUE;

    public QuickInteractPopup(QuickInteract quickInteract) {
        this.chatActions = quickInteract;
    }

    public boolean isOpen() {
        return this.playerName != null && !this.closing;
    }

    public boolean isVisible() {
        return this.openAnimation.get() > 0.001;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public void open(String string, float f, float f2, int i, int i2) {
        if (string != null) {
            if (!string.equals(this.playerName) || this.closing) {
                this.playerName = string;
                this.closing = false;
                this.screenWidth = i;
                this.screenHeight = i2;
                this.rebuildActions();
                this.size.set(126.0F, 24.0F + 20.0F * this.actions.size() + 2.0F);
                this.position.set(this.clampToScreen(f, this.size.x, this.screenWidth), this.clampToScreen(f2, this.size.y, this.screenHeight));
                this.openAnimation.animate(1.0, 0.3, Easings.BACK_OUT);
            }
        }
    }

    private void rebuildActions() {
        this.actions.clear();
        this.hoverAnimations.clear();
        List list = this.chatActions.getActionsFor(this.playerName);

        for (QuickInteractPopup.InteractAction interactAction : QuickInteractPopup.InteractAction.values()) {
            QuickInteract.QuickAction quickAction = this.toChatAction(interactAction);
            if (quickAction == null || list.contains(quickAction)) {
                this.actions.add(interactAction);
            }
        }
    }

    private QuickInteract.QuickAction toChatAction(QuickInteractPopup.InteractAction interactAction) {
        switch (interactAction) {
            case ACCEPT_TP:
                return QuickInteract.QuickAction.ACCEPT_TELEPORT;
            case DECLINE_TP:
                return QuickInteract.QuickAction.DECLINE_TELEPORT;
            case ACCEPT_DUEL:
                return QuickInteract.QuickAction.ACCEPT;
            case DECLINE_DUEL:
                return QuickInteract.QuickAction.DECLINE;
            case CANCEL_TELEPORT:
                return QuickInteract.QuickAction.CANCEL_TELEPORT;
            case DUEL:
            default:
                return null;
            case CANCEL_DUEL:
                return QuickInteract.QuickAction.CANCEL_DUEL;
        }
    }

    @Override
    public void close() {
        if (this.playerName != null) {
            this.closing = true;
            this.openAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
        }
    }

    public void reset() {
        this.playerName = null;
        this.closing = false;
        this.openAnimation.forceValue(0.0);
    }

    public boolean isMouseOver(double d, double d2) {
        return this.isVisible() && this.playerName != null && this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        super.render(drawContext, i, i2, f);
        this.openAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001) {
            this.playerName = null;
            this.closing = false;
        } else if (this.playerName != null) {
            float f2 = this.openAnimation.get();
            float f3 = Math.min(f2, 1.0F) * this.getAlpha();
            float f4 = this.position.x;
            float f5 = this.position.y;
            float f6 = this.size.x;
            float f7 = this.size.y;
            float f8 = 0.9F + 0.1F * f2;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f4 + f6 / 2.0F, f5 + f7 / 2.0F, f8);
            GuiRenderUtil.drawPopup(drawContext, f4, f5, f6, f7, f3, 8.0F);
            this.renderHeader(drawContext, f4, f5, f6, f3, i, i2);
            this.renderActions(drawContext, f4, f5, f6, f3, i, i2);
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderHeader(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f4);
        new RenderUtil(drawContext, f, f2, f3, 24.0F).textMiddleLeft(Fonts.INTER_SEMIBOLD, this.playerName, 8.0F, 0.0F, i3, 7.0F);
        float f5 = f + f3 - 8.0F - 8.0F + 2.0F;
        float f6 = f2 + 12.0F - 4.0F;
        boolean flag = this.isHovered(i, i2, f + f3 - 24.0F, f2, 24.0, 24.0);
        int i4 = ColorUtil.multiplyAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f4);
        new RenderUtil(drawContext, f5, f6, 8.0F, 8.0F).icon(Icon.CLOSE, 0.0F, 0.0F, 8.0F, i4);
        GuiRenderUtil.drawOverlay(drawContext, f + 8.0F, MathUtil.roundToStep(f2 + 24.0F, 0.5F), f3 - 16.0F, 0.5F, f4);
    }

    private void renderActions(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        float f5 = f2 + 24.0F;
        int i3 = this.actions.size();

        for (int i4 = 0; i4 < i3; i4++) {
            QuickInteractPopup.InteractAction interactAction = this.actions.get(i4);
            if (i4 > 0) {
                GuiRenderUtil.drawOverlay(drawContext, f + 8.0F, MathUtil.roundToStep(f5, 0.5F), f3 - 16.0F, 0.5F, f4);
            }

            boolean flag;
            if (interactAction == QuickInteractPopup.InteractAction.PRIORITY) {
                flag = TargetComponent.isPriority(this.playerName);
            } else {
                flag = false;
            }

            float f6 = this.updateHoverAnimation(interactAction, this.isHovered(i, i2, f, f5, f3, 20.0));
            PopupMenuItemRenderer.renderItem(drawContext, f, f5, f3, this.getActionIcon(interactAction), this.getActionLabel(interactAction), f6, flag, f4);
            f5 += 20.0F;
        }
    }

    private float updateHoverAnimation(QuickInteractPopup.InteractAction interactAction, boolean flag) {
        Animation animation = this.hoverAnimations.computeIfAbsent(interactAction, interactAction2 -> new Animation());
        animation.update();
        if (flag) {
            animation.animate(1.0, 0.16, Easings.QUAD_OUT);
            return animation.get();
        } else {
            animation.animate(0.0, 0.16, Easings.QUAD_OUT);
            return animation.get();
        }
    }

    private Icon getActionIcon(QuickInteractPopup.InteractAction interactAction) {
        QuickInteract.QuickAction quickAction = this.toChatAction(interactAction);
        if (quickAction != null) {
            return quickAction.getIcon();
        }

        switch (interactAction) {
            case FRIEND:
                if (this.chatActions.isFriend(this.playerName)) {
                    return Icon.USER_MINUS;
                }

                return Icon.USER_PLUS;
            case DUEL:
                return Icon.SWORDS;
            case REPORT:
                return Icon.FLAG;
            default:
                return Icon.CROSSHAIRS;
        }
    }

    private String getActionLabel(QuickInteractPopup.InteractAction interactAction) {
        QuickInteract.QuickAction quickAction = this.toChatAction(interactAction);
        if (quickAction != null) {
            return quickAction.getDisplayName();
        }

        switch (interactAction) {
            case FRIEND:
                if (this.chatActions.isFriend(this.playerName)) {
                    return Language.translate("quickinteract.remove friend", "Remove friend");
                }

                return Language.translate("quickinteract.add friend", "Add friend");
            case DUEL:
                return Language.translate("quickinteract.duel", "Challenge to duel");
            case REPORT:
                return Language.translate("quickinteract.report", "Send report");
            default:
                return Language.translate("quickinteract.priority", "Target priority");
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.playerName != null && !this.closing) {
            double d = click.x();
            double d2 = click.y();
            if (this.isHovered(d, d2, this.position.x + this.size.x - 24.0F, this.position.y, 24.0, 24.0)) {
                this.close();
                return true;
            }

            if (this.isLeftButton(click.button())) {
                float f = this.position.y + 24.0F;
                int i = this.actions.size();

                for (int i2 = 0; i2 < i; i2++) {
                    if (this.isHovered(d, d2, this.position.x, f, this.size.x, 20.0)) {
                        this.executeAction(this.actions.get(i2));
                        return true;
                    }

                    f += 20.0F;
                }
            }

            return this.isMouseOver(d, d2);
        } else {
            return false;
        }
    }

    private void executeAction(QuickInteractPopup.InteractAction interactAction) {
        QuickInteract.QuickAction quickAction = this.toChatAction(interactAction);
        if (quickAction != null) {
            this.chatActions.performAction(quickAction);
        } else {
            switch (interactAction) {
                case FRIEND:
                    this.chatActions.toggleFriend(this.playerName);
                    return;
                case DUEL:
                    this.chatActions.sendDuel(this.playerName);
                    return;
                case REPORT:
                    this.chatActions.sendReport(this.playerName);
                    return;
                default:
                    TargetComponent.togglePriority(this.playerName);
            }
        }
    }

    private float clampToScreen(float f, float f2, int i) {
        if (i > 0 && i != Integer.MAX_VALUE) {
            float f3 = Math.max(4.0F, i - 4.0F - f2);
            return f3 < 4.0F ? 4.0F : Math.max(4.0F, Math.min(f, f3));
        } else {
            return f;
        }
    }

    @Override
    public void resize(int i, int i2) {
        this.screenWidth = i;
        this.screenHeight = i2;
    }

    @Override
    public void init(int i, int i2) {
        this.screenWidth = i;
        this.screenHeight = i2;
    }

    @Override
    public void mouseMoved(double d, double d2) {
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
        return this.isMouseOver(d, d2);
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
    public void removed() {
    }

    enum InteractAction {
        FRIEND,
        ACCEPT_TP,
        DECLINE_TP,
        ACCEPT_DUEL,
        DECLINE_DUEL,
        CANCEL_TELEPORT,
        DUEL,
        CANCEL_DUEL,
        REPORT,
        PRIORITY;
    }
}
