package tech.sadnes.client.feature.impl.misc;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.network.packet.PlayerState;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.party.PartyMemberView;
import tech.sadnes.client.party.PartyPingType;
import tech.sadnes.client.party.PartyStateReporter;
import tech.sadnes.client.party.WorldIdentity;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MeshDrawUtil;
import tech.sadnes.client.render.ParticleBlurRenderer;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.texture.AvatarTextureLoader;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.RaycastUtil;
import ua.mintantileak.profile.Profile;

@FeatureInfo(
    name = "PartyPoint",
    description = "feature.impl.misc.partypoint",
    displayName = "feature.impl.misc.partypoint",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SOCIAL,
    autoEnabled = true
)
public final class PartyPoint extends Feature {
    private static final long PING_DURATION_MS = 12000L;
    private static final float FILL_RADIUS = 0.85F;
    private static final float BEAM_HEIGHT = 2.6F;
    private static final float BEAM_HALF_WIDTH = 0.13F;
    private static final float RIPPLE_PERIOD = 2.2F;
    private static final int RIPPLE_COUNT = 3;
    private static final int CIRCLE_SEGMENTS = 48;
    private static final float[] CIRCLE_COS = new float[48];
    private static final float[] CIRCLE_SIN = new float[48];
    private final KeybindSetting bind = new KeybindSetting("partypoint.bind");
    private final Map<String, PartyPoint.PingMarker> markers = new HashMap<>();
    private long lastFrameNanos;
    private float animationTime;
    private boolean wasBindPressed;
    private Vec3d targetPos;
    private Entity targetEntity;

    private void handleBind() {
        boolean flag = this.isBindPressed();
        if (flag && !this.wasBindPressed && mc.currentScreen == null && mc.player != null && mc.world != null) {
            if (this.isInParty()) {
                this.updateTarget();
                if (this.targetPos != null) {
                    this.sendPing(PartyPingType.GO, this.targetPos);
                    this.wasBindPressed = flag;
                    return;
                }
            } else {
                NotificationUtil.warning(this.getDisplayName(), Language.translate("party.point.notInParty"));
            }
        }

        this.wasBindPressed = flag;
    }

    private void updateTarget() {
        if (mc.player != null && mc.world != null) {
            float f = Rotation.getCameraYaw();
            float f2 = Rotation.getCameraPitch();
            HitResult hitResult = RaycastUtil.raycastBlocks(f, f2, 256.0, true);
            this.targetEntity = this.raycastEntity(f, f2, 256.0, hitResult.getPos());
            if (this.targetEntity != null) {
                this.targetPos = new Vec3d(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ());
            } else {
                this.targetPos = hitResult.getPos();
            }
        }
    }

    private boolean isBindPressed() {
        Integer integer = this.bind.getDefaultValue();
        if (integer != null && Key.isPressed(integer)) {
            return true;
        }

        Iterator iterator2 = this.bind.getBinds().iterator();

        while (iterator2.hasNext()) {
            if (Key.isPressed(((ValueBind)iterator2.next()).getKey())) {
                return true;
            }
        }

        return false;
    }

    private void sendPing(PartyPingType partyPingType, Vec3d vec3d) {
        BackendService backendService = Sadnes.getInstance().getBackend();
        if (backendService != null) {
            Entity entity = this.targetEntity;
            if (entity != null && entity.isAlive()) {
                backendService.setEntityPing(partyPingType.getId(), entity.getUuid(), entity.getX(), entity.getY(), entity.getZ(), 12000L);
            } else {
                backendService.setPing(partyPingType.getId(), vec3d.x, vec3d.y, vec3d.z, 12000L);
            }
        }
    }

    private Entity raycastEntity(float f, float f2, double d, Vec3d vec3d) {
        Entity entity2 = mc.getCameraEntity();
        if (entity2 != null && mc.world != null) {
            Vec3d vec3d2 = entity2.getEyePos();
            Vec3d vec3d3 = RaycastUtil.getRotationVector(f2, f);
            Vec3d vec3d4 = vec3d2.add(vec3d3.multiply(d));
            Box box = entity2.getBoundingBox().stretch(vec3d3.multiply(d)).expand(1.0);
            EntityHitResult entityHitResult = RaycastUtil.raycastEntities(
                entity2, vec3d2, vec3d4, box, entity3 -> !entity3.isSpectator() && entity3.canHit() && entity3 != mc.player, d
            );
            if (entityHitResult == null) {
                return null;
            }

            double d2 = vec3d2.squaredDistanceTo(entityHitResult.getPos());
            double d3 = vec3d != null ? vec3d2.squaredDistanceTo(vec3d) : d * d;
            return d2 <= d3 ? entityHitResult.getEntity() : null;
        } else {
            return null;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.resetState();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.resetState();
    }

    private void resetState() {
        this.markers.clear();
        this.wasBindPressed = false;
        this.targetPos = null;
        this.targetEntity = null;
        BackendService backendService = Sadnes.getInstance().getBackend();
        if (backendService != null) {
            backendService.clearPing();
        }
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isWorld()) {
            this.handleBind();
            if (mc.player != null && mc.world != null) {
                this.updateMarkers();
                this.renderWorldMarkers(renderEvent);
            }
        } else {
            if (renderEvent.isHudPre()) {
                this.renderLabels(renderEvent);
            }
        }
    }

    private void renderLabels(RenderEvent renderEvent) {
        if (mc.player != null && mc.world != null && !this.markers.isEmpty()) {
            MsdfFont msdfFont = Fonts.INTER_MEDIUM;
            if (msdfFont == null) {
                return;
            }

            for (PartyPoint.PingMarker pingMarker : this.markers.values()) {
                float f = pingMarker.fade.get();
                if (!(f < 0.01F)) {
                    Vec3d vec3d = this.getMarkerPos(pingMarker, renderEvent.getTickDelta());
                    Vector2f vector2f = ProjectionUtil.worldToScreen(vec3d.x, vec3d.y, vec3d.z);
                    if (vector2f != null) {
                        int i = (int)Math.round(Math.sqrt(mc.player.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z)));
                        this.drawLabel(renderEvent.getContext(), msdfFont, pingMarker, i, vector2f.x, vector2f.y, f);
                    }
                }
            }
        }
    }

    private Vec3d getMarkerPos(PartyPoint.PingMarker pingMarker, float f) {
        if (pingMarker.entityId != null) {
            Entity entity = this.resolveEntity(pingMarker);
            if (entity != null) {
                Vec3d vec3d = ProjectionUtil.getInterpolatedPos(entity, f);
                return new Vec3d(vec3d.x, vec3d.y + entity.getHeight() / 2.0F, vec3d.z);
            }
        }

        return new Vec3d(pingMarker.x, pingMarker.y + 1.4, pingMarker.z);
    }

    private Entity resolveEntity(PartyPoint.PingMarker pingMarker) {
        Entity entity = pingMarker.cachedEntity;
        if (entity != null && !entity.isRemoved() && pingMarker.entityId.equals(entity.getUuid())) {
            return entity;
        }

        for (Entity entity2 : mc.world.getEntities()) {
            if (pingMarker.entityId.equals(entity2.getUuid())) {
                pingMarker.cachedEntity = entity2;
                return entity2;
            }
        }

        pingMarker.cachedEntity = null;
        return null;
    }

    private void updateMarkers() {
        List<PartyPoint.PingData> pingDatas = this.collectPings();
        HashSet hashSet = new HashSet(pingDatas.size() * 2);

        for (PartyPoint.PingData pingData : pingDatas) {
            hashSet.add(pingData.owner());
            PartyPoint.PingMarker pingMarker = this.markers.computeIfAbsent(pingData.owner(), string -> new PartyPoint.PingMarker());
            pingMarker.owner = pingData.owner();
            pingMarker.type = pingData.type();
            pingMarker.x = pingData.x();
            pingMarker.y = pingData.y();
            pingMarker.z = pingData.z();
            if (!Objects.equals(pingMarker.entityId, pingData.entity())) {
                pingMarker.cachedEntity = null;
            }

            pingMarker.entityId = pingData.entity();
        }

        Iterator iterator2 = this.markers.entrySet().iterator();

        while (iterator2.hasNext()) {
            Entry entry = (Entry)iterator2.next();
            boolean flag = hashSet.contains(entry.getKey());
            PartyPoint.PingMarker pingMarker2 = (PartyPoint.PingMarker)entry.getValue();
            pingMarker2.fade.update();
            pingMarker2.fade.animate(flag ? 1.0 : 0.0, 0.18, Easings.SINE_OUT);
            if (!flag && pingMarker2.fade.get() < 0.01F) {
                iterator2.remove();
            }
        }
    }

    private List<PartyPoint.PingData> collectPings() {
        ArrayList arrayList = new ArrayList();
        BackendService backendService = Sadnes.getInstance().getBackend();
        if (backendService == null) {
            return arrayList;
        }

        PartyManager partyManager = backendService.getPartyManager();
        if (partyManager != null && partyManager.inParty()) {
            PartyStateReporter.ActivePing activePing = backendService.getActivePing();
            if (activePing != null) {
                arrayList.add(new PartyPoint.PingData(this.getOwnName(), PartyPingType.byId(activePing.type()), activePing.x(), activePing.y(), activePing.z(), activePing.entity()));
            }

            String string = WorldIdentity.currentWorldId();
            int i = Profile.getUid();

            for (PartyMemberView partyMemberView : partyManager.getMemberViews()) {
                if (partyMemberView.uid() != i) {
                    PlayerState playerState = partyMemberView.state();
                    if (playerState != null && playerState.isHasPing() && WorldIdentity.sameWorld(string, playerState.getWorld())) {
                        arrayList.add(
                            new PartyPoint.PingData(
                                partyMemberView.username(),
                                PartyPingType.byId(playerState.getPingType()),
                                playerState.getPingX(),
                                playerState.getPingY(),
                                playerState.getPingZ(),
                                playerState.getPingEntity()
                            )
                        );
                    }
                }
            }
        }

        return arrayList;
    }

    private void renderWorldMarkers(RenderEvent renderEvent) {
        if (!this.markers.isEmpty()) {
            long l = System.nanoTime();
            float f = this.lastFrameNanos == 0L ? 0.016666668F : Math.min((float)(l - this.lastFrameNanos) / 1.0E9F, 0.05F);
            this.lastFrameNanos = l;
            this.animationTime += f;
            float f2 = 0.88F + 0.12F * (float)Math.sin(this.animationTime * 1.8F);
            MatrixStack matrixStack = renderEvent.getMatrices();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();

            try (
                BufferAllocator bufferAllocator = new BufferAllocator(16384);
                BufferAllocator bufferAllocator2 = new BufferAllocator(32768);
            ) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                double d = 0.0;
                double d2 = 0.0;

                for (PartyPoint.PingMarker pingMarker : this.markers.values()) {
                    float f3 = pingMarker.fade.get();
                    if (!(f3 < 0.01F) && pingMarker.entityId == null) {
                        int i = pingMarker.type.getColor();
                        double d3 = pingMarker.x - vec3d.x;
                        double d4 = pingMarker.y - vec3d.y;
                        double d5 = pingMarker.z - vec3d.z;
                        d += Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5) * f3;
                        d2 += f3;
                        float f4 = (float)(pingMarker.x - vec3d.x);
                        float f5 = (float)(pingMarker.y + 0.03 - vec3d.y);
                        float f6 = (float)(pingMarker.z - vec3d.z);
                        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
                        this.drawRipples(bufferBuilder, matrixStack.peek(), pingMarker, i, f3);
                        this.drawFilledCircle(bufferBuilder2, matrix4f, f4, f5, f6, 0.85F, i, f3 * f2);
                        this.drawBeam(bufferBuilder2, matrix4f, f4, f5, f6, i, f3 * f2);
                    }
                }

                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer != null || builtBuffer2 != null) {
                    float f7 = d2 > 1.0E-4 ? (float)(d / d2) : 8.0F;
                    float f8 = ProjectionUtil.getScaledSize(0.045F, f7);
                    ParticleBlurRenderer.INSTANCE.render(f8, 1.0F, () -> {
                        if (builtBuffer2 != null) {
                            MeshDrawUtil.drawLabeled(Pipelines.AURA_COLOR_BLOOM, builtBuffer2, "sadnes party point fill");
                        }

                        if (builtBuffer != null) {
                            MeshDrawUtil.drawLabeled(Pipelines.AA_LINE, builtBuffer, "sadnes party point ring");
                        }
                    });
                }
            }
        }
    }

    private void drawRipples(BufferBuilder bufferBuilder, net.minecraft.client.util.math.MatrixStack.Entry entry, PartyPoint.PingMarker pingMarker, int i, float f) {
        float f2 = this.animationTime % 2.2F / 2.2F;

        for (int i2 = 0; i2 < 3; i2++) {
            float f3 = (f2 + i2 / 3.0F) % 1.0F;
            float f4 = 1.0F - (1.0F - f3) * (1.0F - f3);
            float f5 = Math.min(f3 / 0.12F, 1.0F) * (float)Math.pow(1.0F - f3, 1.6);
            this.drawRing(bufferBuilder, entry, pingMarker, 0.85F * (0.35F + 1.75F * f4), ColorUtil.multiplyAlpha(i, f * f5 * 0.75F), 4.2F - 2.6F * f4);
        }
    }

    private void drawRing(
        BufferBuilder bufferBuilder, net.minecraft.client.util.math.MatrixStack.Entry entry, PartyPoint.PingMarker pingMarker, float f, int i, float f2
    ) {
        if (!(f <= 0.01F) && !(f2 <= 0.1F)) {
            ArrayList arrayList = new ArrayList(48);

            for (int i2 = 0; i2 < 48; i2++) {
                arrayList.add(new Vec3d(pingMarker.x + CIRCLE_COS[i2] * f, pingMarker.y + 0.03, pingMarker.z + CIRCLE_SIN[i2] * f));
            }

            Render3DUtil.emitLines(bufferBuilder, entry, arrayList, i, f2);
        }
    }

    private void drawFilledCircle(BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, float f5) {
        int i2 = ColorUtil.multiplyAlpha(i, 0.18F * f5);
        int i3 = ColorUtil.multiplyAlpha(i, 0.0F);

        for (int i4 = 0; i4 < 48; i4++) {
            int i5 = (i4 + 1) % 48;
            bufferBuilder.vertex(matrix4f, f, f2, f3).color(i2);
            bufferBuilder.vertex(matrix4f, f, f2, f3).color(i2);
            bufferBuilder.vertex(matrix4f, f + CIRCLE_COS[i5] * f4, f2, f3 + CIRCLE_SIN[i5] * f4).color(i3);
            bufferBuilder.vertex(matrix4f, f + CIRCLE_COS[i4] * f4, f2, f3 + CIRCLE_SIN[i4] * f4).color(i3);
        }
    }

    private void drawBeam(BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, int i, float f4) {
        int i2 = ColorUtil.multiplyAlpha(i, 0.26F * f4);
        int i3 = ColorUtil.multiplyAlpha(i, 0.0F);
        this.drawBeamQuad(bufferBuilder, matrix4f, f - 0.13F, f3, f + 0.13F, f3, f2, 2.6F, i2, i3);
        this.drawBeamQuad(bufferBuilder, matrix4f, f, f3 - 0.13F, f, f3 + 0.13F, f2, 2.6F, i2, i3);
    }

    private void drawBeamQuad(BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        bufferBuilder.vertex(matrix4f, f, f5, f2).color(i);
        bufferBuilder.vertex(matrix4f, f3, f5, f4).color(i);
        bufferBuilder.vertex(matrix4f, f3, f5 + f6, f4).color(i2);
        bufferBuilder.vertex(matrix4f, f, f5 + f6, f2).color(i2);
    }

    private void drawLabel(DrawContext drawContext, MsdfFont msdfFont, PartyPoint.PingMarker pingMarker, int i, float f, float f2, float f3) {
        float f4 = (float)MathHelper.clamp(1.15 - i / 320.0, 0.85, 1.15);
        float f5 = 6.5F * f4;
        float f6 = 3.0F * f4;
        float f7 = 3.0F * f4;
        float f8 = f5 + 4.0F * f4;
        int i2 = pingMarker.type.getColor();
        String string = i + "м";
        float f9 = msdfFont.getWidth(pingMarker.owner, f5);
        float f10 = msdfFont.getWidth(string, f5);
        float f11 = f6 + f8 + f7 + f9 + f7 + f10 + f6;
        float f12 = Math.max(f8, f5) + f6 * 2.0F;
        float f13 = f - f11 / 2.0F;
        float f14 = f2 - f12 / 2.0F;
        float f15 = f12 / 2.0F;
        RenderUtil renderUtil = new RenderUtil(drawContext, f13, f14, f11, f12);
        renderUtil.blur(f3, Radius.smoothAll(f15), RenderUtil.BlurQuality.LOW);
        renderUtil.rect(ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getBackground(), i2, 0.14F), 0.6F * f3), Radius.smoothAll(f15));
        this.drawAvatar(drawContext, pingMarker, i2, f13 + f6, f14 + (f12 - f8) / 2.0F, f8, f3);
        float f16 = (f12 - f5) / 2.0F;
        float f17 = f6 + f8 + f7;
        renderUtil.textTopLeft(msdfFont, pingMarker.owner, f17, f16, ColorUtil.multiplyAlpha(Theme.getTextColor(), f3), f5);
        renderUtil.textTopLeft(msdfFont, string, f17 + f9 + f7, f16, ColorUtil.multiplyAlpha(i2, f3), f5);
    }

    private void drawAvatar(DrawContext drawContext, PartyPoint.PingMarker pingMarker, int i, float f, float f2, float f3, float f4) {
        Radius radius = Radius.all(f3 / 2.0F);
        Identifier identifier = this.getAvatarTexture(pingMarker.owner);
        if (identifier != null) {
            GuiRenderUtil.bindTexture(identifier);
            if (GuiRenderUtil.getBoundTexture() != null) {
                new RenderUtil(drawContext, f, f2, f3, f3).frostedGlass(f4, radius);
                new RenderUtil(drawContext, f, f2, f3, f3).shadow(0.8F, ColorUtil.multiplyAlpha(i, f4), radius);
                return;
            }
        }

        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f3);
        renderUtil.rect(ColorUtil.multiplyAlpha(i, f4), radius);
        renderUtil.icon(pingMarker.type.getIcon(), f3 * 0.22F, f3 * 0.22F, f3 * 0.56F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, f4));
    }

    private Identifier getAvatarTexture(String string) {
        BackendService backendService = Sadnes.getInstance().getBackend();
        if (backendService == null) {
            return null;
        }

        String string2 = backendService.getPartyManager().getAvatarUrl(string);
        return string2.isEmpty() ? null : AvatarTextureLoader.getTexture(string2);
    }

    private boolean isInParty() {
        BackendService backendService = Sadnes.getInstance().getBackend();
        return backendService != null && backendService.getPartyManager() != null && backendService.getPartyManager().inParty();
    }

    private String getOwnName() {
        String string = Profile.getUsername();
        return string != null && !string.isBlank() ? string : "you";
    }

    static {
        for (int i = 0; i < 48; i++) {
            double d = (Math.PI * 2) * i / 48.0;
            CIRCLE_COS[i] = (float)Math.cos(d);
            CIRCLE_SIN[i] = (float)Math.sin(d);
        }
    }

    record PingData(String owner, PartyPingType type, double x, double y, double z, UUID entity) {
    }

    static final class PingMarker {
        String owner;
        PartyPingType type;
        double x;
        double y;
        double z;
        UUID entityId;
        Entity cachedEntity;
        final Animation fade = new Animation();
    }
}
