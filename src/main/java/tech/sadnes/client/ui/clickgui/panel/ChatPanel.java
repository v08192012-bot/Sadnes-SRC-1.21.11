package tech.sadnes.client.ui.clickgui.panel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class ChatPanel extends Component {
    private static final float WIDTH = 188.0F;
    private static final float HEIGHT = 152.0F;
    private static final float PADDING = 8.0F;
    private static final float HEADER_HEIGHT = 15.0F;
    private static final float ICON_SIZE = 8.0F;
    private static final float SEARCH_HEIGHT = 13.0F;
    private static final float TAB_HEIGHT = 11.0F;
    private static final float INPUT_HEIGHT = 15.0F;
    private static final float ROW_HEIGHT = 24.0F;
    private static final int ONLINE_COLOR = ColorUtil.rgb(76, 205, 128);
    private static final TextFieldComponent SEARCH_FIELD = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.5F, "chat.search", false, false);
    private static final TextFieldComponent MESSAGE_FIELD = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.5F, "chat.message", false, false);
    private final Animation openAnimation = new Animation();
    private final SmoothScrollbar listScroll = new SmoothScrollbar().setAutoHide(true);
    private final SmoothScrollbar messageScroll = new SmoothScrollbar().setAutoHide(true);
    private final List<ChatPanel.Conversation> conversations = new ArrayList<>();
    private boolean open;
    private boolean closing;
    private boolean positioned;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private ChatPanel.Tab selectedTab = ChatPanel.Tab.CHATS;
    private int openConversationIndex = -1;
    private boolean scrollToBottom;
    private float closeButtonX;
    private float closeButtonY;
    private float backButtonX;
    private float backButtonY;
    private float searchX;
    private float searchY;
    private float searchWidth;
    private float tabsX;
    private float tabsY;
    private float tabsWidth;
    private float tabWidth;
    private float listY;
    private float listHeight;
    private float messagesTop;
    private float messagesBottom;
    private float inputX;
    private float inputY;
    private float inputWidth;
    private float sendButtonX;
    private float sendButtonY;

    public boolean isOpen() {
        return this.open && !this.closing;
    }

    public boolean isVisible() {
        return this.openAnimation.get() > 0.001;
    }

    public boolean isMouseOver(double d, double d2) {
        return this.isVisible() && this.isHovered(d, d2, this.position.x, this.position.y, 188.0, 152.0);
    }

    public void open() {
        this.open = true;
        this.closing = false;
        if (!this.positioned) {
            this.position.set((GuiScale.getScaledWidth() - 188.0F) / 2.0F, (GuiScale.getScaledHeight() - 152.0F) / 2.0F);
            this.positioned = true;
        }

        this.listScroll.reset();
        this.messageScroll.reset();
        this.openConversationIndex = -1;
        this.dragging = false;
        SEARCH_FIELD.setText("");
        SEARCH_FIELD.setFocused(false);
        MESSAGE_FIELD.setText("");
        MESSAGE_FIELD.setFocused(false);
        if (this.conversations.isEmpty()) {
            this.populateSampleConversations();
        }

        this.openAnimation.animate(1.0, 0.3, Easings.QUART_OUT);
    }

    @Override
    public void close() {
        if (this.open && !this.closing) {
            this.closing = true;
            this.dragging = false;
            SEARCH_FIELD.setFocused(false);
            MESSAGE_FIELD.setFocused(false);
            this.openAnimation.animate(0.0, 0.22, Easings.QUAD_IN);
        }
    }

    public void reset() {
        this.open = false;
        this.closing = false;
        this.dragging = false;
        SEARCH_FIELD.setFocused(false);
        MESSAGE_FIELD.setFocused(false);
        this.openAnimation.forceValue(0.0);
    }

    private void populateSampleConversations() {
        ChatPanel.Conversation conversation = new ChatPanel.Conversation("elytrapise4ka", true, 2);
        conversation.messages.add(new ChatPanel.Message(false, "привет ✨ как сам?"));
        conversation.messages.add(new ChatPanel.Message(true, "норм \ud83d\udd25 тестим чат"));
        conversation.messages.add(new ChatPanel.Message(false, "выглядит как телега \ud83d\ude0e"));
        ChatPanel.Conversation conversation2 = new ChatPanel.Conversation("duck_fan", true, 0);
        conversation2.messages.add(new ChatPanel.Message(false, "\ud83e\udd86\ud83e\udd86\ud83e\udd86"));
        conversation2.messages.add(new ChatPanel.Message(true, "утки топ"));
        ChatPanel.Conversation conversation3 = new ChatPanel.Conversation("Sadne$$", false, 0);
        conversation3.messages.add(new ChatPanel.Message(true, "го катку?"));
        ChatPanel.Conversation conversation4 = new ChatPanel.Conversation("mouse_xd", false, 5);
        conversation4.messages.add(new ChatPanel.Message(false, "прочитай личку когда сможешь ❤️"));
        this.conversations.add(conversation);
        this.conversations.add(conversation2);
        this.conversations.add(conversation3);
        this.conversations.add(conversation4);
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.openAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001) {
            this.open = false;
            this.closing = false;
        }

        if (this.open && !(this.openAnimation.get() <= 0.001) && Fonts.INTER_MEDIUM != null) {
            float f2 = this.position.x;
            float f3 = this.position.y;
            this.size.set(188.0F, 152.0F);
            float f4 = (float)Math.min(this.openAnimation.get(), 1.0);
            float f5 = f4 * this.getAlpha();
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, 188.0F, 152.0F);
            renderUtil.blur(f5, Radius.all(9.0F), RenderUtil.BlurQuality.LOW, true);
            renderUtil.rect(Theme.withAlpha(Theme.getBackground(), 0.4F), f5, Radius.all(9.0F));
            new RenderUtil(drawContext, f2, f3 + 15.0F, 188.0F, 0.5F).rect(Theme.withAlpha(Theme.getOutline(), 0.4F * f5));
            this.renderHeader(drawContext, i, i2, f2, f3, f5);
            if (this.openConversationIndex < 0) {
                this.renderConversationList(drawContext, i, i2, f2, f3, f5);
            } else {
                this.renderConversationView(drawContext, i, i2, f2, f3, f5);
            }
        }
    }

    private void renderHeader(DrawContext drawContext, int i, int i2, float f, float f2, float f3) {
        this.closeButtonX = f + 188.0F - 6.0F - 8.0F;
        this.closeButtonY = f2 + 3.5F;
        boolean flag = this.isHovered(i, i2, this.closeButtonX - 2.0F, this.closeButtonY - 2.0F, 12.0, 12.0);
        new RenderUtil(drawContext, this.closeButtonX, this.closeButtonY, 8.0F, 8.0F)
            .icon(Icon.CLOSE, 0.0F, 0.0F, 8.0F, Theme.withAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f3));
        if (this.openConversationIndex < 0) {
            new RenderUtil(drawContext, f + 8.0F, f2, 172.0F, 15.0F)
                .textMiddleLeft(Fonts.INTER_BOLD, Language.translate("chat.title", "Chat"), 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f3), 8.0F);
        } else {
            ChatPanel.Conversation conversation = this.conversations.get(this.openConversationIndex);
            this.backButtonX = f + 6.0F;
            this.backButtonY = f2 + 3.5F;
            boolean flag2 = this.isHovered(i, i2, this.backButtonX - 2.0F, this.backButtonY - 2.0F, 12.0, 12.0);
            RenderUtil.drawChevron(
                drawContext, this.backButtonX, this.backButtonY, 6.0F, 180.0F, Theme.withAlpha(flag2 ? Theme.getTextColor() : Theme.getSecondaryText(), f3)
            );
            new RenderUtil(drawContext, f + 6.0F + 8.0F + 3.0F, f2, 151.0F, 15.0F)
                .textMiddleLeft(Fonts.INTER_SEMIBOLD, conversation.name, 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f3), 7.5F);
            if (conversation.online) {
                new RenderUtil(drawContext, this.closeButtonX - 9.0F, f2 + 7.5F - 2.0F, 4.0F, 4.0F).rect(Theme.withAlpha(ONLINE_COLOR, f3), Radius.all(2.0F));
            }
        }
    }

    private void renderConversationList(DrawContext drawContext, int i, int i2, float f, float f2, float f3) {
        this.searchX = f + 8.0F;
        this.searchY = f2 + 15.0F + 3.0F;
        this.searchWidth = 172.0F;
        GuiRenderUtil.drawOutlined(drawContext, this.searchX, this.searchY, this.searchWidth, 13.0F, f3 * 0.5F, 4.3333335F);
        if (SEARCH_FIELD.isFocused()) {
            new RenderUtil(drawContext, this.searchX, this.searchY, this.searchWidth, 13.0F)
                .shadow(1.0F, Theme.withAlpha(Theme.getPrimary(), 0.7F * f3), Radius.all(4.3333335F));
        }

        new RenderUtil(drawContext, this.searchX + 4.0F, this.searchY + 6.5F - 4.0F, 8.0F, 8.0F)
            .icon(Icon.SEARCH, 0.0F, 0.0F, 8.0F, Theme.withAlpha(Theme.getSecondaryText(), f3 * 0.7F));
        float f4 = this.searchX + 4.0F + 8.0F + 4.0F;
        SEARCH_FIELD.render(
            drawContext, new Vector2f(f4, this.searchY + 6.5F - 3.25F), Theme.withAlpha(Theme.getTextColor(), f3), this.searchX + this.searchWidth - 4.0F - f4
        );
        ChatPanel.Tab[] tabs = ChatPanel.Tab.values();
        String[] strings = new String[]{
            Language.translate("chat.tab.chats", "Chats"),
            Language.translate("chat.tab.friends", "Friends"),
            Language.translate("chat.tab.requests", "Requests")
        };
        this.tabsX = f + 8.0F;
        this.tabsY = this.searchY + 13.0F + 3.0F;
        this.tabsWidth = 172.0F;
        this.tabWidth = this.tabsWidth / tabs.length;

        for (int i3 = 0; i3 < tabs.length; i3++) {
            boolean flag = this.selectedTab == tabs[i3];
            float f5 = this.tabsX + i3 * this.tabWidth;
            int i4 = flag ? Theme.withAlpha(Theme.getPrimary(), f3) : Theme.withAlpha(Theme.getSecondaryText(), 0.8F * f3);
            new RenderUtil(drawContext, f5, this.tabsY, this.tabWidth, 11.0F).textCenter(Fonts.INTER_MEDIUM, strings[i3], i4, 6.0F);
            if (flag) {
                new RenderUtil(drawContext, f5 + this.tabWidth * 0.25F, this.tabsY + 11.0F - 1.0F, this.tabWidth * 0.5F, 1.0F)
                    .rect(Theme.withAlpha(Theme.getPrimary(), f3), Radius.all(1.0F));
            }
        }

        this.listY = this.tabsY + 11.0F + 2.0F;
        this.listHeight = f2 + 152.0F - this.listY - 5.0F;
        List list = this.getFilteredConversations();
        if (this.selectedTab != ChatPanel.Tab.REQUESTS && !list.isEmpty()) {
            float f6 = list.size() * 24.0F;
            this.listScroll.update(f6, this.listHeight);
            this.listScroll.updateHover(i, i2);
            boolean flag2 = ScissorStack.push(drawContext, f, this.listY, 188.0F, this.listHeight);

            try {
                float f7 = this.listScroll.getScroll();

                for (int i5 = 0; i5 < list.size(); i5++) {
                    float f8 = this.listY + f7 + i5 * 24.0F;
                    if (!(f8 + 24.0F < this.listY) && !(f8 > this.listY + this.listHeight)) {
                        this.renderConversationRow(drawContext, f + 6.0F, f8, 176.0F, i, i2, f3, (ChatPanel.Conversation)list.get(i5));
                    }
                }
            } finally {
                if (flag2) {
                    ScissorStack.pop(drawContext);
                }
            }

            this.listScroll.render(drawContext, f + 188.0F - 4.0F, this.listY, this.listHeight, f6, f3);
        } else {
            String string = this.selectedTab == ChatPanel.Tab.REQUESTS
                ? Language.translate("chat.empty_list", "Nothing here")
                : Language.translate("chat.empty_list", "Nothing here");
            new RenderUtil(drawContext, f, this.listY, 188.0F, this.listHeight)
                .textCenter(Fonts.INTER_MEDIUM, string, Theme.withAlpha(Theme.getSecondaryText(), 0.75F * f3), 6.5F);
        }
    }

    private void renderConversationRow(DrawContext drawContext, float f, float f2, float f3, int i, int i2, float f4, ChatPanel.Conversation conversation) {
        boolean flag = this.openConversationIndex >= 0 && this.conversations.indexOf(conversation) == this.openConversationIndex;
        boolean flag2 = this.isHovered(i, i2, f, f2, f3, 24.0);
        if (flag) {
            new RenderUtil(drawContext, f, f2, f3, 24.0F).rect(Theme.withAlpha(Theme.getPrimary(), 0.16F * f4), Radius.all(6.0F));
        } else if (flag2) {
            GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, 24.0F, f4, Radius.all(6.0F));
        }

        float f5 = f + 4.0F;
        float f6 = f2 + (24.0F - 17.0F) / 2.0F;
        this.renderAvatar(drawContext, f5, f6, 17.0F, f4);
        if (conversation.online) {
            new RenderUtil(drawContext, f5 + 17.0F - 5.0F, f6 + 17.0F - 5.0F, 5.0F, 5.0F).rect(Theme.withAlpha(ONLINE_COLOR, f4), Radius.all(2.5F));
        }

        float f7 = f5 + 17.0F + 5.0F;
        float f8 = f + f3 - f7 - 5.0F;
        new RenderUtil(drawContext, f7, f2 + 5.0F, f8, 8.0F)
            .textTopLeft(Fonts.INTER_SEMIBOLD, conversation.name, 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f4), 6.5F);
        String string = this.selectedTab == ChatPanel.Tab.FRIENDS
            ? Language.translate(conversation.online ? "chat.online" : "chat.offline", conversation.online ? "online" : "offline")
            : conversation.getLastMessage();
        new RenderUtil(drawContext, f7, f2 + 13.5F, f8, 8.0F)
            .scrollingTextTop(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, f8, Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f4), 6.0F, false);
        if (conversation.unreadCount > 0 && this.selectedTab != ChatPanel.Tab.FRIENDS) {
            float f9 = Math.max(8.0F, Fonts.INTER_MEDIUM.getWidth(String.valueOf(conversation.unreadCount), 6.0F) + 5.0F);
            float f10 = f + f3 - f9 - 2.0F;
            float f11 = f2 + 8.0F;
            new RenderUtil(drawContext, f10, f11, f9, 8.0F).rect(Theme.withAlpha(Theme.getPrimary(), f4), Radius.all(4.0F));
            new RenderUtil(drawContext, f10, f11, f9, 8.0F)
                .textCenter(Fonts.INTER_SEMIBOLD, String.valueOf(conversation.unreadCount), Theme.withAlpha(ColorUtil.WHITE, f4), 6.0F);
        }
    }

    private void renderConversationView(DrawContext drawContext, int i, int i2, float f, float f2, float f3) {
        this.inputX = f + 8.0F;
        this.inputY = f2 + 152.0F - 15.0F - 5.0F;
        this.inputWidth = 154.0F;
        this.sendButtonX = this.inputX + this.inputWidth + 3.0F;
        this.sendButtonY = this.inputY;
        this.messagesTop = f2 + 15.0F + 3.0F;
        this.messagesBottom = this.inputY - 4.0F;
        this.renderMessages(drawContext, i, i2, f, f3, this.conversations.get(this.openConversationIndex));
        GuiRenderUtil.drawOutlined(drawContext, this.inputX, this.inputY, this.inputWidth, 15.0F, f3 * 0.5F, 5.0F);
        if (MESSAGE_FIELD.isFocused()) {
            new RenderUtil(drawContext, this.inputX, this.inputY, this.inputWidth, 15.0F)
                .shadow(1.0F, Theme.withAlpha(Theme.getPrimary(), 0.7F * f3), Radius.all(5.0F));
        }

        MESSAGE_FIELD.render(
            drawContext, new Vector2f(this.inputX + 6.0F, this.inputY + 7.5F - 3.25F), Theme.withAlpha(Theme.getTextColor(), f3), this.inputWidth - 12.0F
        );
        boolean flag = !MESSAGE_FIELD.isEmpty();
        boolean flag2 = this.isHovered(i, i2, this.sendButtonX, this.sendButtonY, 15.0, 15.0);
        new RenderUtil(drawContext, this.sendButtonX, this.sendButtonY, 15.0F, 15.0F)
            .rect(Theme.withAlpha(Theme.getPrimary(), (flag ? (flag2 ? 1.0F : 0.85F) : 0.35F) * f3), Radius.all(5.0F));
        new RenderUtil(drawContext, this.sendButtonX, this.sendButtonY, 15.0F, 15.0F).icon(Icon.CARET_RIGHT, 4.0F, 4.0F, 7.0F, Theme.withAlpha(ColorUtil.WHITE, f3));
    }

    private void renderMessages(DrawContext drawContext, int i, int i2, float f, float f2, ChatPanel.Conversation conversation) {
        float f3 = f + 8.0F;
        float f4 = 188.0F - 8.0F * 2.0F;
        float f5 = f4 * 0.74F;
        float f6 = this.messagesBottom - this.messagesTop;
        ArrayList arrayList = new ArrayList();
        float f7 = 0.0F;

        for (ChatPanel.Message message : conversation.messages) {
            List list = this.wrapText(message.text, f5 - 10.0F, 6.5F);
            arrayList.add(list);
            f7 += list.size() * 8.0F + 7.0F + 3.0F;
        }

        this.messageScroll.update(f7, f6);
        this.messageScroll.updateHover(i, i2);
        if (this.scrollToBottom) {
            this.messageScroll.setScroll(Math.min(0.0F, f6 - f7));
            this.scrollToBottom = false;
        }

        boolean flag = ScissorStack.push(drawContext, f, this.messagesTop, 188.0F, f6);

        try {
            float f8 = this.messagesTop + this.messageScroll.getScroll();

            for (int i3 = 0; i3 < conversation.messages.size(); i3++) {
                ChatPanel.Message message2 = conversation.messages.get(i3);
                List<String> strings = (List<String>)arrayList.get(i3);
                float f9 = 8.0F;

                for (String string : strings) {
                    f9 = Math.max(f9, Fonts.INTER_MEDIUM.getWidth(string, 6.5F) + 10.0F);
                }

                float f10 = Math.min(f9, f5);
                float f11 = strings.size() * 8.0F + 7.0F;
                float f12 = message2.mine ? f3 + f4 - f10 : f3;
                if (f8 + f11 >= this.messagesTop && f8 <= this.messagesBottom) {
                    int i4 = message2.mine ? Theme.withAlpha(Theme.getPrimary(), 0.9F * f2) : Theme.withAlpha(Theme.getOutline(), 0.35F * f2);
                    new RenderUtil(drawContext, f12, f8, f10, f11).rect(i4, Radius.smoothAll(6.0F));
                    int i5 = message2.mine ? Theme.withAlpha(ColorUtil.WHITE, f2) : Theme.withAlpha(Theme.getTextColor(), f2);

                    for (int i6 = 0; i6 < strings.size(); i6++) {
                        new RenderUtil(drawContext, f12 + 5.0F, f8 + 3.5F + i6 * 8.0F, f10 - 10.0F, 8.0F)
                            .textTopLeft(Fonts.INTER_MEDIUM, (String)strings.get(i6), 0.0F, 0.0F, i5, 6.5F);
                    }
                }

                f8 += f11 + 3.0F;
            }
        } finally {
            if (flag) {
                ScissorStack.pop(drawContext);
            }
        }

        this.messageScroll.render(drawContext, f + 188.0F - 4.0F, this.messagesTop, f6, f7, f2);
    }

    private void renderAvatar(DrawContext drawContext, float f, float f2, float f3, float f4) {
        GuiRenderUtil.bindTexture(Texture.AVATAR_DEFAULT);
        if (GuiRenderUtil.getBoundTexture() != null) {
            new RenderUtil(drawContext, f, f2, f3, f3).frostedGlass(f4, Radius.all(f3 / 2.0F));
        }
    }

    private List<ChatPanel.Conversation> getFilteredConversations() {
        String string = SEARCH_FIELD.getText();
        ArrayList<ChatPanel.Conversation> conversations2 = new ArrayList<>();

        for (ChatPanel.Conversation conversation : this.conversations) {
            if (this.selectedTab != ChatPanel.Tab.FRIENDS || !conversation.messages.isEmpty()) {
                conversations2.add(conversation);
            }
        }

        if (string != null && !string.isBlank()) {
            String string2 = string.toLowerCase();
            ArrayList arrayList = new ArrayList();

            for (ChatPanel.Conversation conversation2 : conversations2) {
                if (conversation2.name.toLowerCase().contains(string2)) {
                    arrayList.add(conversation2);
                }
            }

            return arrayList;
        } else {
            return conversations2;
        }
    }

    private List<String> wrapText(String string, float f, float f2) {
        ArrayList arrayList = new ArrayList();
        if (string != null && !string.isEmpty()) {
            String[] strings = string.split(" ");
            StringBuilder builder = new StringBuilder();

            for (String string2 : strings) {
                String string3 = builder.length() == 0 ? string2 : builder + " " + string2;
                if (Fonts.INTER_MEDIUM.getWidth(string3, f2) > f && builder.length() > 0) {
                    arrayList.add(builder.toString());
                    builder = new StringBuilder(string2);
                } else {
                    builder = new StringBuilder(string3);
                }
            }

            if (builder.length() > 0 || arrayList.isEmpty()) {
                arrayList.add(builder.toString());
            }

            return arrayList;
        } else {
            arrayList.add("");
            return arrayList;
        }
    }

    private void sendMessage() {
        if (this.openConversationIndex >= 0 && !MESSAGE_FIELD.isEmpty()) {
            this.conversations.get(this.openConversationIndex).messages.add(new ChatPanel.Message(true, MESSAGE_FIELD.getText().trim()));
            MESSAGE_FIELD.setText("");
            this.scrollToBottom = true;
        }
    }

    private void openConversation(int i) {
        this.openConversationIndex = i;
        this.conversations.get(i).unreadCount = 0;
        this.scrollToBottom = true;
        this.messageScroll.reset();
        SEARCH_FIELD.setFocused(false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        if (!this.isMouseOver(d, d2)) {
            return false;
        }

        boolean flag2 = this.isLeftButton(click.button());
        if (flag2 && this.isHovered(d, d2, this.closeButtonX - 2.0F, this.closeButtonY - 2.0F, 12.0, 12.0)) {
            this.close();
            return true;
        }

        if (this.openConversationIndex < 0) {
            if (flag2 && this.isHovered(d, d2, this.searchX, this.searchY, this.searchWidth, 13.0)) {
                SEARCH_FIELD.setFocused(true);
                MESSAGE_FIELD.setFocused(false);
                return true;
            }

            if (flag2 && this.isHovered(d, d2, this.tabsX, this.tabsY, this.tabsWidth, 11.0)) {
                int i = (int)((d - this.tabsX) / this.tabWidth);
                ChatPanel.Tab[] tabs = ChatPanel.Tab.values();
                if (i >= 0 && i < tabs.length) {
                    this.selectedTab = tabs[i];
                }

                return true;
            }

            if (this.listScroll.mouseClicked(d, d2)) {
                return true;
            }

            if (this.isHovered(d, d2, this.position.x, this.listY, 188.0, this.listHeight)) {
                List list = this.getFilteredConversations();
                float f = this.listScroll.getScroll();

                for (int i2 = 0; i2 < list.size(); i2++) {
                    float f2 = this.listY + f + i2 * 24.0F;
                    if (!(f2 + 24.0F < this.listY)
                        && !(f2 > this.listY + this.listHeight)
                        && this.isHovered(d, d2, this.position.x + 6.0F, f2, 176.0, 24.0)) {
                        this.openConversation(this.conversations.indexOf(list.get(i2)));
                        return true;
                    }
                }
            }
        } else {
            if (flag2 && this.isHovered(d, d2, this.backButtonX - 2.0F, this.backButtonY - 2.0F, 12.0, 12.0)) {
                this.openConversationIndex = -1;
                MESSAGE_FIELD.setFocused(false);
                return true;
            }

            if (flag2 && this.isHovered(d, d2, this.sendButtonX, this.sendButtonY, 15.0, 15.0)) {
                this.sendMessage();
                MESSAGE_FIELD.setFocused(true);
                return true;
            }

            if (flag2 && this.isHovered(d, d2, this.inputX, this.inputY, this.inputWidth, 15.0)) {
                MESSAGE_FIELD.setFocused(true);
                SEARCH_FIELD.setFocused(false);
                return true;
            }

            if (this.messageScroll.mouseClicked(d, d2)) {
                return true;
            }
        }

        if (flag2 && this.isHovered(d, d2, this.position.x, this.position.y, 188.0, 15.0)) {
            this.dragging = true;
            this.dragOffsetX = (float)d - this.position.x;
            this.dragOffsetY = (float)d2 - this.position.y;
            return true;
        } else {
            return true;
        }
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.dragging) {
            this.position
                .set(
                    this.clamp((float)click.x() - this.dragOffsetX, 0.0F, Math.max(0.0F, GuiScale.getScaledWidth() - 188.0F)),
                    this.clamp((float)click.y() - this.dragOffsetY, 0.0F, Math.max(0.0F, GuiScale.getScaledHeight() - 152.0F))
                );
            return true;
        } else {
            return this.listScroll.mouseDragged(click.x(), click.y()) ? true : this.messageScroll.mouseDragged(click.x(), click.y());
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.dragging = false;
        this.listScroll.mouseReleased();
        this.messageScroll.mouseReleased();
        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.openConversationIndex < 0) {
            if (this.isHovered(d, d2, this.position.x, this.listY, 188.0, this.listHeight)) {
                this.listScroll.onScroll(d4);
                return true;
            }
        } else if (this.isHovered(d, d2, this.position.x, this.messagesTop, 188.0, this.messagesBottom - this.messagesTop)) {
            this.messageScroll.onScroll(d4);
            return true;
        }

        return this.isMouseOver(d, d2);
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        int i = keyInput.key();
        switch (i) {
            case 256:
                break;
            case 257:
            case 335:
                if (MESSAGE_FIELD.isFocused() && !MESSAGE_FIELD.isEmpty()) {
                    this.sendMessage();
                    return true;
                }

                if (i == 256) {
                    break;
                }
            default:
                if (SEARCH_FIELD.isFocused()) {
                    SEARCH_FIELD.keyPressed(i);
                    return true;
                }

                if (MESSAGE_FIELD.isFocused()) {
                    MESSAGE_FIELD.keyPressed(i);
                    return true;
                }

                return false;
        }

        if (SEARCH_FIELD.isFocused() || MESSAGE_FIELD.isFocused()) {
            SEARCH_FIELD.setFocused(false);
            MESSAGE_FIELD.setFocused(false);
            return true;
        } else if (this.openConversationIndex >= 0) {
            this.openConversationIndex = -1;
            return true;
        } else {
            this.close();
            return true;
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (SEARCH_FIELD.isFocused()) {
            SEARCH_FIELD.charTyped(charInput.codepoint());
            return true;
        } else if (MESSAGE_FIELD.isFocused()) {
            MESSAGE_FIELD.charTyped(charInput.codepoint());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public void resize(int i, int i2) {
        this.reset();
    }

    @Override
    public void init(int i, int i2) {
        if (this.positioned) {
            this.position
                .set(
                    this.clamp(this.position.x, 0.0F, Math.max(0.0F, GuiScale.getScaledWidth() - 188.0F)),
                    this.clamp(this.position.y, 0.0F, Math.max(0.0F, GuiScale.getScaledHeight() - 152.0F))
                );
        }
    }

    @Override
    public void removed() {
        SEARCH_FIELD.setFocused(false);
        MESSAGE_FIELD.setFocused(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    static final class Conversation {
        final String name;
        final boolean online;
        int unreadCount;
        final List<ChatPanel.Message> messages = new ArrayList<>();

        Conversation(String string, boolean flag, int i) {
            this.name = string;
            this.online = flag;
            this.unreadCount = i;
        }

        String getLastMessage() {
            return this.messages.isEmpty() ? "" : this.messages.get(this.messages.size() - 1).text;
        }
    }

    record Message(boolean mine, String text) {
    }

    enum Tab {
        CHATS,
        FRIENDS,
        REQUESTS;
    }
}
