package tech.sadnes.client.neuro;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.BetaScope;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import ua.mintantileak.profile.Profile;

public final class NeuroDebug {
    public static final int CAPACITY = 180;
    public static final int CH_REQ_YAW = 0;
    public static final int CH_REQ_PITCH = 1;
    public static final int CH_ACT_YAW = 2;
    public static final int CH_ACT_PITCH = 3;
    public static final int CH_NET_YAW = 4;
    public static final int CH_NET_PITCH = 5;
    public static final int CH_ERR_YAW = 6;
    public static final int CH_ERR_PITCH = 7;
    public static final int CH_SIG_YAW = 8;
    public static final int CH_SIG_PITCH = 9;
    public static final int FLAG_REVERSE_YAW = 1;
    public static final int FLAG_REVERSE_PITCH = 2;
    public static final int FLAG_BOX_CLAMP_YAW = 4;
    public static final int FLAG_BOX_CLAMP_PITCH = 8;
    public static final int FLAG_SPEED_CLAMP_YAW = 16;
    public static final int FLAG_SPEED_CLAMP_PITCH = 32;
    public static final int FLAG_EPISODE_START = 64;
    private static final Logger LOGGER = LoggerFactory.getLogger("NeuroAim");
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final NeuroStorage storage;
    private final float[][] channels = new float[10][180];
    private final byte[] flags = new byte[180];
    private int writeIndex;
    private int sampleCount;
    private float lastYaw;
    private float lastPitch;
    private boolean hasLast;
    private final ArrayDeque<Vec3d> trail = new ArrayDeque<>();
    private long lastRecordTime;
    private PrintWriter logWriter;
    private int loggedRows;
    private int episode;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:[.,]\\d+)?");
    private volatile float acSuspicion;
    private volatile float acDelta;
    private volatile float acBuffer;
    private volatile long acTimestamp;
    private LivingEntity target;
    private Vec3d aimPoint;
    private float pendingYaw;
    private float pendingPitch;
    private float errorMagnitude;
    private float distance;
    private float dominantPi;
    private float sigmaYaw;
    private float sigmaPitch;
    private boolean showAimPoint;
    private boolean showTrail;
    private boolean showHitbox;
    private boolean showDebugHud;
    private boolean csvLogging;
    private final DragSetting hud = new DragSetting(
        "neuro.debug.hud", () -> mc.getWindow().getScaledWidth() - 190.0F, () -> (mc.getWindow().getScaledHeight() - 182.0F) / 2.0F
    );

    NeuroDebug(NeuroStorage neuroStorage) {
        this.storage = neuroStorage;
        this.hud.setSize(182.0F, 182.0F);
        this.hud.setDraggable(() -> this.showDebugHud && hasAccess() ? true : false);
        this.hud.setRenderCondition(this::shouldRenderHud);
        this.hud.onInput((drawContext, i, i2, f) -> this.renderHud(drawContext));
        DragComponent.register(this.hud);
        Sadnes.getEventBus().subscribe(this);
    }

    public static boolean hasAccess() {
        return BetaScope.ONLY_ADMIN.isAllowed(Profile.getRole());
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            Packet packet = packetEvent.getPacket();
            String string;
            if (packet instanceof OverlayMessageS2CPacket) {
                string = ((OverlayMessageS2CPacket)packet).text().getString();
            } else {
                if (!(packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) || !gameMessageS2CPacket.overlay()) {
                    return;
                }

                string = gameMessageS2CPacket.content().getString();
            }

            if (ServerManager.isServer("shard")) {
                if (string.indexOf(37) >= 0) {
                    Matcher matcher2 = NUMBER_PATTERN.matcher(string);
                    float[] floats = new float[3];
                    int i = 0;

                    while (i < 3 && matcher2.find()) {
                        floats[i++] = Float.parseFloat(matcher2.group().replace(',', '.'));
                    }

                    if (i >= 3) {
                        this.acSuspicion = floats[0];
                        this.acDelta = floats[1];
                        this.acBuffer = floats[2];
                        this.acTimestamp = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    public void setCsvLogging(boolean flag) {
        if (this.csvLogging != flag) {
            this.csvLogging = flag;
            if (!flag) {
                this.closeLog();
            }
        }
    }

    private void openLog() {
        try {
            File file = this.storage.getDebugDir();
            file.mkdirs();
            File file2 = new File(file, "debug-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv");
            this.logWriter = new PrintWriter(new BufferedWriter(new FileWriter(file2, StandardCharsets.UTF_8)));
            String string = NeuroAim.INSTANCE.getModelName();
            this.logWriter.println("# model=" + (string != null ? string : "-"));
            this.logWriter
                .println(
                    "time,ep,curYaw,curPitch,errYaw,errPitch,netYaw,netPitch,sigYaw,sigPitch,reqYaw,reqPitch,actYaw,actPitch,pendYaw,pendPitch,errMag,dist,domPi,speedGain,flags,acSusp,acDelta,acBuf,acAge"
                );
            this.loggedRows = 0;
            this.episode = 0;
            LOGGER.info("Neuro debug log: {}", file2.getAbsolutePath());
        } catch (IOException ioException) {
            LOGGER.warn("Neuro debug log open failed: {}", ioException.getMessage());
            this.logWriter = null;
            this.csvLogging = false;
        }
    }

    private void closeLog() {
        if (this.logWriter != null) {
            this.logWriter.flush();
            this.logWriter.close();
            this.logWriter = null;
        }
    }

    void record(
        LivingEntity livingEntity,
        Vec3d vec3d,
        Box box,
        Vec3d vec3d2,
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
        float f13,
        float f14,
        float f15,
        float f16,
        int i,
        boolean flag
    ) {
        if (hasAccess()) {
            if (flag) {
                this.trail.clear();
                this.hasLast = false;
                i |= 64;
                this.episode++;
                if (this.logWriter != null) {
                    this.logWriter.flush();
                }
            }

            float f17 = this.hasLast ? MathHelper.wrapDegrees(f - this.lastYaw) : 0.0F;
            float f18 = this.hasLast ? f2 - this.lastPitch : 0.0F;
            this.lastYaw = f;
            this.lastPitch = f2;
            this.hasLast = true;
            this.channels[2][this.writeIndex] = f17;
            this.channels[3][this.writeIndex] = f18;
            this.channels[0][this.writeIndex] = f9;
            this.channels[1][this.writeIndex] = f10;
            this.channels[4][this.writeIndex] = f5;
            this.channels[5][this.writeIndex] = f6;
            this.channels[6][this.writeIndex] = f3;
            this.channels[7][this.writeIndex] = f4;
            this.channels[8][this.writeIndex] = f7;
            this.channels[9][this.writeIndex] = f8;
            this.flags[this.writeIndex] = (byte)i;
            this.writeIndex = (this.writeIndex + 1) % 180;
            if (this.sampleCount < 180) {
                this.sampleCount++;
            }

            this.target = livingEntity;
            this.pendingYaw = f11;
            this.pendingPitch = f12;
            this.errorMagnitude = f13;
            this.distance = f14;
            this.dominantPi = f15;
            this.sigmaYaw = f7;
            this.sigmaPitch = f8;
            this.lastRecordTime = System.currentTimeMillis();
            this.aimPoint = vec3d2;
            Vec3d vec3d3 = Vec3d.fromPolar(f12, f11);
            Vec3d vec3d4 = box.raycast(vec3d, vec3d.add(vec3d3.multiply(f14 + 3.0F))).orElse(vec3d2);
            this.trail.addLast(vec3d4);

            while (this.trail.size() > 60) {
                this.trail.removeFirst();
            }

            if (this.csvLogging && this.logWriter == null) {
                this.openLog();
            }

            if (this.logWriter != null) {
                long l = System.currentTimeMillis();
                this.logWriter
                    .printf(
                        Locale.ROOT,
                        "%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.4f,%.4f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%d,%.1f,%.2f,%.2f,%d%n",
                        l,
                        this.episode,
                        f,
                        f2,
                        f3,
                        f4,
                        f5,
                        f6,
                        f7,
                        f8,
                        f9,
                        f10,
                        f17,
                        f18,
                        f11,
                        f12,
                        f13,
                        f14,
                        f15,
                        f16,
                        i,
                        this.acSuspicion,
                        this.acDelta,
                        this.acBuffer,
                        this.acTimestamp == 0L ? -1L : l - this.acTimestamp
                    );
                if (++this.loggedRows % 100 == 0) {
                    this.logWriter.flush();
                }
            }
        }
    }

    void endEpisode() {
        this.target = null;
        this.trail.clear();
        this.hasLast = false;
        if (this.logWriter != null) {
            this.logWriter.flush();
        }
    }

    public int getSampleCount() {
        return this.sampleCount;
    }

    public float getValue(int i, int i2) {
        return this.channels[i][(this.writeIndex - this.sampleCount + i2 + 180) % 180];
    }

    public byte getFlags(int i) {
        return this.flags[(this.writeIndex - this.sampleCount + i + 180) % 180];
    }

    public float meanAbs(int i) {
        if (this.sampleCount == 0) {
            return 0.0F;
        }

        float f = 0.0F;

        for (int i2 = 0; i2 < this.sampleCount; i2++) {
            f += Math.abs(this.getValue(i, i2));
        }

        return f / this.sampleCount;
    }

    public float reversalRate(int i) {
        int i2 = 0;
        int i3 = 0;
        float f = 0.0F;

        for (int i4 = 0; i4 < this.sampleCount; i4++) {
            float f2 = this.getValue(i, i4);
            if (!(Math.abs(f2) < 0.001F)) {
                float f3 = Math.signum(f2);
                if (f != 0.0F) {
                    i3++;
                    if (f3 != f) {
                        i2++;
                    }
                }

                f = f3;
            }
        }

        return i3 == 0 ? 0.0F : (float)i2 / i3;
    }

    public float zeroRate(int i) {
        if (this.sampleCount == 0) {
            return 0.0F;
        }

        int i2 = 0;

        for (int i3 = 0; i3 < this.sampleCount; i3++) {
            if (Math.abs(this.getValue(i, i3)) < 0.001F) {
                i2++;
            }
        }

        return (float)i2 / this.sampleCount;
    }

    public int countClampedTicks() {
        int i = 0;

        for (int i2 = 0; i2 < this.sampleCount; i2++) {
            if ((this.getFlags(i2) & -65) != 0) {
                i++;
            }
        }

        return i;
    }

    public String buildSummaryLine() {
        String string = NeuroAim.INSTANCE.getModelName();
        return (string != null ? string : "—")
            + "  ·  π "
            + String.format("%.2f", this.dominantPi)
            + "  ·  err "
            + String.format("%.1f°", this.errorMagnitude)
            + "  ·  d "
            + String.format("%.2f", this.distance)
            + "  ·  σ "
            + String.format("%.2f/%.2f", this.sigmaYaw, this.sigmaPitch);
    }

    public String buildStatsLine() {
        return "rev "
            + String.format("%.2f/%.2f", this.reversalRate(2), this.reversalRate(3))
            + "  ·  |d| "
            + String.format("%.2f/%.2f", this.meanAbs(2), this.meanAbs(3))
            + "  ·  zero "
            + (int)(this.zeroRate(2) * 100.0F)
            + "%  ·  clamp "
            + this.countClampedTicks();
    }

    public float[] computeStats(int i) {
        float f = 0.0F;
        float f2 = 0.0F;
        float f3 = Float.MAX_VALUE;
        int i2 = 0;

        for (int i3 = 0; i3 < this.sampleCount; i3++) {
            float f4 = Math.abs(this.getValue(i, i3));
            if (!(f4 < 0.001F)) {
                f += f4;
                if (f4 > f2) {
                    f2 = f4;
                }

                if (f4 < f3) {
                    f3 = f4;
                }

                i2++;
            }
        }

        return i2 == 0 ? new float[]{0.0F, 0.0F, 0.0F} : new float[]{f / i2, f2, f3};
    }

    public String formatStats(String string, int i) {
        float[] floats = this.computeStats(i);
        return string + " avg " + String.format("%.2f", floats[0]) + "  ·  max " + String.format("%.2f", floats[1]) + "  ·  min " + String.format("%.2f", floats[2]);
    }

    public void drawGraph(
        DrawContext drawContext,
        float f,
        float f2,
        float f3,
        float f4,
        String string,
        int i,
        int i2,
        int i3,
        int i4,
        float f5,
        int i5,
        float f6
    ) {
        int i6 = Math.min(this.sampleCount, i5);
        new RenderUtil(drawContext, f, f2, f3, 8.0F).textTopLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), f6), 5.5F);
        if (i6 > 0) {
            String string2 = String.format("%.1f / %.1f", this.getValue(i, this.sampleCount - 1), this.getValue(i2, this.sampleCount - 1));
            new RenderUtil(drawContext, f, f2, f3, 8.0F).textTopRight(Fonts.INTER_MEDIUM, string2, 0.0F, 0.0F, Theme.withAlpha(Theme.getPrimary(), f6), 5.5F);
        }

        float f7 = f2 + 9.0F;
        GuiRenderUtil.drawSurface(drawContext, f, f7, f3, f4, f6, 6.0F);
        float f8 = f7 + f4 / 2.0F;
        new RenderUtil(drawContext, f + 2.0F, f8 - 0.25F, f3 - 4.0F, 0.5F).rect(Theme.withAlpha(Theme.getOutline(), 0.5F * f6));
        if (i6 != 0) {
            float f9 = f5;

            for (int i7 = this.sampleCount - i6; i7 < this.sampleCount; i7++) {
                float f10 = Math.abs(this.getValue(i, i7));
                float f11 = Math.abs(this.getValue(i2, i7));
                if (f10 > f9) {
                    f9 = f10;
                }

                if (f11 > f9) {
                    f9 = f11;
                }
            }

            float f12 = (f4 / 2.0F - 4.0F) / f9;
            float f13 = (f3 - 4.0F) / i5;
            int i8 = Theme.withAlpha(Theme.getPrimary(), 0.9F * f6);
            int i9 = Theme.withAlpha(Theme.getPrimary(), 0.3F * f6);
            int i10 = Theme.withAlpha(Theme.getTextColor(), 0.75F * f6);
            int i11 = Theme.withAlpha(ColorUtil.rgb(235, 90, 90), f6);
            int i12 = Theme.withAlpha(Theme.getTextColor(), 0.2F * f6);

            for (int i13 = 0; i13 < i6; i13++) {
                int i14 = this.sampleCount - i6 + i13;
                float f14 = f + 2.0F + (i5 - i6 + i13) * f13;
                byte b = this.getFlags(i14);
                if ((b & 64) != 0) {
                    new RenderUtil(drawContext, f14, f7 + 1.5F, 0.5F, f4 - 3.0F).rect(i12);
                }

                if (i3 >= 0) {
                    drawBar(drawContext, f14, f8, f13, MathHelper.clamp(this.getValue(i3, i14), -f9, f9) * f12, i9);
                }

                drawBar(drawContext, f14, f8, f13, MathHelper.clamp(this.getValue(i, i14), -f9, f9) * f12, i8);
                float f15 = MathHelper.clamp(this.getValue(i2, i14), -f9, f9) * f12;
                new RenderUtil(drawContext, f14, f8 - f15 - 0.5F, Math.max(f13 - 0.5F, 0.5F), 1.0F).rect(i10);
                if (i4 != 0 && (b & i4) != 0) {
                    new RenderUtil(drawContext, f14, f7 + f4 - 2.5F, Math.max(f13 - 0.5F, 0.5F), 1.2F).rect(i11);
                }
            }
        }
    }

    private static void drawBar(DrawContext drawContext, float f, float f2, float f3, float f4, int i) {
        if (!(Math.abs(f4) < 0.1F)) {
            float f5 = Math.max(f3 - 0.5F, 0.5F);
            if (f4 >= 0.0F) {
                new RenderUtil(drawContext, f, f2 - f4, f5, f4).rect(i);
            } else {
                new RenderUtil(drawContext, f, f2, f5, -f4).rect(i);
            }
        }
    }

    private boolean shouldRenderHud() {
        if (!this.showDebugHud || !hasAccess() || mc.player == null || this.sampleCount == 0) {
            return false;
        } else {
            return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) ? false : System.currentTimeMillis() - this.lastRecordTime < 15000L;
        }
    }

    private void renderHud(DrawContext drawContext) {
        if (this.shouldRenderHud()) {
            float f = this.hud.position2.x + 6.0F;
            float f2 = this.hud.position2.y + 6.0F;
            GuiRenderUtil.drawOutlined(drawContext, this.hud.position2.x, this.hud.position2.y, 182.0F, 182.0F, 0.85F, 5.0F);
            new RenderUtil(drawContext, f, f2, 170.0F, 8.0F)
                .textTopLeft(Fonts.INTER_MEDIUM, this.buildSummaryLine(), 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.9F), 5.0F);
            float f3 = f2 + 12.0F;
            this.drawGraph(drawContext, f, f3, 170.0F, 30.0F, Language.translate("neuro.debug.deltaYaw", "Дельта yaw"), 0, 2, 4, 21, 3.0F, 90, 1.0F);
            float f4 = f3 + (9.0F + 30.0F + 5.0F);
            this.drawGraph(drawContext, f, f4, 170.0F, 30.0F, Language.translate("neuro.debug.deltaPitch", "Дельта pitch"), 1, 3, 5, 42, 3.0F, 90, 1.0F);
            float f5 = f4 + (9.0F + 30.0F + 5.0F);
            this.drawGraph(drawContext, f, f5, 170.0F, 30.0F, Language.translate("neuro.debug.error", "Ошибка наводки"), 6, 7, -1, 0, 10.0F, 90, 1.0F);
            float f6 = f5 + (9.0F + 30.0F + 5.0F);
            new RenderUtil(drawContext, f, f6, 170.0F, 8.0F)
                .textTopLeft(Fonts.INTER_MEDIUM, this.buildStatsLine(), 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.9F), 5.0F);
            new RenderUtil(drawContext, f, f6 + 7.0F, 170.0F, 8.0F)
                .textTopLeft(Fonts.INTER_MEDIUM, this.formatStats("yaw", 2), 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.9F), 5.0F);
            new RenderUtil(drawContext, f, f6 + 14.0F, 170.0F, 8.0F)
                .textTopLeft(Fonts.INTER_MEDIUM, this.formatStats("pitch", 3), 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.9F), 5.0F);
        }
    }

    @EventHandler(priority = -500)
    public void onRender3D(RenderEvent renderEvent) {
        if (renderEvent.isWorld()) {
            if (this.showAimPoint || this.showTrail || this.showHitbox) {
                if (hasAccess() && mc.player != null) {
                    LivingEntity livingEntity = this.target;
                    boolean flag = System.currentTimeMillis() - this.lastRecordTime < 400L;
                    if (livingEntity != null && livingEntity.isAlive() && flag) {
                        float f = renderEvent.getTickDelta();
                        Vec3d vec3d = ProjectionUtil.getInterpolatedPos(livingEntity, f);
                        Box box = livingEntity.getBoundingBox().offset(vec3d.subtract(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ()));
                        if (ProjectionUtil.isVisible(box)) {
                            if (this.showHitbox) {
                                float f2 = MathHelper.clamp(this.errorMagnitude / 30.0F, 0.0F, 1.0F);
                                int i = ColorUtil.interpolate(Theme.getPrimary(), ColorUtil.rgb(235, 90, 90), f2);
                                Render3DUtil.drawGlowOutlinedBox(renderEvent.getMatrices(), box, ColorUtil.multiplyAlpha(i, 0.85F));
                            }

                            if (this.showTrail && this.trail.size() > 1) {
                                Render3DUtil.drawGlowLines(
                                    renderEvent.getMatrices(), new ArrayList<>(this.trail), ColorUtil.multiplyAlpha(Theme.getPrimary(), 0.65F), 1.5F
                                );
                            }

                            if (this.showAimPoint) {
                                Vec3d vec3d2 = mc.player.getCameraPosVec(f);
                                Vec3d vec3d3 = Vec3d.fromPolar(this.pendingPitch, this.pendingYaw);
                                Vec3d vec3d4 = box.raycast(vec3d2, vec3d2.add(vec3d3.multiply(this.distance + 3.0F))).orElse(null);
                                int i2 = vec3d4 != null ? Theme.getPrimary() : ColorUtil.rgb(235, 90, 90);
                                if (vec3d4 == null) {
                                    vec3d4 = this.aimPoint != null ? this.aimPoint : box.getCenter();
                                }

                                Render3DUtil.drawGlowBox(
                                    renderEvent.getMatrices(),
                                    new Box(vec3d4.x - 0.05, vec3d4.y - 0.05, vec3d4.z - 0.05, vec3d4.x + 0.05, vec3d4.y + 0.05, vec3d4.z + 0.05),
                                    i2,
                                    ColorUtil.multiplyAlpha(i2, 0.35F)
                                );
                                Vec3d vec3d5 = box.getCenter();
                                Render3DUtil.drawGlowOutlinedBox(
                                    renderEvent.getMatrices(),
                                    new Box(vec3d5.x - 0.025, vec3d5.y - 0.025, vec3d5.z - 0.025, vec3d5.x + 0.025, vec3d5.y + 0.025, vec3d5.z + 0.025),
                                    ColorUtil.multiplyAlpha(ColorUtil.WHITE, 0.45F)
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public float getPendingYaw() {
        return this.pendingYaw;
    }

    public float getPendingPitch() {
        return this.pendingPitch;
    }

    public float getErrorMagnitude() {
        return this.errorMagnitude;
    }

    public float getDistance() {
        return this.distance;
    }

    public float getDominantPi() {
        return this.dominantPi;
    }

    public float getSigmaYaw() {
        return this.sigmaYaw;
    }

    public float getSigmaPitch() {
        return this.sigmaPitch;
    }

    public boolean isShowAimPoint() {
        return this.showAimPoint;
    }

    public boolean isShowTrail() {
        return this.showTrail;
    }

    public boolean isShowHitbox() {
        return this.showHitbox;
    }

    public boolean isShowDebugHud() {
        return this.showDebugHud;
    }

    public void setShowAimPoint(boolean flag) {
        this.showAimPoint = flag;
    }

    public void setShowTrail(boolean flag) {
        this.showTrail = flag;
    }

    public void setShowHitbox(boolean flag) {
        this.showHitbox = flag;
    }

    public void setShowDebugHud(boolean flag) {
        this.showDebugHud = flag;
    }

    public boolean isCsvLogging() {
        return this.csvLogging;
    }

    public DragSetting getHud() {
        return this.hud;
    }
}
