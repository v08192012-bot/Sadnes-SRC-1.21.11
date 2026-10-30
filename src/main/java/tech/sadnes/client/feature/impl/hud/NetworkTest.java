package tech.sadnes.client.feature.impl.hud;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.feature.Beta;
import tech.sadnes.client.feature.BetaScope;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

@Beta(scope = BetaScope.ONLY_ADMIN)
@FeatureInfo(
    name = "NetworkTest",
    description = "feature.impl.hud.networktest",
    displayName = "feature.impl.hud.networktest",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_INFORMATION
)
public class NetworkTest extends Feature {
    private static final int GOOD_COLOR = ColorUtil.rgb(85, 220, 120);
    private static final int WARN_COLOR = ColorUtil.rgb(240, 190, 90);
    private static final int BAD_COLOR = ColorUtil.rgb(230, 90, 90);
    private final long[] pingHistory = new long[48];
    private int historySize;
    private int historyIndex;
    private long minPing = Long.MAX_VALUE;
    private long maxPing;
    private long pingSum;
    private int pingCount;
    private long lastPing = Long.MIN_VALUE;
    private int lastConnectionId = -1;

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        DrawContext drawContext = render2DEvent.getContext();
        BackendService backendService = Sadnes.getInstance().getBackend();
        int i = 4;
        if (backendService == null) {
            this.drawText(drawContext, 4, 4, "sadnes-network: manager offline", BAD_COLOR);
        } else {
            boolean flag = backendService.isConnected();
            boolean flag2 = backendService.isConnecting();
            long l = backendService.getServerTime();
            int i2 = backendService.getConnectCount();
            if (i2 != this.lastConnectionId) {
                this.resetStats();
                this.lastConnectionId = i2;
            }

            if (flag && l >= 0L && l != this.lastPing) {
                this.recordPing(l);
                this.lastPing = l;
            }

            String string = flag ? "CONNECTED" : (flag2 ? "CONNECTING" : "OFFLINE");
            int i3 = flag ? GOOD_COLOR : (flag2 ? WARN_COLOR : BAD_COLOR);
            this.drawText(drawContext, 4, 4, "sadnes-network", Theme.getTextColor());
            this.drawText(drawContext, 4 + mc.textRenderer.getWidth("sadnes-network  "), 4, string, i3);
            i += 12;
            String string2 = flag ? (l < 0L ? "— ms" : l + " ms") : "—";
            String string3 = this.pingCount > 0 ? "  min " + this.minPing + " / avg " + this.pingSum / this.pingCount + " / max " + this.maxPing : "";
            this.drawText(drawContext, 4, 16, "ping    " + string2 + string3, this.getPingColor(flag, l));
            i += 10;
            String string4 = flag && backendService.getConnectedAt() > 0L ? formatUptime(System.currentTimeMillis() - backendService.getConnectedAt()) : "—";
            int i4 = Math.max(0, backendService.getConnectCount() - 1);
            int i5 = backendService.getDisconnectCount();
            this.drawText(
                drawContext,
                4,
                26,
                "uptime  " + string4 + "    reconnects " + i4 + "    drops " + i5,
                i4 <= 0 && i5 <= 0 ? Theme.getSecondaryText() : WARN_COLOR
            );
            i += 10;
            String string5 = backendService.getLastError();
            if (string5 != null && !string5.isBlank()) {
                this.drawText(drawContext, 4, 36, "lastdrop " + truncate(string5, 46), BAD_COLOR);
                i += 10;
            }

            this.drawText(drawContext, 4, i, "server  " + backendService.getServerUrl(), Theme.getSecondaryText());
            i += 10;
            PartyManager partyManager = backendService.getPartyManager();
            boolean flag3;
            if (partyManager != null) {
                flag3 = partyManager.inParty();
            } else {
                flag3 = false;
            }

            String string6 = flag3 ? "[" + partyManager.getPartyName() + "] " + partyManager.getMembers().size() + (partyManager.isLeader() ? " · leader" : "") : "not in party";
            this.drawText(drawContext, 4, i, "party   " + string6, flag3 ? GOOD_COLOR : Theme.getSecondaryText());
            i += 10;
            this.drawText(drawContext, 4, i, "states  cached " + backendService.getTrackedStateCount(), Theme.getSecondaryText());
            i += 10;
            this.drawText(drawContext, 4, i, "uid " + Profile.getUid() + " · " + Profile.getUsername() + " · " + this.getRoleName(), Theme.getSecondaryText());
            i += 13;
            if (this.pingCount > 0) {
                this.renderPingGraph(drawContext, 4, i);
            }
        }
    }

    private void renderPingGraph(DrawContext drawContext, int i, int i2) {
        drawContext.fill(i, i2, i + 98, i2 + 22, ColorUtil.multiplyAlpha(Theme.getBackground(), 0.5F));
        long l = Math.max(this.maxPing, 1L);
        int i3 = this.pingHistory.length;

        for (int i4 = 0; i4 < this.historySize; i4++) {
            long l2 = this.pingHistory[(this.historyIndex - this.historySize + i4 + i3) % i3];
            int i5 = (int)Math.max(1L, Math.round((double)l2 / l * 20));
            int i6 = i + 1 + i4 * 2;
            if (i6 + 1 > i + 98 - 1) {
                return;
            }

            drawContext.fill(i6, i2 + 22 - 1 - i5, i6 + 1, i2 + 22 - 1, l2 <= 50L ? GOOD_COLOR : (l2 <= 100L ? WARN_COLOR : BAD_COLOR));
        }
    }

    private void recordPing(long l) {
        this.minPing = Math.min(this.minPing, l);
        this.maxPing = Math.max(this.maxPing, l);
        this.pingSum += l;
        this.pingCount++;
        this.pingHistory[this.historyIndex] = l;
        this.historyIndex = (this.historyIndex + 1) % this.pingHistory.length;
        if (this.historySize < this.pingHistory.length) {
            this.historySize++;
        }
    }

    private void resetStats() {
        this.minPing = Long.MAX_VALUE;
        this.maxPing = 0L;
        this.pingSum = 0L;
        this.pingCount = 0;
        this.historySize = 0;
        this.historyIndex = 0;
        this.lastPing = Long.MIN_VALUE;
    }

    private int getPingColor(boolean flag, long l) {
        if (!flag || l < 0L) {
            return Theme.getSecondaryText();
        } else if (l <= 50L) {
            return GOOD_COLOR;
        } else {
            return l <= 100L ? WARN_COLOR : BAD_COLOR;
        }
    }

    private String getRoleName() {
        Role role = Profile.getRole();
        return role == null ? "?" : role.name();
    }

    private static String formatUptime(long l) {
        long l2 = l / 1000L;
        long l3 = l2 / 3600L;
        long l4 = l2 % 3600L / 60L;
        long l5 = l2 % 60L;
        return l3 > 0L ? String.format("%d:%02d:%02d", l3, l4, l5) : String.format("%d:%02d", l4, l5);
    }

    private static String truncate(String string, int i) {
        return string.length() > i ? string.substring(0, i - 1) + "…" : string;
    }

    private void drawText(DrawContext drawContext, int i, int i2, String string, int i3) {
        drawContext.drawText(mc.textRenderer, string, i, i2, i3, true);
    }
}
