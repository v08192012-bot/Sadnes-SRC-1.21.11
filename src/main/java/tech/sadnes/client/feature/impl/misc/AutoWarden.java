package tech.sadnes.client.feature.impl.misc;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.misc.autowarden.WardenChestTracker;
import tech.sadnes.client.feature.impl.misc.autowarden.WardenItems;
import tech.sadnes.client.feature.impl.movement.Baritone;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.ServerRotationTracker;
import tech.sadnes.client.util.rotation.RotationUtil;

@FeatureInfo(
    name = "AutoWarden",
    description = "feature.impl.misc.autowarden.autowarden",
    displayName = "feature.impl.misc.autowarden.autowarden",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SERVER
)
public final class AutoWarden extends Feature {
    private final TextSetting anarchies = new TextSetting("autowarden.anarchies", "", "autowarden.anarchies.hint");
    private final BooleanSetting useSpeed = new BooleanSetting("autowarden.useSpeed", false);
    private final BooleanSetting reportKillers = new BooleanSetting("autowarden.reportKillers", false);
    private final ModeSetting lootFilter = new ModeSetting("autowarden.lootFilter", new String[]{"low", "medium", "high"}).setValue("medium");
    private final WardenChestTracker chestTracker = new WardenChestTracker();
    private final List<Integer> anarchyList = new ArrayList<>();
    private final Map<BlockPos, Integer> chestOpenCounts = new HashMap<>();
    private final Map<BlockPos, Integer> wardenPositions = new HashMap<>();
    private final TimerUtil targetSwitchTimer = new TimerUtil();
    private AutoWarden.Stage stage = AutoWarden.Stage.DEPOSIT;
    private int homeAnarchy = -1;
    private int anarchyIndex;
    private int wardenAngryUntil;
    private boolean rotateAnarchy;
    private boolean clampToArea;
    private BlockPos targetChest;
    private String pendingReport;
    private boolean enabledBaritone;
    private int useSlot = -1;
    private int previousSlot;
    private int useTicks;
    private boolean usingStarted;
    private boolean savedAvoidance;
    private int savedMaxFallHeight;
    private boolean savedBlockFreeLook;
    private double savedRandomLooking;
    private double savedRandomLooking113;
    private List<Block> savedBlocksToAvoid = List.of();

    @Override
    public void onDisable() {
        super.onDisable();
        int i = ServerManager.getFunTimeAnarchy();
        if (i >= 0) {
            this.homeAnarchy = i;
        }

        this.anarchyList.clear();

        for (String string : this.anarchies.getValue().split("[^0-9]+")) {
            if (!string.isEmpty()) {
                int i2 = Integer.parseInt(string);
                if (i2 >= 1 && i2 <= 999 && i2 != this.homeAnarchy && !this.anarchyList.contains(i2)) {
                    this.anarchyList.add(i2);
                }
            }
        }

        this.anarchyIndex = 0;
        this.stage = AutoWarden.Stage.FARM;
        this.wardenPositions.clear();
        this.enabledBaritone = false;
        ChatUtil.print(Language.translate("autowarden.quickToggle"));
        Settings settings = BaritoneAPI.getSettings();
        this.savedAvoidance = settings.avoidance.value;
        this.savedMaxFallHeight = settings.maxFallHeightNoWater.value;
        this.savedBlockFreeLook = settings.blockFreeLook.value;
        this.savedRandomLooking = settings.randomLooking.value;
        this.savedRandomLooking113 = settings.randomLooking113.value;
        this.savedBlocksToAvoid = new ArrayList<>(settings.blocksToAvoid.value);
        settings.avoidance.value = true;
        settings.maxFallHeightNoWater.value = 256;
        settings.blockFreeLook.value = true;
        settings.randomLooking.value = 1.0;
        settings.randomLooking113.value = 1.0;
        ArrayList arrayList = new ArrayList<>(this.savedBlocksToAvoid);

        for (Block block : Registries.BLOCK) {
            if (block.getDefaultState().isIn(BlockTags.CANDLES) && !arrayList.contains(block)) {
                arrayList.add(block);
            }
        }

        settings.blocksToAvoid.value = arrayList;
    }

    @Override
    public void onEnable() {
        if (this.useSlot != -1) {
            this.stopItemUse();
        }

        this.cancelPathing();
        Settings settings = BaritoneAPI.getSettings();
        settings.avoidance.value = this.savedAvoidance;
        settings.maxFallHeightNoWater.value = this.savedMaxFallHeight;
        settings.blockFreeLook.value = this.savedBlockFreeLook;
        settings.randomLooking.value = this.savedRandomLooking;
        settings.randomLooking113.value = this.savedRandomLooking113;
        settings.blocksToAvoid.value = new ArrayList<>(this.savedBlocksToAvoid);
        Baritone baritone = Baritone.getInstance();
        if (this.enabledBaritone && baritone.isEnabled()) {
            baritone.setEnabled(false);
        }

        super.onEnable();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.currentScreen instanceof DeathScreen && mc.player.deathTime >= 5) {
            mc.player.requestRespawn();
        }

        if (mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed()) {
            this.toggle();
        } else {
            this.chestTracker.update();
            this.tickItemUse();

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof WardenEntity) {
                    this.wardenPositions.put(entity.getBlockPos(), mc.player.age + 100);
                }
            }

            this.wardenPositions.values().removeIf(integer -> mc.player.age > integer);
            int i = ServerManager.getFunTimeAnarchy();
            if (this.pendingReport != null) {
                if (mc.player.age >= 20 && mc.player.age < 30) {
                    ChatUtil.sendChat("report " + this.pendingReport + " чит");
                    this.pendingReport = null;
                }
            } else if (mc.player.age < 5) {
                this.wardenAngryUntil = 0;
                this.chestOpenCounts.clear();
            } else if (i < 0) {
                if (mc.player.age % 100 == 0 && this.homeAnarchy >= 0 && mc.player.age > 300) {
                    ChatUtil.sendChat("an" + this.homeAnarchy);
                }

                this.stage = AutoWarden.Stage.DEPOSIT;
            } else {
                if (mc.player.age % 100 == 0 && this.isInWardenArea()) {
                    this.clampToArea = true;
                }

                boolean flag;
                if (mc.player.hasStatusEffect(StatusEffects.GLOWING)) {
                    flag = this.isPlayerNearby(mc.player.getEntityPos(), 32.0);
                } else {
                    flag = false;
                }

                if (flag) {
                    this.fleeFromThreats(false);
                } else {
                    switch (this.stage) {
                        case DEPOSIT:
                            this.tickDeposit(i);
                            break;
                        case RESTOCK:
                            this.tickRestock(i);
                            break;
                        case FARM:
                            this.tickFarm(i);
                            break;
                        case ESCAPE:
                            this.tickEscape(i);
                    }

                    if (this.isStuck() && mc.player.age % 15 == 0) {
                        this.cancelPathing();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                String string = gameMessageS2CPacket.content().getString();
                mc.execute(() -> this.handleChatMessage(string));
            } else if (packetEvent.getPacket() instanceof PlaySoundS2CPacket playSoundS2CPacket) {
                String string2 = playSoundS2CPacket.getSound().value().id().getPath();
                mc.execute(() -> this.handleSound(string2));
            } else {
                if (packetEvent.getPacket() instanceof PlaySoundFromEntityS2CPacket playSoundFromEntityS2CPacket) {
                    String string3 = playSoundFromEntityS2CPacket.getSound().value().id().getPath();
                    mc.execute(() -> this.handleSound(string3));
                }
            }
        }
    }

    @EventHandler
    public void onMovementInput(MoveInputEvent moveInputEvent) {
        if (mc.player != null) {
            if (!mc.player.isOnGround() && !mc.player.isClimbing()) {
                moveInputEvent.setSneak(false);
            }

            if (this.isStuck() && mc.player.getMainHandStack().isEmpty() && !this.isNearChest(3.0)) {
                if (mc.player.age % 10 <= ThreadLocalRandom.current().nextFloat(3.0F, 8.0F)) {
                    moveInputEvent.setForward(-1.0F);
                    moveInputEvent.setStrafe(-1.0F);
                } else {
                    moveInputEvent.setForward(1.0F);
                    moveInputEvent.setStrafe(1.0F);
                }
            }
        }
    }

    private void handleChatMessage(String string) {
        if (mc.player != null && string.contains("Помянем. Вы погибли")) {
            this.rotateAnarchy = true;
            if (this.reportKillers.getValue() && string.contains("Вас убил")) {
                StringBuilder builder = new StringBuilder();

                for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
                    builder.append(statusEffectInstance.getEffectType().value().getName().getString()).append(' ');
                }

                ChatUtil.print(
                    Language.translate("autowarden.deathEffects"), builder.isEmpty() ? Language.translate("autowarden.deathEffects.none") : builder.toString().trim()
                );
                if (!mc.player.hasStatusEffect(StatusEffects.GLOWING) && !this.isNearChest(2.0)) {
                    this.pendingReport = string.split("Вас убил ")[1].split(",")[0].trim();
                }
            }
        }
    }

    private void handleSound(String string) {
        boolean flag;
        if (!string.contains("warden.roar") && !string.contains("warden.angry")) {
            flag = string.contains("warden.sonic");
        } else {
            flag = true;
        }

        if (mc.player != null && flag) {
            this.wardenAngryUntil = mc.player.age + 100;
        }
    }

    private void tickDeposit(int i) {
        if (this.homeAnarchy >= 0 && i != this.homeAnarchy && !ServerManager.isInPvp() && mc.player.age % 5 == 0 && mc.player.age > 5) {
            ChatUtil.sendChat("an" + this.homeAnarchy);
        }

        if (i == this.homeAnarchy) {
            this.freeMainHand();
            this.handleStorage(this.hasItemsToDeposit(), true, AutoWarden.Stage.RESTOCK);
        }
    }

    private void tickRestock(int i) {
        if (this.rotateAnarchy && !this.anarchyList.isEmpty()) {
            this.anarchyIndex = (this.anarchyIndex + 1) % this.anarchyList.size();
            this.rotateAnarchy = false;
        }

        this.wardenAngryUntil = 0;
        this.chestOpenCounts.clear();
        if (mc.player.age % 200 == 0 && !this.hasCursorStack()) {
            mc.player.closeScreen();
        }

        if (i == this.homeAnarchy) {
            if (this.needsRestock() || this.hasCursorStack()) {
                this.handleStorage(true, false, AutoWarden.Stage.FARM);
                return;
            }

            this.handleStorage(false, false, AutoWarden.Stage.FARM);
        }
    }

    private void tickFarm(int i) {
        if (!this.checkShouldEscape()) {
            if (mc.player.getHungerManager().getFoodLevel() < 18) {
                this.startUsingSlot(this.findFoodSlot());
            }

            if (this.useSlot != -1) {
                if (this.isMoving()) {
                    this.cancelPathing();
                }
            } else if (this.anarchyList.isEmpty()) {
                if (mc.player.age % 20 == 0) {
                    ChatUtil.print(Language.translate("autowarden.noAnarchies"));
                    return;
                }
            } else {
                if (this.anarchyIndex >= this.anarchyList.size()) {
                    this.anarchyIndex = 0;
                }

                int i2 = this.anarchyList.get(this.anarchyIndex);
                if (i != i2) {
                    if (mc.player.age % 10 == 0 && mc.player.age > 10) {
                        ChatUtil.sendChat("an" + i2);
                        return;
                    }
                } else {
                    if (mc.player.age <= 5) {
                        return;
                    }

                    StatusEffectInstance statusEffectInstance = mc.player.getStatusEffect(StatusEffects.INVISIBILITY);
                    boolean flag;
                    if (!mc.player.hasStatusEffect(StatusEffects.GLOWING) && (statusEffectInstance == null || statusEffectInstance.getDuration() < 400)) {
                        flag = false;
                        boolean flag2 = statusEffectInstance == null && this.countItems(WardenItems::isInvisibilityPotion) < 1;
                        if (flag2 && mc.player.age % 5 == 0 && !ServerManager.isInPvp()) {
                            this.stage = AutoWarden.Stage.ESCAPE;
                            return;
                        }

                        if (mc.player.age > 20) {
                            this.startUsingSlot(this.findInventorySlot(WardenItems::isInvisibilityPotion));
                        }
                    } else {
                        flag = true;
                    }

                    if (this.isInWardenArea()) {
                        if (!flag) {
                            return;
                        }

                        int i3 = this.useSpeed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)
                            ? this.findInventorySlot(WardenItems::isSpeedPotion)
                            : -1;
                        if (i3 >= 0) {
                            this.startUsingSlot(i3);
                            return;
                        }

                        this.lootChests();
                        return;
                    }

                    if (mc.player.age % 50 == 0) {
                        ChatUtil.sendChat("home");
                        return;
                    }
                }
            }
        }
    }

    private void tickEscape(int i) {
        if (this.homeAnarchy >= 0 && i == this.homeAnarchy) {
            this.stage = AutoWarden.Stage.DEPOSIT;
        } else {
            boolean flag = ServerManager.isInPvp();
            if (this.isWardenThreatening() && flag) {
                this.fleeFromThreats(true);
            } else {
                BlockPos blockPos = this.findBestChest();
                boolean flag2 = blockPos != null
                    && this.chestTracker.getRefillTime(blockPos) < 0L
                    && mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= 16.0;
                if (mc.currentScreen instanceof GenericContainerScreen || flag2) {
                    this.lootChests();
                } else if (!flag) {
                    this.stage = AutoWarden.Stage.DEPOSIT;
                } else {
                    boolean flag3 = this.countOccupiedSlots() < 23
                        && !this.isPlayerNearby(mc.player.getEntityPos(), 2.0)
                        && ServerManager.getPvpTime() > 16
                        && blockPos != null;
                    if (flag3) {
                        this.lootChests();
                    } else {
                        this.fleeFromThreats(true);
                    }
                }
            }
        }
    }

    private boolean checkShouldEscape() {
        boolean flag = this.isWardenThreatening();
        int i = this.countOccupiedSlots();
        boolean flag2 = this.chestOpenCounts.values().stream().filter(integer -> integer >= 2).count() >= 3L && mc.player.age % 30 == 0;
        boolean flag3;
        if (!flag && i <= this.scaleByLootFilter(20) && mc.player.getHungerManager().getFoodLevel() >= 8) {
            flag3 = flag2;
        } else {
            flag3 = true;
        }

        if (flag3 && mc.player.age > 100) {
            if (flag) {
                this.rotateAnarchy = true;
            }

            this.stage = AutoWarden.Stage.ESCAPE;
            return true;
        } else {
            int i2 = ServerManager.getPvpTime();
            boolean flag4 = i2 < 0 && i > this.scaleByLootFilter(8);
            if (i2 >= 0 && i2 < 7 && !this.isPlayerNearby(mc.player.getEntityPos(), 14.0) && i > this.scaleByLootFilter(7)) {
                this.stage = AutoWarden.Stage.ESCAPE;
                return true;
            }

            if (!flag4) {
                return false;
            }

            this.stage = AutoWarden.Stage.ESCAPE;
            return true;
        }
    }

    private int scaleByLootFilter(int i) {
        double d = i;
        if (this.lootFilter.is("low")) {
            return (int)(d * 1.5);
        } else {
            return this.lootFilter.is("high") ? (int)(d * 0.8) : (int)(d * 1.0);
        }
    }

    private boolean isWardenThreatening() {
        if (mc.player.age >= this.wardenAngryUntil) {
            return false;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof WardenEntity wardenEntity) {
                double d = mc.player.squaredDistanceTo(wardenEntity);
                double d2 = mc.player.getX() - wardenEntity.getX();
                double d3 = mc.player.getZ() - wardenEntity.getZ();
                double d4 = Math.toDegrees(Math.atan2(-d2, d3));
                boolean flag = Math.abs(((wardenEntity.getBodyYaw() - d4) % 360.0 + 540.0) % 360.0 - 180.0) < 10.0;
                boolean flag2 = d2 * (wardenEntity.getX() - wardenEntity.lastX) + d3 * (wardenEntity.getZ() - wardenEntity.lastZ) > 0.01;
                if (d < 900.0 && flag && (d < 16.0 || flag2)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void fleeFromThreats(boolean flag) {
        this.closeContainer();
        this.freeMainHand();
        BlockPos blockPos = null;
        double d = -1.0;
        int i = mc.player.getBlockPos().getY();

        for (byte b = 0; b < 360; b += 30) {
            int i2 = this.clampX((int)(mc.player.getX() + Math.cos(Math.toRadians(b)) * 25.0));
            int i3 = this.clampZ((int)(mc.player.getZ() + Math.sin(Math.toRadians(b)) * 25.0));
            double d2 = Double.MAX_VALUE;

            for (Entity entity : mc.world.getEntities()) {
                boolean flag2;
                label38: {
                    boolean flag3;
                    label37: {
                        if (entity != mc.player) {
                            if (entity instanceof PlayerEntity) {
                                flag3 = true;
                                break label37;
                            }

                            if (flag) {
                                flag2 = entity instanceof WardenEntity;
                                break label38;
                            }
                        }

                        flag3 = false;
                    }

                    flag2 = flag3;
                }

                if (flag2) {
                    d2 = Math.min(d2, Math.hypot(entity.getX() - i2, entity.getZ() - i3));
                }
            }

            if (!(d2 <= d)) {
                d = d2;
                blockPos = new BlockPos(i2, i, i3);
            }
        }

        this.pathTo(blockPos);
    }

    private void lootChests() {
        if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            this.takeLoot(genericContainerScreen);
        } else {
            BlockPos blockPos = this.findBestChest();
            if (blockPos == null) {
                blockPos = this.findSoonestRefillChest();
            }

            boolean flag = blockPos != null
                && this.targetChest != null
                && !blockPos.equals(this.targetChest)
                && this.chestTracker.getRefillTime(this.targetChest) > 25000L;
            if (!flag) {
                this.targetSwitchTimer.reset();
            }

            if (!flag || this.targetSwitchTimer.hasPassed(1000L)) {
                this.targetChest = blockPos;
            }

            BlockPos blockPos2 = this.targetChest;
            if (blockPos2 == null) {
                if (mc.player.age % 40 == 0) {
                    this.stage = AutoWarden.Stage.ESCAPE;
                    this.rotateAnarchy = true;
                }
            } else {
                long l = this.chestTracker.getRefillTime(blockPos2);
                if (l > 1000L && this.isPlayerNearby(Vec3d.ofCenter(blockPos2), 7.0)) {
                    BlockPos blockPos3 = this.findStandPos(blockPos2);
                    if (blockPos3 != null) {
                        if (mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos3)) <= 2.0) {
                            this.cancelPathing();
                        } else {
                            this.pathTo(blockPos3);
                        }
                    }
                } else if (l > 6000L) {
                    double d = mc.player.age / 40 * 2.4;
                    this.pathTo(
                        new BlockPos(
                            this.clampX(blockPos2.getX() + (int)(Math.cos(d) * 10.0)), blockPos2.getY(), this.clampZ(blockPos2.getZ() + (int)(Math.sin(d) * 10.0))
                        )
                    );
                } else {
                    double d2 = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos2));
                    if (d2 <= 20.0) {
                        boolean flag2 = l >= 0L;
                        boolean flag3 = this.chestOpenCounts.getOrDefault(blockPos2, 0) < (flag2 ? 1 : 3);
                        if (flag3 && this.openChest(blockPos2, flag2 ? 6 : 1)) {
                            this.chestOpenCounts.merge(blockPos2, 1, Integer::sum);
                        }
                    } else {
                        if (d2 > 10.0) {
                            this.freeMainHand();
                        }

                        this.pathTo(this.findStandPos(blockPos2));
                    }
                }
            }
        }
    }

    private BlockPos findSoonestRefillChest() {
        BlockPos blockPos = null;
        long l = 45000L;

        for (BlockPos blockPos2 : this.chestTracker.getChests()) {
            long l2 = this.chestTracker.getRefillTime(blockPos2);
            if (l2 >= 0L && l2 < l && this.findStandPos(blockPos2) != null && !this.isNearWarden(blockPos2) && !this.isGuardedByPlayer(blockPos2)) {
                l = l2;
                blockPos = blockPos2;
            }
        }

        return blockPos;
    }

    private BlockPos findBestChest() {
        BlockPos blockPos = null;
        byte b = 99;
        double d = Double.MAX_VALUE;

        for (BlockPos blockPos2 : this.chestTracker.getChests()) {
            if (this.findStandPos(blockPos2) != null && !this.isGuardedByPlayer(blockPos2)) {
                double d2 = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos2));
                long l = this.chestTracker.getRefillTime(blockPos2);
                boolean flag = l < 0L;
                boolean flag2 = !this.isNearWarden(blockPos2) || flag && d2 <= 16.0;
                if ((!flag || this.chestOpenCounts.getOrDefault(blockPos2, 0) < 3) && flag2) {
                    byte b2;
                    if (flag && d2 <= 25.0) {
                        b2 = 0;
                    } else if (!flag && l <= 5000L && d2 <= 144.0) {
                        b2 = 1;
                    } else if (flag && d2 <= 144.0) {
                        b2 = 2;
                    } else if (!flag && l <= 15000L && d2 <= 625.0) {
                        b2 = 3;
                    } else {
                        if (!flag) {
                            continue;
                        }

                        b2 = 4;
                    }

                    double d3 = Vec3d.ofCenter(blockPos2).y - mc.player.getEyeY();
                    double d4 = blockPos2.getX() + 0.5 - mc.player.getX();
                    double d5 = blockPos2.getZ() + 0.5 - mc.player.getZ();
                    double d6 = d4 * d4 + d5 * d5 + (d3 > 0.0 ? 2 : 1) * d3 * d3;
                    if (b2 <= b && (b2 != b || !(d6 >= d))) {
                        b = b2;
                        d = d6;
                        blockPos = blockPos2;
                    }
                }
            }
        }

        return blockPos;
    }

    private BlockPos findStandPos(BlockPos blockPos) {
        double d = mc.player.getEyeHeight(mc.player.getPose()) - 0.5;

        for (int i = -1; i <= 1; i++) {
            for (int i2 = -1; i2 <= 1; i2++) {
                if (i != 0 || i2 != 0) {
                    BlockPos blockPos2 = blockPos.add(i, 0, i2);
                    boolean flag;
                    if (mc.world.getBlockState(blockPos2).isAir() && mc.world.getBlockState(blockPos2.up()).isAir()) {
                        flag = !mc.world.getBlockState(blockPos2.down()).isAir();
                    } else {
                        flag = false;
                    }

                    if (flag && this.findVisiblePoint(Vec3d.ofCenter(blockPos2).add(0.0, d, 0.0), blockPos) != null) {
                        return blockPos2;
                    }
                }
            }
        }

        BlockPos blockPos3 = blockPos.up();
        boolean flag2;
        if (mc.world.getBlockState(blockPos3).isAir()) {
            flag2 = mc.world.getBlockState(blockPos3.up()).isAir();
        } else {
            flag2 = false;
        }

        return flag2 && this.findVisiblePoint(Vec3d.ofCenter(blockPos3).add(0.0, d, 0.0), blockPos) != null ? blockPos3 : null;
    }

    private Vec3d findVisiblePoint(Vec3d vec3d, BlockPos blockPos) {
        Vec3d vec3d2 = Vec3d.ofCenter(blockPos);
        Vec3d vec3d3 = null;
        double d = Double.MAX_VALUE;

        for (int i = -1; i <= 1; i++) {
            for (int i2 = -1; i2 <= 1; i2++) {
                for (int i3 = -1; i3 <= 1; i3++) {
                    Vec3d vec3d4 = vec3d2.add(i * 0.4, i2 * 0.4, i3 * 0.4);
                    double d2 = vec3d4.squaredDistanceTo(vec3d2);
                    if (!(d2 >= d)) {
                        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d, vec3d4, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
                        if (blockHitResult.getBlockPos().equals(blockPos)) {
                            d = d2;
                            vec3d3 = vec3d4;
                        }
                    }
                }
            }
        }

        return vec3d3;
    }

    private boolean openChest(BlockPos blockPos, int i) {
        if (blockPos != null && !(mc.currentScreen instanceof GenericContainerScreen)) {
            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = this.findVisiblePoint(vec3d, blockPos);
            if (vec3d2 == null) {
                return false;
            }

            Rotation rotation = new Rotation(RotationUtil.getRotations(vec3d, vec3d2));
            float f = mc.player.age + mc.getRenderTickCounter().getTickProgress(false);
            float f2 = (float)((Math.sin(f * 0.31F) * 0.5 + Math.sin(f * 0.73F + 1.1F) * 0.3 + Math.sin(f * 1.7F + 2.6F) * 0.2) * 8.0);
            RotationComponent.rotateTo(new Rotation(rotation.getYaw() + f2, MathHelper.clamp(rotation.getPitch() + f2 / 4.0F, -90.0F, 90.0F)), 40.0F, 40.0F, 2, 10);
            boolean flag = new Rotation(ServerRotationTracker.getServerYaw(), ServerRotationTracker.getServerPitch()).distanceTo(rotation) <= 5.0F;
            if (mc.player.age % i == 0) {
                if (!flag) {
                    return false;
                }

                BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
                if (!blockHitResult.getBlockPos().equals(blockPos)) {
                    return false;
                }

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
                mc.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }

        return false;
    }

    private void takeLoot(GenericContainerScreen genericContainerScreen) {
        if (this.isMoving()) {
            this.cancelPathing();
        } else if (mc.player.age % 2 == 0) {
            Slot slot = this.findSlot(genericContainerScreen, false, itemStack -> !itemStack.isEmpty() && !WardenItems.isWorthLooting(itemStack, this.lootFilter.getValue()));
            if (slot != null) {
                this.clickSlot(genericContainerScreen, slot, 0, SlotActionType.QUICK_MOVE);
            } else {
                this.closeContainer();
            }
        }
    }

    private void handleStorage(boolean flag, boolean flag2, AutoWarden.Stage stage2) {
        if (!flag) {
            if (this.closeContainer()) {
                this.stage = stage2;
            }
        } else if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            if (flag2) {
                this.depositItems(genericContainerScreen);
            } else {
                this.restockItems(genericContainerScreen);
            }
        } else {
            this.openChest(this.findStorageChest(flag2), 2);
        }
    }

    private void depositItems(GenericContainerScreen genericContainerScreen) {
        if (mc.player.age % 2 == 0) {
            boolean flag = false;
            boolean flag2 = false;
            int i = 0;

            for (Slot slot : genericContainerScreen.getScreenHandler().slots) {
                if (i >= 4) {
                    return;
                }

                ItemStack itemStack = slot.getStack();
                if (slot.inventory == mc.player.getInventory() && !itemStack.isEmpty() && (!this.useSpeed.getValue() || !WardenItems.isSpeedPotion(itemStack))) {
                    if (!flag && WardenItems.isInvisibilityPotion(itemStack)) {
                        flag = true;
                    } else if (!flag2 && itemStack.isOf(Items.GOLDEN_CARROT)) {
                        flag2 = true;
                    } else {
                        this.clickSlot(genericContainerScreen, slot, 0, SlotActionType.QUICK_MOVE);
                        i++;
                    }
                }
            }
        }
    }

    private boolean hasItemsToDeposit() {
        boolean flag = false;
        boolean flag2 = false;

        for (ItemStack itemStack : mc.player.getInventory().getMainStacks()) {
            if (!itemStack.isEmpty() && (!this.useSpeed.getValue() || !WardenItems.isSpeedPotion(itemStack))) {
                if (flag || !WardenItems.isInvisibilityPotion(itemStack)) {
                    if (flag2 || !itemStack.isOf(Items.GOLDEN_CARROT)) {
                        return true;
                    }

                    flag2 = true;
                } else {
                    flag = true;
                }
            }
        }

        return false;
    }

    private void restockItems(GenericContainerScreen genericContainerScreen) {
        if (mc.player.age % 2 == 0) {
            ItemStack itemStack = genericContainerScreen.getScreenHandler().getCursorStack();
            Predicate<ItemStack> predicate = itemStack2 -> itemStack2.isEmpty() || ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack);
            if (itemStack.isEmpty()) {
                this.clickSlot(genericContainerScreen, this.findSlot(genericContainerScreen, false, this::isRestockItemNeeded), 0, SlotActionType.PICKUP);
            } else if (this.isRestockItemNeeded(itemStack)) {
                this.clickSlot(genericContainerScreen, this.findSlot(genericContainerScreen, true, predicate), 1, SlotActionType.PICKUP);
            } else {
                this.clickSlot(genericContainerScreen, this.findSlot(genericContainerScreen, false, predicate), 0, SlotActionType.PICKUP);
            }
        }
    }

    private boolean isRestockItemNeeded(ItemStack itemStack) {
        return itemStack.isEmpty()
            ? false
            : WardenItems.isInvisibilityPotion(itemStack) && this.countItems(WardenItems::isInvisibilityPotion) < 1
                || itemStack.isOf(Items.GOLDEN_CARROT) && this.countItems(itemStack2 -> itemStack2.isOf(Items.GOLDEN_CARROT)) < 3
                || this.useSpeed.getValue() && WardenItems.isSpeedPotion(itemStack) && this.findInventorySlot(WardenItems::isSpeedPotion) < 0;
    }

    private boolean needsRestock() {
        return this.countItems(WardenItems::isInvisibilityPotion) < 1
            || this.countItems(itemStack -> itemStack.isOf(Items.GOLDEN_CARROT)) < 3
            || this.useSpeed.getValue() && this.findInventorySlot(WardenItems::isSpeedPotion) < 0;
    }

    private BlockPos findStorageChest(boolean flag) {
        BlockPos blockPos = mc.player.getBlockPos();
        Mutable mutable = new Mutable();
        int i = -4;
        int i2 = -4;
        int i3 = -4;

        while (true) {
            while (i3 > 4) {
                if (++i2 <= 4) {
                    i3 = -4;
                } else {
                    if (++i > 4) {
                        return null;
                    }

                    i2 = -4;
                    i3 = -4;
                }
            }

            mutable.set(blockPos.getX() + i, blockPos.getY() + i2, blockPos.getZ() + i3);
            if (mc.world.getBlockState(mutable).isOf(Blocks.CHEST) && this.isRestockChest(mutable) == flag && mc.world.getBlockState(mutable.up()).isAir()) {
                BlockHitResult blockHitResult = mc.world
                    .raycast(new RaycastContext(mc.player.getEyePos(), Vec3d.ofCenter(mutable), ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
                if (blockHitResult.getBlockPos().equals(mutable)) {
                    return mutable.toImmutable();
                }
            }

            i3++;
        }
    }

    private boolean isRestockChest(BlockPos blockPos) {
        if (mc.world.getBlockState(blockPos.down()).isOf(Blocks.HOPPER)) {
            return true;
        }

        BlockState blockState = mc.world.getBlockState(blockPos);
        if (blockState.get(Properties.CHEST_TYPE) == ChestType.SINGLE) {
            return false;
        }

        for (Direction direction : Type.HORIZONTAL) {
            BlockPos blockPos2 = blockPos.offset(direction);
            BlockState blockState2 = mc.world.getBlockState(blockPos2);
            boolean flag = blockState2.isOf(Blocks.CHEST)
                && blockState2.get(Properties.CHEST_TYPE) != ChestType.SINGLE
                && blockState2.get(Properties.CHEST_TYPE) != blockState.get(Properties.CHEST_TYPE)
                && blockState2.get(Properties.HORIZONTAL_FACING) == blockState.get(Properties.HORIZONTAL_FACING);
            if (flag && mc.world.getBlockState(blockPos2.down()).isOf(Blocks.HOPPER)) {
                return true;
            }
        }

        return false;
    }

    private void startUsingSlot(int i) {
        if (i >= 0 && this.useSlot == -1) {
            this.useSlot = i;
            this.previousSlot = InventoryUtil.getSelectedSlot();
            this.useTicks = 0;
            this.usingStarted = false;
        }
    }

    private void tickItemUse() {
        if (this.useSlot != -1 && mc.player.age > 40) {
            this.useTicks++;
            if (this.useTicks == 1) {
                if (this.useSlot > 8) {
                    InventoryUtil.swapToHand(this.useSlot, Hand.MAIN_HAND);
                } else {
                    InventoryUtil.setSelectedSlot(this.useSlot);
                }
            } else if (!this.usingStarted) {
                if (mc.player.isUsingItem()) {
                    this.usingStarted = true;
                } else {
                    mc.options.useKey.setPressed(true);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
            } else if (!mc.player.isUsingItem()) {
                this.stopItemUse();
                return;
            }

            if (this.useTicks >= 60) {
                ChatUtil.print(Language.translate("autowarden.useFailed"));
                this.stopItemUse();
            }
        }
    }

    private void stopItemUse() {
        mc.options.useKey.setPressed(false);
        if (this.useSlot > 8) {
            InventoryUtil.swapToHand(this.useSlot, Hand.MAIN_HAND);
            this.useSlot = -1;
        } else {
            InventoryUtil.setSelectedSlot(this.previousSlot);
            this.useSlot = -1;
        }
    }

    private int findFoodSlot() {
        int i = -1;
        int i2 = Integer.MAX_VALUE;

        for (int i3 = 0; i3 < 36 && i2 > 0; i3++) {
            int i4 = WardenItems.getFoodPriority(mc.player.getInventory().getStack(i3));
            if (i4 >= 0 && i4 < i2) {
                i2 = i4;
                i = i3;
            }
        }

        return i;
    }

    private void freeMainHand() {
        if (!mc.player.getMainHandStack().isEmpty()) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).isEmpty()) {
                    InventoryUtil.setSelectedSlot(i);
                    return;
                }
            }

            for (int i2 = 9; i2 < 36; i2++) {
                if (mc.player.getInventory().getStack(i2).isEmpty()) {
                    if (mc.player.age % 10 >= 2 && this.isMoving()) {
                        this.cancelPathing();
                    }

                    if (mc.player.age % 10 == 4) {
                        InventoryUtil.swapToHand(i2, Hand.MAIN_HAND);
                    }
                    break;
                }
            }
        }
    }

    private boolean closeContainer() {
        if (mc.currentScreen instanceof GenericContainerScreen && mc.player.age % 2 == 0) {
            mc.player.closeHandledScreen();
        }

        return !(mc.currentScreen instanceof GenericContainerScreen);
    }

    private Slot findSlot(GenericContainerScreen genericContainerScreen, boolean flag, Predicate<ItemStack> predicate) {
        for (Slot slot : genericContainerScreen.getScreenHandler().slots) {
            if (slot.inventory == mc.player.getInventory() == flag && predicate.test(slot.getStack())) {
                return slot;
            }
        }

        return null;
    }

    private void clickSlot(GenericContainerScreen genericContainerScreen, Slot slot, int i, SlotActionType slotActionType) {
        if (slot != null) {
            mc.interactionManager.clickSlot(genericContainerScreen.getScreenHandler().syncId, slot.id, i, slotActionType, mc.player);
        }
    }

    private boolean hasCursorStack() {
        Screen screen = mc.currentScreen;
        return screen instanceof GenericContainerScreen && !((GenericContainerScreen)screen).getScreenHandler().getCursorStack().isEmpty();
    }

    private int findInventorySlot(Predicate<ItemStack> predicate) {
        for (int i = 0; i < 36; i++) {
            if (predicate.test(mc.player.getInventory().getStack(i))) {
                return i;
            }
        }

        return -1;
    }

    private int countItems(Predicate<ItemStack> predicate) {
        int i = 0;

        for (ItemStack itemStack : mc.player.getInventory().getMainStacks()) {
            if (!itemStack.isEmpty() && predicate.test(itemStack)) {
                i += itemStack.getCount();
            }
        }

        return i;
    }

    private int countOccupiedSlots() {
        int i = 0;

        for (ItemStack itemStack : mc.player.getInventory().getMainStacks()) {
            if (!itemStack.isEmpty()) {
                i++;
            }
        }

        return i;
    }

    private boolean isStuck() {
        BlockPos blockPos = mc.player.getBlockPos();
        boolean flag;
        if (!mc.world.getBlockState(blockPos).isIn(BlockTags.CANDLES)) {
            flag = mc.world.getBlockState(blockPos.down()).isIn(BlockTags.CANDLES);
        } else {
            flag = true;
        }

        if (flag) {
            return true;
        }

        if (this.stage == AutoWarden.Stage.FARM
            && !this.isWardenThreatening()
            && this.isInWardenArea()
            && mc.currentScreen == null
            && !this.isMoving()
            && this.useSlot == -1) {
            Box box = mc.player.getBoundingBox().expand(0.05, 0.0, 0.05);

            for (BlockPos blockPos2 : BlockPos.iterate(BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {
                if (!mc.world.getBlockState(blockPos2).isAir()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private boolean isMoving() {
        return mc.player.getVelocity().horizontalLengthSquared() > 0.0025;
    }

    private boolean isInWardenArea() {
        return mc.world.getRegistryKey() == World.OVERWORLD
            && mc.player.getX() <= -1921.0
            && mc.player.getX() >= -2070.0
            && mc.player.getZ() <= -1929.0
            && mc.player.getZ() >= -2076.0;
    }

    private int clampX(int i) {
        return this.clampToArea ? MathHelper.clamp(i, -2060, -1931) : i;
    }

    private int clampZ(int i) {
        return this.clampToArea ? MathHelper.clamp(i, -2066, -1939) : i;
    }

    private boolean isNearChest(double d) {
        for (BlockPos blockPos : this.chestTracker.getChests()) {
            if (mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= d * d) {
                return true;
            }
        }

        return false;
    }

    private boolean isNearWarden(BlockPos blockPos) {
        for (BlockPos blockPos2 : this.wardenPositions.keySet()) {
            if (blockPos2.getSquaredDistance(blockPos) < 25.0) {
                return true;
            }
        }

        return false;
    }

    private boolean isPlayerNearby(Vec3d vec3d, double d) {
        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if (playerEntity != mc.player && playerEntity.getEntityPos().squaredDistanceTo(vec3d) < d * d) {
                return true;
            }
        }

        return false;
    }

    private boolean isGuardedByPlayer(BlockPos blockPos) {
        Vec3d vec3d = Vec3d.ofCenter(blockPos);

        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if (playerEntity != mc.player && !(playerEntity.getEntityPos().squaredDistanceTo(vec3d) >= 20.0)) {
                for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    if (WardenItems.hasEnchantedTrim(playerEntity.getEquippedStack(equipmentSlot))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void pathTo(BlockPos blockPos) {
        if ((mc.player.age % 10 == 0 || !this.isMoving() && mc.player.age % 5 == 0) && blockPos != null) {
            Baritone baritone = Baritone.getInstance();
            if (!baritone.isEnabled()) {
                baritone.setEnabled(true);
                this.enabledBaritone = true;
            }

            IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (iBaritone != null) {
                iBaritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.clampX(blockPos.getX()), blockPos.getY(), this.clampZ(blockPos.getZ())));
                return;
            }
        }
    }

    private void cancelPathing() {
        IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (iBaritone != null) {
            iBaritone.getPathingBehavior().cancelEverything();
        }
    }

    enum Stage {
        DEPOSIT,
        RESTOCK,
        FARM,
        ESCAPE;
    }
}
