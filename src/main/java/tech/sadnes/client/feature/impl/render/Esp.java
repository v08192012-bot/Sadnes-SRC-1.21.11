package tech.sadnes.client.feature.impl.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.feature.impl.misc.FreeCam;
import tech.sadnes.client.feature.impl.render.esp.ArmorElement;
import tech.sadnes.client.feature.impl.render.esp.BoxElement;
import tech.sadnes.client.feature.impl.render.esp.ChamsElement;
import tech.sadnes.client.feature.impl.render.esp.CooldownsElement;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.feature.impl.render.esp.GlowElement;
import tech.sadnes.client.feature.impl.render.esp.HealthBarElement;
import tech.sadnes.client.feature.impl.render.esp.HeldItemsElement;
import tech.sadnes.client.feature.impl.render.esp.NametagElement;
import tech.sadnes.client.feature.impl.render.esp.PotionsElement;
import tech.sadnes.client.feature.impl.render.esp.ScriptShaderElement;
import tech.sadnes.client.render.ModelChamsRenderer;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.RoundedRectRenderer;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.shader.GlowRenderer;
import tech.sadnes.client.render.shader.ScriptShader;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.util.color.ColorUtil;

@FeatureInfo(
    name = "Esp",
    description = "feature.impl.render.esp",
    displayName = "feature.impl.render.esp",
    category = Category.VISUALS,
    subcategory = Subcategory.ENTITIES
)
public class Esp extends Feature {
    public final List<EspElement> elements = createElements();
    public final ModeSetting performanceMode = new ModeSetting("option.performance mode", "quality", new String[]{"maximum", "balanced", "quality"});
    public final NumberSetting backgroundAlpha = new NumberSetting("option.background alpha", 1.0F, 0.0F, 1.0F, 0.05F);
    public final BooleanSetting dormant = new BooleanSetting("option.dormant", false);
    public final NumberSetting dormantTime = new NumberSetting("option.dormant time", 5.0F, 2.0F, 20.0F, 0.5F);
    private final EnumMap<EspTargetType, List<EspElement>> defaultElements;
    private final EnumMap<EspTargetType, List<EspElement>> activeElements;
    private final EnumMap<EspTargetType, List<ScriptShaderElement>> scriptElements;
    private final EnumMap<EspTargetType, BoxElement> boxElements;
    private final EnumMap<EspTargetType, GlowElement> glowElements;
    private final EnumMap<EspTargetType, ChamsElement> chamsElements;
    private final EnumMap<EspTargetType, NametagElement> nametagElements;
    private final List<Setting<?>> settings;
    private final List<Esp.EspTarget> targetPool;
    private final RoundedRectRenderer.Batch blurBatch;
    private int targetCount;
    private boolean targetsCollected;
    private final Long2ObjectOpenHashMap<Esp.EspTarget> itemStacks;
    private static final float DORMANT_FONT_SIZE = 6.0F;
    private static final int DORMANT_COLOR = ColorUtil.fromHex("FF5555");
    private static final int MAX_PLAYER_CACHE = 512;
    private static final EquipmentSlot[] COPIED_SLOTS = new EquipmentSlot[]{
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };
    private final Queue<Esp.PositionUpdate> pendingPositions;
    private final Map<UUID, Esp.DormantPlayer> dormantPlayers;
    private final RemotePlayerTracker remotePlayerTracker;
    private final Map<Integer, PlayerEntity> playerCache;

    public Esp() {
        this.dormantTime.setVisibleWhen(this.dormant::getValue);
        this.defaultElements = groupByTarget(this.elements);
        this.activeElements = new EnumMap<>(this.defaultElements);
        this.scriptElements = new EnumMap<>(EspTargetType.class);
        this.boxElements = this.collectElements(BoxElement.class);
        this.glowElements = this.collectElements(GlowElement.class);
        this.chamsElements = this.collectElements(ChamsElement.class);
        this.nametagElements = this.collectElements(NametagElement.class);
        this.settings = collectSettings(this.elements);
        this.targetPool = new ArrayList<>();
        this.blurBatch = RoundedRectRenderer.createBatch();
        this.itemStacks = new Long2ObjectOpenHashMap<>();
        this.pendingPositions = new ConcurrentLinkedQueue<>();
        this.dormantPlayers = new HashMap<>();
        this.remotePlayerTracker = new RemotePlayerTracker();
        this.playerCache = new LinkedHashMap<Integer, PlayerEntity>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Entry<Integer, PlayerEntity> entry) {
                return this.size() > 512;
            }
        };
    }

    public static Esp getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Esp.class);
    }

    public List<EspElement> getElements(EspTargetType espTargetType) {
        return this.activeElements.getOrDefault(espTargetType, List.of());
    }

    public void applyOverrides(List<? extends EspElement> list) {
        for (EspTargetType espTargetType : EspTargetType.values()) {
            ArrayList<EspElement> espElements = new ArrayList<>();

            for (EspElement espElement : this.defaultElements.get(espTargetType)) {
                EspElement espElement2 = findOverride(list, espTargetType, espElement.getDisplayName());
                espElement.setSuppressed(espElement2 != null);
                espElements.add(espElement2 != null ? espElement2 : espElement);
            }

            for (EspElement espElement3 : list) {
                if (espElement3.isTarget(espTargetType) && !espElements.contains(espElement3)) {
                    espElements.add(espElement3);
                }
            }

            this.activeElements.put(espTargetType, Collections.unmodifiableList(espElements));
            ArrayList arrayList = new ArrayList();

            for (EspElement espElement4 : espElements) {
                if (espElement4 instanceof ScriptShaderElement scriptShaderElement) {
                    arrayList.add(scriptShaderElement);
                }
            }

            this.scriptElements.put(espTargetType, List.copyOf(arrayList));
        }
    }

    private static EspElement findOverride(List<? extends EspElement> list, EspTargetType espTargetType, String string) {
        for (EspElement espElement : list) {
            if (espElement.isTarget(espTargetType) && string.equals(espElement.getAlias())) {
                return espElement;
            }
        }

        return null;
    }

    public boolean isChamsTarget(Entity entity) {
        if (this.isEnabled() && entity != null) {
            EspTargetType espTargetType = getTargetType(entity);
            if (espTargetType == null) {
                return false;
            }

            ChamsElement chamsElement = this.chamsElements.get(espTargetType);
            if (chamsElement != null && chamsElement.isActive() && !chamsElement.model.getValue()) {
                return true;
            }

            for (ScriptShaderElement scriptShaderElement : this.scriptElements.getOrDefault(espTargetType, List.of())) {
                if (scriptShaderElement.shouldRenderShader()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.clearDormant();
    }

    @EventHandler(priority = 250)
    public void onRender(RenderEvent renderEvent) {
        if (mc.player != null && mc.world != null) {
            if (renderEvent.isWorld()) {
                this.renderWorld(renderEvent);
            } else {
                if (renderEvent.isHudPre()) {
                    this.renderHud(renderEvent);
                    this.renderDormantTimers(renderEvent);
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (this.dormant.getValue() && packetEvent.isReceive()) {
            if (packetEvent.getPacket() instanceof EntityPositionSyncS2CPacket entityPositionSyncS2CPacket) {
                this.pendingPositions.add(new Esp.PositionUpdate(entityPositionSyncS2CPacket.id(), entityPositionSyncS2CPacket.values().position(), entityPositionSyncS2CPacket.values().yaw(), entityPositionSyncS2CPacket.values().pitch()));
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.clearDormant();
    }

    @EventHandler
    public void onTick(WorldTickEvent worldTickEvent) {
        if (mc.world == null) {
            this.clearDormant();
        } else {
            HashSet hashSet = new HashSet();

            for (PlayerEntity playerEntity : mc.world.getPlayers()) {
                if (playerEntity != mc.player) {
                    this.playerCache.put(playerEntity.getId(), playerEntity);
                    hashSet.add(playerEntity.getUuid());
                }
            }

            if (!this.dormant.getValue()) {
                this.pendingPositions.clear();
                if (!this.dormantPlayers.isEmpty()) {
                    this.dormantPlayers.clear();
                }
            } else {
                long l = System.currentTimeMillis();

                Esp.PositionUpdate positionUpdate;
                while ((positionUpdate = this.pendingPositions.poll()) != null) {
                    PlayerEntity playerEntity2 = this.playerCache.get(positionUpdate.id());
                    if (playerEntity2 != null) {
                        UUID uuid = playerEntity2.getUuid();
                        if (hashSet.contains(uuid)) {
                            this.dormantPlayers.remove(uuid);
                        } else {
                            Esp.DormantPlayer dormantPlayer = this.dormantPlayers.get(uuid);
                            if (dormantPlayer == null) {
                                this.dormantPlayers.put(uuid, this.createDormantPlayer(playerEntity2, positionUpdate, l));
                            } else {
                                dormantPlayer.lastSeen = l;
                                applyPosition(dormantPlayer.entity, positionUpdate.pos(), positionUpdate.yaw(), positionUpdate.pitch());
                            }
                        }
                    }
                }

                long l2 = (long)(this.dormantTime.getValue() * 1000.0F);
                Iterator iterator2 = this.dormantPlayers.entrySet().iterator();

                while (iterator2.hasNext()) {
                    Entry entry = (Entry)iterator2.next();
                    if (hashSet.contains(entry.getKey()) || l - ((Esp.DormantPlayer)entry.getValue()).lastSeen >= l2) {
                        iterator2.remove();
                    }
                }
            }
        }
    }

    private Esp.DormantPlayer createDormantPlayer(PlayerEntity playerEntity, Esp.PositionUpdate positionUpdate, long l) {
        OtherClientPlayerEntity otherClientPlayerEntity = new OtherClientPlayerEntity(mc.world, playerEntity.getGameProfile());

        for (EquipmentSlot equipmentSlot : COPIED_SLOTS) {
            otherClientPlayerEntity.equipStack(equipmentSlot, playerEntity.getEquippedStack(equipmentSlot).copy());
        }

        applyPosition(otherClientPlayerEntity, positionUpdate.pos(), positionUpdate.yaw(), positionUpdate.pitch());
        return new Esp.DormantPlayer(otherClientPlayerEntity, l);
    }

    private static void applyPosition(OtherClientPlayerEntity otherClientPlayerEntity, Vec3d vec3d, float f, float f2) {
        otherClientPlayerEntity.setPosition(vec3d.x, vec3d.y, vec3d.z);
        otherClientPlayerEntity.lastRenderX = vec3d.x;
        otherClientPlayerEntity.lastRenderY = vec3d.y;
        otherClientPlayerEntity.lastRenderZ = vec3d.z;
        otherClientPlayerEntity.setYaw(f);
        otherClientPlayerEntity.setHeadYaw(f);
        otherClientPlayerEntity.setPitch(f2);
        otherClientPlayerEntity.lastYaw = f;
        otherClientPlayerEntity.bodyYaw = f;
        otherClientPlayerEntity.lastBodyYaw = f;
    }

    private void clearDormant() {
        this.pendingPositions.clear();
        if (!this.dormantPlayers.isEmpty()) {
            this.dormantPlayers.clear();
        }

        if (!this.playerCache.isEmpty()) {
            this.playerCache.clear();
        }
    }

    private void renderDormantTimers(RenderEvent renderEvent) {
        if (this.dormant.getValue() && !this.dormantPlayers.isEmpty()) {
            MsdfFont msdfFont = Fonts.INTER_MEDIUM;
            if (msdfFont == null) {
                return;
            }

            DrawContext drawContext = renderEvent.getContext();
            long l = System.currentTimeMillis();

            for (Esp.DormantPlayer dormantPlayer : this.dormantPlayers.values()) {
                OtherClientPlayerEntity otherClientPlayerEntity = dormantPlayer.entity;
                double d = otherClientPlayerEntity.getBoundingBox().minY - 0.25;
                if (ProjectionUtil.isVisible(otherClientPlayerEntity.getX(), d, otherClientPlayerEntity.getZ())) {
                    Vector2f vector2f = ProjectionUtil.worldToScreen(otherClientPlayerEntity.getX(), d, otherClientPlayerEntity.getZ());
                    if (vector2f != null) {
                        String string = "[" + String.format(Locale.ROOT, "%.1f", (float)(l - dormantPlayer.lastSeen) / 1000.0F) + "]";
                        msdfFont.draw(drawContext, string, vector2f.x - msdfFont.getWidth(string, 6.0F) / 2.0F, vector2f.y, DORMANT_COLOR, 6.0F);
                    }
                }
            }
        }
    }

    private void renderWorld(RenderEvent renderEvent) {
        float f = renderEvent.getTickDelta();
        this.collectTargets(f);
        this.targetsCollected = true;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d vec3d = camera.getCameraPos();
        MatrixStack matrixStack = renderEvent.getMatrices();
        EnumMap<EspTargetType, List<Entity>> enumMap = new EnumMap<>(EspTargetType.class);
        EnumMap<EspTargetType, List<Entity>> enumMap2 = new EnumMap<>(EspTargetType.class);
        LinkedHashMap<ScriptShaderElement, List<Entity>> linkedHashMap = new LinkedHashMap<>();

        try (Render3DUtil.BoxBatch boxBatch = Render3DUtil.glowBoxBatch()) {
            for (int i = 0; i < this.targetCount; i++) {
                Esp.EspTarget espTarget = this.targetPool.get(i);
                EspTargetType espTargetType = espTarget.type;
                Entity entity2 = espTarget.entity;
                GlowElement glowElement = this.glowElements.get(espTargetType);
                if (glowElement != null && glowElement.isActive()) {
                    enumMap.computeIfAbsent(espTargetType, espTargetType2 -> new ArrayList<>()).add(entity2);
                }

                ChamsElement chamsElement = this.chamsElements.get(espTargetType);
                if (chamsElement != null && chamsElement.isActive()) {
                    enumMap2.computeIfAbsent(espTargetType, espTargetType3 -> new ArrayList<>()).add(entity2);
                }

                for (ScriptShaderElement scriptShaderElement : this.scriptElements.getOrDefault(espTargetType, List.of())) {
                    if (scriptShaderElement.isActive()) {
                        linkedHashMap.computeIfAbsent(scriptShaderElement, scriptShaderElement2 -> new ArrayList<>()).add(entity2);
                    }
                }

                BoxElement boxElement = this.boxElements.get(espTargetType);
                if (boxElement != null && boxElement.isBox3d()) {
                    boolean flag = boxElement.render.isSelected("Lines");
                    boolean flag2 = boxElement.render.isSelected("Fill");
                    if (flag || flag2) {
                        Box box = entity2.getBoundingBox();
                        float f2 = (float)((box.maxX - box.minX) / 2.0);
                        float f3 = (float)((box.maxZ - box.minZ) / 2.0);
                        float f4 = (float)(box.maxY - box.minY) - (entity2.isSneaking() ? 0.1F : 0.0F);
                        float f5 = getBodyYaw(entity2, f);
                        int i2 = boxElement.getEntityColor(entity2, 0, 1.0F);
                        int i3 = ColorUtil.darken(ColorUtil.setAlphaf(i2, 0.15F), 0.5F);
                        matrixStack.push();
                        matrixStack.translate(espTarget.pos.x - vec3d.x, espTarget.pos.y - vec3d.y, espTarget.pos.z - vec3d.z);
                        matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(f5));
                        if (flag2) {
                            boxBatch.fill(matrixStack, -f2, 0.0F, -f3, f2, f4, f3, i3);
                        }

                        if (flag) {
                            boxBatch.outline(matrixStack, -f2, 0.0F, -f3, f2, f4, f3, i2);
                        }

                        matrixStack.pop();
                    }
                }
            }

            boxBatch.draw();
        }

        this.renderGlow(matrixStack, camera, f, enumMap, linkedHashMap);
        this.renderChams(matrixStack, camera, f, enumMap2, linkedHashMap);
    }

    private void renderGlow(MatrixStack matrixStack, Camera camera, float f, EnumMap<EspTargetType, List<Entity>> enumMap, Map<ScriptShaderElement, List<Entity>> map) {
        for (EspTargetType espTargetType : enumMap.keySet()) {
            GlowElement glowElement = this.glowElements.get(espTargetType);
            if (glowElement != null) {
                GlowRenderer.INSTANCE.renderEntityGlow(matrixStack, camera, f, (List<Entity>)enumMap.get(espTargetType), glowElement.getColor(), glowElement.radius.getValue());
            }
        }

        for (Entry entry : map.entrySet()) {
            ScriptShaderElement scriptShaderElement = (ScriptShaderElement)entry.getKey();
            if (!scriptShaderElement.isChams() && scriptShaderElement.getShader() != null) {
                float f2 = scriptShaderElement.getRadius();
                GlowRenderer.INSTANCE
                    .renderScriptGlow(matrixStack, camera, f, (List<Entity>)entry.getValue(), scriptShaderElement.getColor(), f2, scriptShaderElement.getShader(), scriptShaderElement.createUniforms(f2));
            }
        }
    }

    private void renderChams(MatrixStack matrixStack, Camera camera, float f, EnumMap<EspTargetType, List<Entity>> enumMap, Map<ScriptShaderElement, List<Entity>> map) {
        ArrayList arrayList = new ArrayList(enumMap.size() + map.size());

        for (EspTargetType espTargetType : enumMap.keySet()) {
            ChamsElement chamsElement = this.chamsElements.get(espTargetType);
            if (chamsElement != null) {
                arrayList.add(
                    new ModelChamsRenderer.Request(
                        (List<Entity>)enumMap.get(espTargetType), chamsElement.getColor(), chamsElement.getModeId(), chamsElement.speed.getValue(), chamsElement.scale.getValue(), chamsElement.model.getValue()
                    )
                );
            }
        }

        for (Entry entry : map.entrySet()) {
            ScriptShaderElement scriptShaderElement = (ScriptShaderElement)entry.getKey();
            ScriptShader scriptShader = scriptShaderElement.isChams() ? scriptShaderElement.getShader() : null;
            if (scriptShader != null) {
                arrayList.add(
                    ModelChamsRenderer.Request.script((List<Entity>)entry.getValue(), scriptShaderElement.getColor(), scriptShaderElement.isModel(), scriptShader, scriptShaderElement.createUniforms(0.0F))
                );
            }
        }

        if (!arrayList.isEmpty()) {
            ModelChamsRenderer.INSTANCE.render(matrixStack, camera, f, arrayList);
        }
    }

    private static float getBodyYaw(Entity entity, float f) {
        return entity instanceof LivingEntity livingEntity ? MathHelper.lerp(f, livingEntity.lastBodyYaw, livingEntity.bodyYaw) : MathHelper.lerp(f, entity.lastYaw, entity.getYaw());
    }

    private <T extends EspElement> EnumMap<EspTargetType, T> collectElements(Class<T> clazz) {
        EnumMap enumMap = new EnumMap<>(EspTargetType.class);

        for (EspTargetType espTargetType : EspTargetType.values()) {
            for (EspElement espElement : this.activeElements.getOrDefault(espTargetType, List.of())) {
                if (clazz.isInstance(espElement)) {
                    enumMap.put(espTargetType, (EspElement)clazz.cast(espElement));
                    break;
                }
            }
        }

        return enumMap;
    }

    private void renderHud(RenderEvent renderEvent) {
        float f = renderEvent.getTickDelta();
        if (!this.targetsCollected) {
            this.collectTargets(f);
        }

        this.targetsCollected = false;
        DrawContext drawContext = renderEvent.getContext();

        for (int i = 0; i < this.targetCount; i++) {
            Esp.EspTarget espTarget = this.targetPool.get(i);
            List list = this.activeElements.getOrDefault(espTarget.type, List.of());
            int i2 = list.size();
            if (i2 != 0) {
                if (espTarget.type == EspTargetType.ITEM) {
                    for (int i3 = 0; i3 < i2; i3++) {
                        ((EspElement)list.get(i3)).updateItems(espTarget.items);
                    }
                }

                float f2 = espTarget.maxX - espTarget.minX;
                float f3 = espTarget.maxY - espTarget.minY;

                for (int i4 = 0; i4 < i2; i4++) {
                    ((EspElement)list.get(i4)).invalidateLayout();
                }

                for (int i5 = 0; i5 < i2; i5++) {
                    EspElement espElement = (EspElement)list.get(i5);
                    if (espElement.isActive()) {
                        espElement.measure(espTarget.entity, false);
                    }
                }

                for (int i6 = 0; i6 < i2; i6++) {
                    EspElement espElement2 = (EspElement)list.get(i6);
                    if (espElement2.isActive()) {
                        espElement2.layout(espTarget.minX, espTarget.minY, f2, f3, false, list);
                    }
                }

                RoundedRectRenderer.beginBatch(this.blurBatch);

                try {
                    for (int i7 = 0; i7 < i2; i7++) {
                        EspElement espElement3 = (EspElement)list.get(i7);
                        if (espElement3.isActive()) {
                            espElement3.renderBackground(drawContext, espTarget.entity, espTarget.minX, espTarget.minY, espTarget.maxX, espTarget.maxY, f);
                        }
                    }
                } finally {
                    RoundedRectRenderer.endBatch();
                }

                this.blurBatch.flush(drawContext);

                for (int i8 = 0; i8 < i2; i8++) {
                    EspElement espElement4 = (EspElement)list.get(i8);
                    if (espElement4.isActive()) {
                        espElement4.renderOverlay(drawContext, espTarget.entity, espTarget.minX, espTarget.minY, espTarget.maxX, espTarget.maxY, f);
                    }
                }
            }
        }
    }

    private void collectTargets(float f) {
        this.targetCount = 0;

        for (Entity entity2 : TargetComponent.ENTITIES) {
            this.addTarget(entity2, f);
        }

        List list = this.remotePlayerTracker.update(f);
        int i = list.size();

        for (int i2 = 0; i2 < i; i2++) {
            this.addTarget((Entity)list.get(i2), f);
        }

        if (this.dormant.getValue() && !this.dormantPlayers.isEmpty()) {
            for (Esp.DormantPlayer dormantPlayer : this.dormantPlayers.values()) {
                this.addTargetAs(dormantPlayer.entity, f, EspTargetType.OTHERS);
            }
        }

        this.groupItems();
        this.targetPool.subList(0, this.targetCount).sort(Comparator.comparingInt(espTarget -> espTarget.priority));
    }

    private static int getPriority(EspTargetType espTargetType, Entity entity) {
        if (espTargetType == EspTargetType.SELF) {
            return 1000;
        } else if (espTargetType == EspTargetType.FRIENDS) {
            return 100;
        } else if (entity instanceof PlayerEntity playerEntity) {
            int i = 0 + getArmorTier(playerEntity.getEquippedStack(EquipmentSlot.HEAD));
            int i2 = i + getArmorTier(playerEntity.getEquippedStack(EquipmentSlot.CHEST));
            int i3 = i2 + getArmorTier(playerEntity.getEquippedStack(EquipmentSlot.LEGS));
            int i4 = i3 + getArmorTier(playerEntity.getEquippedStack(EquipmentSlot.FEET));
            return 10 + i4;
        } else if (espTargetType == EspTargetType.MOB || espTargetType == EspTargetType.ANIMAL) {
            return 5;
        } else {
            return espTargetType == EspTargetType.ITEM ? 1 : 0;
        }
    }

    private static int getArmorTier(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return 0;
        } else {
            String string = itemStack.getItem().getTranslationKey();
            if (string.contains("netherite")) {
                return 5;
            } else if (string.contains("diamond")) {
                return 4;
            } else if (string.contains("iron") || string.contains("chainmail")) {
                return 3;
            } else if (string.contains("gold")) {
                return 2;
            } else {
                return string.contains("leather") ? 1 : 0;
            }
        }
    }

    private void addTarget(Entity entity, float f) {
        this.addTargetAs(entity, f, getTargetType(entity));
    }

    private void addTargetAs(Entity entity2, float f, EspTargetType espTargetType) {
        if (ProjectionUtil.isVisible(entity2.getBoundingBox())) {
            if (espTargetType != null) {
                if (!(entity2 instanceof LivingEntity) || ((LivingEntity)entity2).isAlive()) {
                    if (!this.isHiddenSelf(entity2)) {
                        List list = this.activeElements.getOrDefault(espTargetType, List.of());
                        if (!list.isEmpty()) {
                            if (hasActiveElement(list)) {
                                Vec3d vec3d = ProjectionUtil.getInterpolatedPos(entity2, f);
                                Box box = this.getInterpolatedBox(entity2, vec3d);
                                Esp.EspTarget espTarget = this.nextTarget();
                                if (!this.projectBounds(box, espTarget)) {
                                    this.targetCount--;
                                } else {
                                    espTarget.entity = entity2;
                                    espTarget.type = espTargetType;
                                    espTarget.pos = vec3d;
                                    espTarget.priority = getPriority(espTargetType, entity2);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void groupItems() {
        this.itemStacks.clear();
        NametagElement nametagElement = this.nametagElements.get(EspTargetType.ITEM);
        boolean flag;
        int i;
        if (nametagElement != null && nametagElement.stackItems != null) {
            flag = nametagElement.stackItems.getValue();
            i = 0;
        } else {
            flag = false;
            i = 0;
        }

        for (; i < this.targetCount; i++) {
            Esp.EspTarget espTarget = this.targetPool.get(i);
            if (espTarget.type == EspTargetType.ITEM && espTarget.entity instanceof ItemEntity itemEntity) {
                if (espTarget.items == null) {
                    espTarget.items = new ArrayList<>(4);
                } else {
                    espTarget.items.clear();
                }

                if (!flag) {
                    espTarget.items.add(itemEntity.getStack());
                } else {
                    long l = itemEntity.getBlockPos().asLong();
                    Esp.EspTarget espTarget2 = this.itemStacks.get(l);
                    if (espTarget2 == null) {
                        this.itemStacks.put(l, espTarget);
                        espTarget.items.add(itemEntity.getStack());
                    } else {
                        espTarget2.items.add(itemEntity.getStack());
                    }
                }
            }
        }
    }

    private static boolean hasActiveElement(List<EspElement> espElements) {
        for (EspElement espElement : espElements) {
            if (espElement.isActive()) {
                return true;
            }
        }

        return false;
    }

    private Esp.EspTarget nextTarget() {
        if (this.targetCount < this.targetPool.size()) {
            return this.targetPool.get(this.targetCount++);
        }

        Esp.EspTarget espTarget = new Esp.EspTarget();
        this.targetPool.add(espTarget);
        this.targetCount++;
        return espTarget;
    }

    private boolean isHiddenSelf(Entity entity) {
        if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) {
            FreeCam freeCam = FreeCam.getInstance();
            return freeCam == null || !freeCam.isEnabled();
        } else {
            return false;
        }
    }

    public static EspTargetType getTargetType(Entity entity) {
        if (entity instanceof PlayerEntity playerEntity) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (playerEntity == minecraftClient.player) {
                return EspTargetType.SELF;
            } else if (Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name())) {
                return EspTargetType.FRIENDS;
            } else if (playerEntity.isInvisible() && isNaked(playerEntity)) {
                return EspTargetType.INVISIBLE;
            } else {
                return isNaked(playerEntity) ? EspTargetType.NAKED : EspTargetType.OTHERS;
            }
        } else if (entity instanceof AnimalEntity) {
            return EspTargetType.ANIMAL;
        } else if (entity instanceof MobEntity) {
            return EspTargetType.MOB;
        } else {
            return entity instanceof ItemEntity ? EspTargetType.ITEM : null;
        }
    }

    private static boolean isNaked(PlayerEntity playerEntity) {
        return playerEntity.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
            && playerEntity.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
            && playerEntity.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
            && playerEntity.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    private static List<EspElement> createElements() {
        ArrayList arrayList = new ArrayList();

        for (EspTargetType espTargetType : EspTargetType.values()) {
            arrayList.add(new NametagElement(espTargetType));
            arrayList.add(new BoxElement(espTargetType));
            arrayList.add(new GlowElement(espTargetType));
            arrayList.add(new ChamsElement(espTargetType));
            if (espTargetType != EspTargetType.ITEM) {
                arrayList.add(new HealthBarElement(espTargetType));
                arrayList.add(new PotionsElement(espTargetType));
            }

            if (espTargetType.isPlayerType()) {
                if (espTargetType != EspTargetType.NAKED && espTargetType != EspTargetType.INVISIBLE) {
                    arrayList.add(new ArmorElement(espTargetType));
                }

                arrayList.add(new HeldItemsElement(espTargetType));
                arrayList.add(new CooldownsElement(espTargetType));
            }
        }

        return Collections.unmodifiableList(arrayList);
    }

    private static EnumMap<EspTargetType, List<EspElement>> groupByTarget(List<EspElement> espElements) {
        EnumMap enumMap = new EnumMap<>(EspTargetType.class);

        for (EspTargetType espTargetType : EspTargetType.values()) {
            ArrayList<EspElement> espElements2 = new ArrayList<>();

            for (EspElement espElement : espElements) {
                if (espElement.isTarget(espTargetType)) {
                    espElements2.add(espElement);
                }
            }

            enumMap.put(espTargetType, Collections.unmodifiableList(espElements2));
        }

        return enumMap;
    }

    private static List<Setting<?>> collectSettings(List<EspElement> espElements) {
        ArrayList arrayList = new ArrayList();

        for (EspElement espElement : espElements) {
            arrayList.addAll(espElement.getSettings());
        }

        return arrayList;
    }

    private boolean projectBounds(Box box, Esp.EspTarget espTarget) {
        float f = Float.MAX_VALUE;
        float f2 = Float.MAX_VALUE;
        float f3 = -Float.MAX_VALUE;
        float f4 = -Float.MAX_VALUE;
        boolean flag = false;
        double[] doubles = new double[]{box.minX, box.maxX};
        double[] doubles2 = new double[]{box.minY, box.maxY};
        double[] doubles3 = new double[]{box.minZ, box.maxZ};

        for (double d : doubles) {
            for (double d2 : doubles2) {
                for (double d3 : doubles3) {
                    Vector2f vector2f = ProjectionUtil.worldToScreen(d, d2, d3);
                    if (vector2f != null) {
                        flag = true;
                        f = Math.min(f, vector2f.x);
                        f2 = Math.min(f2, vector2f.y);
                        f3 = Math.max(f3, vector2f.x);
                        f4 = Math.max(f4, vector2f.y);
                    }
                }
            }
        }

        if (!flag) {
            return false;
        }

        espTarget.minX = f;
        espTarget.minY = f2;
        espTarget.maxX = f3;
        espTarget.maxY = f4;
        return true;
    }

    private Box getInterpolatedBox(Entity entity, Vec3d vec3d) {
        Box box = entity.getBoundingBox();
        double d = (box.maxX - box.minX) / 2.0;
        double d2 = box.maxY - box.minY;
        double d3 = (box.maxZ - box.minZ) / 2.0;
        float f = entity.isSneaking() ? 0.1F : 0.0F;
        return new Box(vec3d.x - d, vec3d.y, vec3d.z - d3, vec3d.x + d, vec3d.y + d2 + 0.2F - f, vec3d.z + d3);
    }

    public List<EspElement> getAllElements() {
        return this.elements;
    }

    public ModeSetting getPerformanceMode() {
        return this.performanceMode;
    }

    public NumberSetting getBackgroundAlpha() {
        return this.backgroundAlpha;
    }

    public BooleanSetting getDormant() {
        return this.dormant;
    }

    public NumberSetting getDormantTime() {
        return this.dormantTime;
    }

    public EnumMap<EspTargetType, List<EspElement>> getDefaultElements() {
        return this.defaultElements;
    }

    public EnumMap<EspTargetType, List<EspElement>> getActiveElements() {
        return this.activeElements;
    }

    public EnumMap<EspTargetType, List<ScriptShaderElement>> getScriptElements() {
        return this.scriptElements;
    }

    public EnumMap<EspTargetType, BoxElement> getBoxElements() {
        return this.boxElements;
    }

    public EnumMap<EspTargetType, GlowElement> getGlowElements() {
        return this.glowElements;
    }

    public EnumMap<EspTargetType, ChamsElement> getChamsElements() {
        return this.chamsElements;
    }

    public EnumMap<EspTargetType, NametagElement> getNametagElements() {
        return this.nametagElements;
    }

    public List<Setting<?>> getSettings2() {
        return this.settings;
    }

    public List<Esp.EspTarget> getTargetPool() {
        return this.targetPool;
    }

    public RoundedRectRenderer.Batch getBlurBatch() {
        return this.blurBatch;
    }

    public int getTargetCount() {
        return this.targetCount;
    }

    public boolean isTargetsCollected() {
        return this.targetsCollected;
    }

    public Long2ObjectOpenHashMap<Esp.EspTarget> getItemStacks() {
        return this.itemStacks;
    }

    public Queue<Esp.PositionUpdate> getPendingPositions() {
        return this.pendingPositions;
    }

    public Map<UUID, Esp.DormantPlayer> getDormantPlayers() {
        return this.dormantPlayers;
    }

    public RemotePlayerTracker getRemotePlayerTracker() {
        return this.remotePlayerTracker;
    }

    public Map<Integer, PlayerEntity> getPlayerCache() {
        return this.playerCache;
    }

    static final class DormantPlayer {
        final OtherClientPlayerEntity entity;
        long lastSeen;

        DormantPlayer(OtherClientPlayerEntity otherClientPlayerEntity, long l) {
            this.entity = otherClientPlayerEntity;
            this.lastSeen = l;
        }
    }

    static final class EspTarget {
        Entity entity;
        EspTargetType type;
        Vec3d pos;
        float minX;
        float minY;
        float maxX;
        float maxY;
        int priority;
        List<ItemStack> items;
    }

    record PositionUpdate(int id, Vec3d pos, float yaw, float pitch) {
    }
}
