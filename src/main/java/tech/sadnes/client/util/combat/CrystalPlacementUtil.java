package tech.sadnes.client.util.combat;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.attribute.EnvironmentAttributes;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.player.AimPointUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.RotationUtil;

public final class CrystalPlacementUtil implements IMinecraft {
    private CrystalPlacementUtil() {
    }

    public static CrystalPlacementUtil.PlacePosition findNearestPlacePos(Item item, Predicate<BlockPos> predicate) {
        Vec3d vec3d = mc.player.getEyePos();
        BlockPos blockPos = BlockPos.ofFloored(vec3d);
        CrystalPlacementUtil.PlacePosition placePosition = null;
        double d = Double.MAX_VALUE;

        for (BlockPos blockPos2 : BlockPos.iterate(blockPos.add(-4, -4, -4), blockPos.add(4, 3, 4))) {
            double d2 = vec3d.squaredDistanceTo(Vec3d.ofCenter(blockPos2));
            if (d2 < d && canPlaceItem(blockPos2, item) && mc.world.getFluidState(blockPos2).isEmpty()) {
                BlockPos blockPos3 = blockPos2.toImmutable();
                if (predicate.test(blockPos3)) {
                    BlockHitResult blockHitResult = getPlaceHit(blockPos3, false);
                    if (blockHitResult != null) {
                        d = d2;
                        placePosition = new CrystalPlacementUtil.PlacePosition(blockPos3, blockHitResult);
                    }
                }
            }
        }

        return placePosition;
    }

    public static LivingEntity findTarget() {
        AttackAura attackAura = AttackAura.getInstance();
        if (attackAura != null && attackAura.isEnabled() && isValidTarget(attackAura.getCurrentTarget())) {
            return attackAura.getCurrentTarget();
        }

        LivingEntity livingEntity = TargetComponent.getCurrentTarget();
        if (isValidTarget(livingEntity)) {
            return livingEntity;
        }

        Vec3d vec3d = mc.player.getEyePos();
        Vec3d vec3d2 = mc.player.getRotationVec(1.0F);
        PlayerEntity playerEntity = null;
        double d = Double.MAX_VALUE;

        for (PlayerEntity playerEntity2 : mc.world.getPlayers()) {
            if (isEnemyPlayer(playerEntity2)) {
                double d2 = mc.player.distanceTo(playerEntity2);
                if (!(d2 > 10.0)) {
                    double d3 = vec3d2.dotProduct(playerEntity2.getEntityPos().subtract(vec3d).normalize());
                    if (!(d3 < 0.0) || !(d2 > 3.5)) {
                        double d4 = d2 + (1.0 - d3) * 4.0;
                        if (!(d4 >= d)) {
                            d = d4;
                            playerEntity = playerEntity2;
                        }
                    }
                }
            }
        }

        return playerEntity;
    }

    public static boolean isValidTarget(LivingEntity livingEntity) {
        return livingEntity != null && livingEntity.isAlive() && mc.player.distanceTo(livingEntity) <= 10.0F;
    }

    private static boolean isEnemyPlayer(PlayerEntity playerEntity) {
        return playerEntity != mc.player
            && playerEntity.isAlive()
            && !playerEntity.isSpectator()
            && !playerEntity.isCreative()
            && !Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name());
    }

    public static boolean isInteractable(BlockPos blockPos, LivingEntity livingEntity) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        double d = mc.player.getBlockInteractionRange();
        Vec3d vec3d = mc.player.getEyePos();
        boolean flag = vec3d.squaredDistanceTo(CombatUtil.clampToBox(vec3d, new Box(blockPos))) <= d * d;
        if (blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
            return flag;
        }

        Vec3d vec3d2 = Vec3d.ofCenter(blockPos);
        boolean flag2 = vec3d2.squaredDistanceTo(CombatUtil.clampToBox(vec3d2, livingEntity.getBoundingBox())) <= 25.0;
        return flag2 && flag && blockState.isReplaceable();
    }

    public static CrystalPlacementUtil.CrystalAttack findCrystalToAttack(LivingEntity livingEntity, CrystalPlacementUtil.DamageLimits damageLimits, BlockPos blockPos, IntSet intSet) {
        double d = mc.player.getEntityInteractionRange();
        float f = getEffectiveHealth();
        CrystalPlacementUtil.CrystalAttack crystalAttack = null;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity endCrystalEntity && endCrystalEntity.isAlive() && !intSet.contains(endCrystalEntity.getId())) {
                BlockHitResult blockHitResult = findTopFaceHit(endCrystalEntity.getBlockPos().down(), endCrystalEntity.getBoundingBox());
                Vec3d vec3d = blockHitResult != null ? blockHitResult.getPos() : findVisibleBoxPoint(endCrystalEntity.getBoundingBox(), d);
                if (vec3d != null) {
                    boolean flag = endCrystalEntity.getBlockPos().equals(blockPos);
                    CrystalPlacementUtil.ExplosionDamage explosionDamage = calculateExplosionDamage(endCrystalEntity.getEntityPos(), livingEntity);
                    boolean flag2 = explosionDamage.selfDamage() <= damageLimits.maxSelfDamage() && explosionDamage.selfDamage() < f - 0.5F;
                    if (flag && flag2) {
                        return new CrystalPlacementUtil.CrystalAttack(endCrystalEntity, vec3d, explosionDamage);
                    }

                    if (isSafeForSelf(explosionDamage, livingEntity, damageLimits) && (crystalAttack == null || explosionDamage.isBetterThan(crystalAttack.damage()))) {
                        crystalAttack = new CrystalPlacementUtil.CrystalAttack(endCrystalEntity, vec3d, explosionDamage);
                    }
                }
            }
        }

        return crystalAttack;
    }

    private static Vec3d findVisibleBoxPoint(Box box, double d) {
        Vec3d vec3d = mc.player.getEyePos();
        if (vec3d.squaredDistanceTo(CombatUtil.clampToBox(vec3d, box)) > d * d) {
            return null;
        }

        Box box2 = box.contract(0.05);
        Vec3d vec3d2 = CombatUtil.clampToBox(vec3d, box2);
        if (!RaycastUtil.canSee(vec3d, vec3d2)) {
            vec3d2 = null;
            double d2 = Double.MAX_VALUE;
            int i = 0;
            int i2 = 0;
            int i3 = 0;

            while (true) {
                label49: {
                    if (i3 <= 4) {
                        if (i != 0 && i != 4 && i2 != 0 && i2 != 4 && i3 != 0 && i3 != 4) {
                            break label49;
                        }
                    } else {
                        if (++i2 <= 4) {
                            i3 = 0;
                            continue;
                        }

                        if (++i > 4) {
                            if (vec3d2 == null) {
                                return null;
                            }
                            break;
                        }

                        i2 = 0;
                        i3 = 0;
                    }

                    Vec3d vec3d3 = new Vec3d(
                        box2.minX + (box2.maxX - box2.minX) * i / 4.0,
                        box2.minY + (box2.maxY - box2.minY) * i2 / 4.0,
                        box2.minZ + (box2.maxZ - box2.minZ) * i3 / 4.0
                    );
                    double d3 = vec3d.squaredDistanceTo(vec3d3);
                    if (!(d3 >= d2) && RaycastUtil.canSee(vec3d, vec3d3)) {
                        d2 = d3;
                        vec3d2 = vec3d3;
                    }
                }

                i3++;
            }
        }

        double d4 = box.raycast(vec3d, vec3d2).map(vec3d::distanceTo).orElse(0.0);
        return d4 <= d ? vec3d2 : null;
    }

    public static CrystalPlacementUtil.PlacementResult findBestCrystalPlacement(
        LivingEntity livingEntity, boolean flag, CrystalPlacementUtil.DamageLimits damageLimits, IntSet intSet, int i, boolean flag2
    ) {
        if (InventoryUtil.countItem(Items.END_CRYSTAL) == 0) {
            return null;
        }

        boolean flag3 = InventoryUtil.countItem(Items.OBSIDIAN) > 0;
        boolean flag4 = i > 0;
        Vec3d vec3d = getPredictedOffset(livingEntity, i);
        BlockPos blockPos = BlockPos.ofFloored(livingEntity.getEntityPos().add(vec3d));
        int i2 = flag4 ? 2 + getAirDepthBelow(blockPos) : 2;
        CrystalPlacementUtil.PlacementResult placementResult = null;
        CrystalPlacementUtil.PlacementResult placementResult2 = null;

        for (BlockPos blockPos2 : BlockPos.iterate(blockPos.add(-3, -i2, -3), blockPos.add(3, 1, 3))) {
            CrystalPlacementUtil.PlacementResult placementResult3 = evaluateCrystalPosition(blockPos2.toImmutable(), livingEntity, vec3d, flag3, flag, damageLimits, intSet);
            if (placementResult3 != null) {
                boolean flag5;
                CrystalPlacementUtil.PlacementResult placementResult4;
                if (placementResult3.supportHit() != null) {
                    flag5 = true;
                    placementResult4 = placementResult2;
                } else {
                    flag5 = false;
                    placementResult4 = placementResult;
                }

                CrystalPlacementUtil.PlacementResult placementResult5 = placementResult4;
                if (placementResult5 == null || placementResult3.damage().isBetterThan(placementResult5.damage())) {
                    if (flag5) {
                        placementResult2 = placementResult3;
                    } else {
                        placementResult = placementResult3;
                    }
                }
            }
        }

        CrystalPlacementUtil.PlacementResult placementResult6 = flag4
                && placementResult != null
                && placementResult2 != null
                && placementResult2.damage().targetDamage() > 1.1F * placementResult.damage().targetDamage()
            ? placementResult2
            : (placementResult != null ? placementResult : placementResult2);
        if (flag2 && !flag4 && flag3) {
            float f = placementResult6 == null ? 0.0F : 1.5F * placementResult6.damage().targetDamage();
            if (f > ExplosionDamageUtil.maxExplosionDamage(livingEntity)) {
                return placementResult6;
            }

            CrystalPlacementUtil.PlacementResult placementResult7 = findMinePlacement(livingEntity, flag, damageLimits, intSet, f);
            return placementResult7 != null ? placementResult7 : placementResult6;
        } else {
            return placementResult6;
        }
    }

    public static CrystalPlacementUtil.PlacementResult evaluateCrystalPlacement(
        BlockPos blockPos, LivingEntity livingEntity, boolean flag, CrystalPlacementUtil.DamageLimits damageLimits, IntSet intSet, int i
    ) {
        boolean flag2 = InventoryUtil.countItem(Items.OBSIDIAN) > 0;
        return evaluateCrystalPosition(blockPos, livingEntity, getPredictedOffset(livingEntity, i), flag2, flag, damageLimits, intSet);
    }

    private static CrystalPlacementUtil.PlacementResult evaluateCrystalPosition(
        BlockPos blockPos, LivingEntity livingEntity, Vec3d vec3d, boolean flag, boolean flag2, CrystalPlacementUtil.DamageLimits damageLimits, IntSet intSet
    ) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        boolean flag3;
        if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK)) {
            flag3 = false;
            if (!flag || !canPlaceGlowstone(blockPos)) {
                return null;
            }
        } else {
            flag3 = true;
        }

        if (!isCrystalSpaceFree(blockPos, intSet)) {
            return null;
        }

        Box box = getCrystalBox(blockPos);
        boolean flag4 = findVisibleBoxPoint(box, mc.player.getEntityInteractionRange()) != null;
        Box box2 = livingEntity.getBoundingBox().offset(vec3d);
        Vec3d vec3d2 = box2.getCenter();
        Vec3d vec3d3 = new Vec3d(vec3d2.x, box2.maxY, vec3d2.z);
        if (!flag4) {
            return null;
        }

        if (!canExplosionReach(blockPos, flag3 ? vec3d3 : vec3d2)) {
            return null;
        }

        BlockHitResult blockHitResult = findTopFaceHit(blockPos, box);
        if (blockHitResult == null) {
            blockHitResult = flag3 ? getInteractHit(blockPos, flag2) : getPlaceHit(blockPos, flag2);
        }

        if (blockHitResult == null) {
            return null;
        } else {
            CrystalPlacementUtil.ExplosionDamage explosionDamage = calculateCrystalDamage(blockPos, livingEntity, vec3d);
            if (!isSafeForSelf(explosionDamage, livingEntity, damageLimits)) {
                return null;
            } else {
                return flag3
                    ? new CrystalPlacementUtil.PlacementResult(blockPos, blockHitResult, null, null, explosionDamage)
                    : new CrystalPlacementUtil.PlacementResult(blockPos, null, blockHitResult, null, explosionDamage);
            }
        }
    }

    private static boolean isCrystalSpaceFree(BlockPos blockPos, IntSet intSet) {
        if (!mc.world.isAir(blockPos.up())) {
            return false;
        }

        Box box = new Box(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 3, blockPos.getZ() + 1);
        return mc.world.getOtherEntities(null, box, entity2 -> !intSet.contains(entity2.getId())).isEmpty();
    }

    private static boolean canExplosionReach(BlockPos blockPos, Vec3d vec3d) {
        Vec3d vec3d2 = Vec3d.ofCenter(blockPos).add(0.0, 0.5, 0.0);
        boolean flag = new Box(blockPos).raycast(vec3d2.add(0.0, 0.01, 0.0), vec3d).isPresent();
        return !flag && hasClearLine(vec3d2, vec3d);
    }

    private static CrystalPlacementUtil.ExplosionDamage calculateCrystalDamage(BlockPos blockPos, LivingEntity livingEntity, Vec3d vec3d) {
        return new CrystalPlacementUtil.ExplosionDamage(ExplosionDamageUtil.anchorDamage(blockPos, livingEntity, vec3d), ExplosionDamageUtil.anchorDamage(blockPos, mc.player));
    }

    private static CrystalPlacementUtil.PlacementResult findMinePlacement(
        LivingEntity livingEntity, boolean flag, CrystalPlacementUtil.DamageLimits damageLimits, IntSet intSet, float f
    ) {
        int i = findBestPickaxeSlot();
        if (i != -1 && mc.player.isOnGround()) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            Vec3d vec3d = livingEntity.getEntityPos();
            Vec3d vec3d2 = livingEntity.getBoundingBox().getCenter();
            BlockPos blockPos = livingEntity.getBlockPos();
            CrystalPlacementUtil.PlacementResult placementResult = null;
            double d = 0.0;

            for (BlockPos blockPos2 : BlockPos.iterate(blockPos.add(-2, -2, -2), blockPos.add(2, -1, 2))) {
                BlockPos blockPos3 = blockPos2.toImmutable();
                if (getBreakTicks(itemStack, blockPos3) <= 4 && isCrystalSpaceFree(blockPos3, intSet)) {
                    boolean flag2 = findVisibleBoxPoint(getCrystalBox(blockPos3), mc.player.getEntityInteractionRange()) != null;
                    if (flag2 && canExplosionReach(blockPos3, vec3d2)) {
                        CrystalPlacementUtil.ExplosionDamage explosionDamage = calculateCrystalDamage(blockPos3, livingEntity, Vec3d.ZERO);
                        double d2 = explosionDamage.targetDamage() * (1.0 + 0.2 * Math.max(0.0, getFacingFactor(livingEntity, vec3d, blockPos3)));
                        if (explosionDamage.targetDamage() >= f && d2 > d && isSafeForSelf(explosionDamage, livingEntity, damageLimits)) {
                            BlockHitResult blockHitResult = getInteractHit(blockPos3, flag);
                            if (blockHitResult != null && canMineBlock(blockPos3)) {
                                placementResult = new CrystalPlacementUtil.PlacementResult(blockPos3, null, null, blockHitResult, explosionDamage);
                                d = d2;
                            }
                        }
                    }
                }
            }

            return placementResult;
        } else {
            return null;
        }
    }

    private static boolean canMineBlock(BlockPos blockPos) {
        ExplosionDamageUtil.SimulatedBlockView simulatedBlockView = new ExplosionDamageUtil.SimulatedBlockView(mc.world, blockPos, Blocks.AIR.getDefaultState());
        Vec3d vec3d = mc.player.getEyePos();
        double d = mc.player.getBlockInteractionRange();

        for (Direction direction : Direction.values()) {
            BlockPos blockPos2 = blockPos.offset(direction);
            Direction direction2 = direction.getOpposite();
            if (direction != Direction.UP && isSolidSupportFace(blockPos2, direction2)) {
                Vec3d vec3d2 = Vec3d.ofCenter(blockPos2).add(Vec3d.of(direction2.getVector()).multiply(0.5));

                for (int i = -1; i <= 1; i++) {
                    for (int i2 = -1; i2 <= 1; i2++) {
                        Vec3d vec3d3 = switch (direction2.getAxis()) {
                            case X -> vec3d2.add(0.0, 0.35 * i, 0.35 * i2);
                            case Y -> vec3d2.add(0.35 * i, 0.0, 0.35 * i2);
                            case Z -> vec3d2.add(0.35 * i, 0.35 * i2, 0.0);
                        };
                        Vec3d vec3d4 = vec3d3.add(vec3d3.subtract(vec3d).normalize().multiply(0.05));
                        BlockHitResult blockHitResult = simulatedBlockView.raycast(new RaycastContext(vec3d, vec3d4, ShapeType.OUTLINE, FluidHandling.NONE, ShapeContext.absent()));
                        boolean flag = blockHitResult.getType() == Type.BLOCK
                            && blockHitResult.getBlockPos().equals(blockPos2)
                            && blockHitResult.getSide() == direction2
                            && vec3d.squaredDistanceTo(blockHitResult.getPos()) <= d * d;
                        if (flag) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static int findBestPickaxeSlot() {
        BlockState blockState = Blocks.STONE.getDefaultState();
        int i = -1;
        float f = 0.0F;
        int i2 = Math.min(36, mc.player.getInventory().size());

        for (int i3 = 0; i3 < i2; i3++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i3);
            if (itemStack.isIn(ItemTags.PICKAXES)) {
                float f2 = getMiningSpeed(itemStack, blockState);
                if (!(f2 <= f)) {
                    f = f2;
                    i = i3;
                }
            }
        }

        return i;
    }

    public static int getBreakTicks(ItemStack itemStack, BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        float f = blockState.getHardness(mc.world, blockPos);
        boolean flag;
        if (f > 0.0F && !blockState.isReplaceable() && !blockState.hasBlockEntity() && blockState.getFluidState().isEmpty()) {
            flag = !blockState.isOf(Blocks.OBSIDIAN);
        } else {
            flag = false;
        }

        boolean flag2 = mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE);
        if (flag && !flag2 && itemStack.isSuitableFor(blockState)) {
            float f2 = getMiningSpeed(itemStack, blockState) / f / 30.0F;
            return f2 >= 1.0F ? 0 : MathHelper.ceil(1.0F / f2);
        } else {
            return Integer.MAX_VALUE;
        }
    }

    private static float getMiningSpeed(ItemStack itemStack, BlockState blockState) {
        float f = itemStack.getMiningSpeedMultiplier(blockState);
        int i = EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), itemStack);
        if (f > 1.0F && i > 0) {
            f += i * i + 1;
        }

        if (StatusEffectUtil.hasHaste(mc.player)) {
            f *= 1.0F + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
        }

        float f2 = f * (float)mc.player.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
        if (mc.player.isSubmergedIn(FluidTags.WATER)) {
            f2 *= (float)mc.player.getAttributeValue(EntityAttributes.SUBMERGED_MINING_SPEED);
        }

        return mc.player.isOnGround() ? f2 : f2 / 5.0F;
    }

    private static Vec3d getPredictedOffset(LivingEntity livingEntity, int i) {
        return i > 0 ? predictMotion(livingEntity, i) : Vec3d.ZERO;
    }

    private static Vec3d predictMotion(LivingEntity livingEntity, int i) {
        Vec3d vec3d = new Vec3d(livingEntity.getX() - livingEntity.lastX, livingEntity.getY() - livingEntity.lastY, livingEntity.getZ() - livingEntity.lastZ);
        boolean flag = livingEntity.isGliding() || livingEntity.isClimbing() || livingEntity.isTouchingWater() || livingEntity.hasVehicle() || vec3d.lengthSquared() > 9.0;
        if (flag) {
            return Vec3d.ZERO;
        }

        List list = List.of();
        Box box = livingEntity.getBoundingBox();
        Vec3d vec3d2 = Vec3d.ZERO;

        for (int i2 = 0; i2 < i; i2++) {
            Vec3d vec3d3 = new Vec3d(vec3d.x, (vec3d.y - 0.08) * 0.98, vec3d.z);
            Vec3d vec3d4 = Entity.adjustMovementForCollisions(livingEntity, vec3d3, box.offset(vec3d2), mc.world, list);
            vec3d2 = vec3d2.add(vec3d4);
            boolean flag2;
            double d;
            if (vec3d3.y < 0.0 && vec3d4.y != vec3d3.y) {
                flag2 = true;
                d = 1.0;
            } else {
                flag2 = false;
                d = 0.91;
            }

            double d2 = d;
            vec3d = new Vec3d(vec3d4.x == vec3d3.x ? vec3d3.x * d2 : 0.0, flag2 ? 0.0 : vec3d3.y, vec3d4.z == vec3d3.z ? vec3d3.z * d2 : 0.0);
        }

        return vec3d2;
    }

    private static double getFacingFactor(LivingEntity livingEntity, Vec3d vec3d, BlockPos blockPos) {
        Vec3d vec3d2 = new Vec3d(blockPos.getX() + 0.5 - vec3d.x, 0.0, blockPos.getZ() + 0.5 - vec3d.z);
        return vec3d2.lengthSquared() < 0.09 ? 0.0 : -Vec3d.fromPolar(0.0F, livingEntity.getYaw()).dotProduct(vec3d2.normalize());
    }

    private static int getAirDepthBelow(BlockPos blockPos) {
        int i = 0;

        while (i < 5 && mc.world.getBlockState(blockPos.down(i + 1)).isReplaceable()) {
            i++;
        }

        return i;
    }

    public static CrystalPlacementUtil.PlacementResult findBestAnchorPlacement(
        LivingEntity livingEntity, BlockPos blockPos, Collection<BlockPos> blockPoses, boolean flag, boolean flag2, boolean flag3, CrystalPlacementUtil.DamageLimits damageLimits, int i
    ) {
        Vec3d vec3d = predictMotion(livingEntity, i);
        Vec3d vec3d2 = livingEntity.getEntityPos().add(vec3d);
        Box box = livingEntity.getBoundingBox().offset(vec3d);
        BlockPos blockPos2 = BlockPos.ofFloored(vec3d2);
        int i2 = 1 + getAirDepthBelow(blockPos2);
        double d = livingEntity.isOnGround() ? 0.3 : 0.0;
        float f = livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
        CrystalPlacementUtil.PlacementResult placementResult = null;
        int i3 = Integer.MAX_VALUE;
        boolean flag4 = false;
        double d2 = 0.0;

        for (BlockPos blockPos3 : BlockPos.iterate(blockPos2.add(-5, -i2, -5), blockPos2.add(5, 1, 5))) {
            BlockPos blockPos4 = blockPos3.toImmutable();
            if (!blockPos4.equals(blockPos) && !blockPoses.contains(blockPos4)) {
                boolean flag5 = mc.world.getBlockState(blockPos4).isOf(Blocks.RESPAWN_ANCHOR);
                boolean flag6 = Math.abs(blockPos4.getX() - blockPos2.getX()) > 3 || Math.abs(blockPos4.getZ() - blockPos2.getZ()) > 3;
                if (flag5 || !flag6) {
                    CrystalPlacementUtil.PlacementResult placementResult2 = evaluateAnchorPosition(blockPos4, livingEntity, vec3d, null, flag, flag2, flag3, damageLimits);
                    if (placementResult2 != null) {
                        Vec3d vec3d3 = Vec3d.ofCenter(blockPos4);
                        int i4 = (int)vec3d3.distanceTo(CombatUtil.clampToBox(vec3d3, box));
                        float f2 = placementResult2.damage().targetDamage();
                        boolean flag7 = f2 >= f;
                        double d3 = Math.max(0.0, getFacingFactor(livingEntity, vec3d2, blockPos4));
                        double d4 = f2 * (1.0 + d * d3) - placementResult2.damage().selfDamage();
                        boolean flag8;
                        if (placementResult == null) {
                            flag8 = true;
                        } else if (flag7 != flag4) {
                            flag8 = flag7;
                        } else if (i4 != i3) {
                            flag8 = i4 < i3;
                        } else {
                            flag8 = d4 > d2 || d4 == d2 && placementResult2.supportHit() == null && placementResult.supportHit() != null;
                        }

                        if (flag8) {
                            placementResult = placementResult2;
                            i3 = i4;
                            flag4 = flag7;
                            d2 = d4;
                        }
                    }
                }
            }
        }

        return placementResult;
    }

    public static CrystalPlacementUtil.PlacementResult evaluateAnchorPosition(
        BlockPos blockPos, LivingEntity livingEntity, Vec3d vec3d, BlockPos blockPos2, boolean flag, boolean flag2, boolean flag3, CrystalPlacementUtil.DamageLimits damageLimits
    ) {
        boolean flag4 = mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, blockPos);
        Vec3d vec3d2 = mc.player.getEyePos();
        if (vec3d2.distanceTo(CombatUtil.clampToBox(vec3d2, new Box(blockPos))) > mc.player.getBlockInteractionRange()) {
            return null;
        }

        if (!flag4) {
            BlockState blockState = mc.world.getBlockState(blockPos);
            boolean flag5 = blockState.isOf(Blocks.RESPAWN_ANCHOR);
            if (!flag5) {
                if (InventoryUtil.countItem(Items.RESPAWN_ANCHOR) == 0) {
                    return null;
                }

                if (!canPlaceAnchor(blockPos)) {
                    return null;
                }
            }

            boolean flag6 = !flag5 || blockState.get(RespawnAnchorBlock.CHARGES) == 0;
            if (flag6 && InventoryUtil.countItem(Items.GLOWSTONE) == 0) {
                return null;
            }

            if (!flag) {
                return null;
            }

            BlockHitResult blockHitResult;
            BlockHitResult blockHitResult2;
            label74: {
                blockHitResult = flag5 ? getInteractHit(blockPos, flag3) : null;
                boolean flag7;
                if (flag5) {
                    blockHitResult2 = null;
                    if (blockHitResult != null) {
                        break label74;
                    }

                    flag7 = true;
                } else if (isReplaceablePlant(blockState)) {
                    blockHitResult2 = getInteractHit(blockPos, flag3);
                    if (blockHitResult2 != null) {
                        break label74;
                    }

                    flag7 = true;
                } else {
                    blockHitResult2 = getPlaceHit(blockPos, flag3);
                    flag7 = blockHitResult2 == null;
                }

                boolean flag8 = flag7;
                if (flag8) {
                    return null;
                }
            }

            CrystalPlacementUtil.ExplosionDamage explosionDamage = new CrystalPlacementUtil.ExplosionDamage(
                ExplosionDamageUtil.crystalDamageAt(blockPos, livingEntity, vec3d), ExplosionDamageUtil.crystalDamageAt(blockPos, mc.player)
            );
            float f = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            boolean flag9 = blockPos.equals(blockPos2);
            boolean flag10 = flag2 || explosionDamage.selfDamage() < f - 0.5F && explosionDamage.selfDamage() <= damageLimits.maxSelfDamage();
            boolean flag11 = flag9 ? flag10 : explosionDamage.isSafe(f, livingEntity.getHealth() + livingEntity.getAbsorptionAmount(), damageLimits, flag2);
            return flag11 ? new CrystalPlacementUtil.PlacementResult(blockPos, blockHitResult, blockHitResult2, null, explosionDamage) : null;
        } else {
            return null;
        }
    }

    public static CrystalPlacementUtil.AnchorBlockPlacement findAnchorShieldPlacement(
        BlockPos blockPos, LivingEntity livingEntity, boolean flag, boolean flag2, CrystalPlacementUtil.DamageLimits damageLimits
    ) {
        if (InventoryUtil.countItem(Items.OBSIDIAN) == 0) {
            return null;
        }

        Vec3d vec3d = Vec3d.ofCenter(blockPos);
        Vec3d vec3d2 = mc.player.getEyePos().subtract(vec3d).normalize();
        float f = ExplosionDamageUtil.crystalDamageAt(blockPos, mc.player);
        float f2 = livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
        CrystalPlacementUtil.AnchorBlockPlacement anchorBlockPlacement = null;

        for (Direction direction : Direction.values()) {
            BlockPos blockPos2 = blockPos.offset(direction);
            Vec3d vec3d3 = Vec3d.ofCenter(blockPos2).subtract(vec3d).normalize();
            if (!(vec3d3.dotProduct(vec3d2) < 0.0) && canPlaceGlowstone(blockPos2) && canReachAnchorFace(blockPos, blockPos2, flag2)) {
                BlockHitResult blockHitResult = getPlaceHit(blockPos2, flag2);
                if (blockHitResult != null) {
                    CrystalPlacementUtil.ExplosionDamage explosionDamage = new CrystalPlacementUtil.ExplosionDamage(
                        ExplosionDamageUtil.crystalDamageWithReplacedBlocks(blockPos, blockPos2, livingEntity),
                        ExplosionDamageUtil.crystalDamageWithReplacedBlocks(blockPos, blockPos2, mc.player)
                    );
                    boolean flag3 = explosionDamage.selfDamage() < f;
                    if (flag3 && isSafeDamage(explosionDamage, livingEntity, damageLimits, flag) && (anchorBlockPlacement == null || explosionDamage.isBetterForAnchor(anchorBlockPlacement.damage(), f2))) {
                        anchorBlockPlacement = new CrystalPlacementUtil.AnchorBlockPlacement(blockPos2, blockHitResult, explosionDamage);
                    }
                }
            }
        }

        return anchorBlockPlacement;
    }

    public static int countShieldableSides(BlockPos blockPos, int i) {
        if (i > 0 && InventoryUtil.countItem(Items.OBSIDIAN) != 0) {
            Vec3d vec3d = mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).normalize();
            int i2 = 0;

            for (Direction direction : Direction.values()) {
                boolean flag = direction.getDoubleVector().dotProduct(vec3d) >= 0.0;
                if (flag && canPlaceGlowstone(blockPos.offset(direction))) {
                    if (++i2 == i) {
                        return i2;
                    }
                }
            }

            return i2;
        } else {
            return 0;
        }
    }

    public static CrystalPlacementUtil.BlockingPlacement findSelfProtectPlacement(
        LivingEntity livingEntity, Iterable<BlockPos> blockPoses, Collection<BlockPos> blockPoses2, BlockPos blockPos, IntSet intSet, Object2IntMap<BlockPos> object2IntMap, boolean flag
    ) {
        if (InventoryUtil.countItem(Items.OBSIDIAN) == 0) {
            return null;
        }

        Vec3d vec3d = null;
        BlockPos blockPos2 = null;
        float f = 0.0F;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity endCrystalEntity && endCrystalEntity.isAlive() && !endCrystalEntity.getBlockPos().equals(blockPos) && !intSet.contains(endCrystalEntity.getId())) {
                Vec3d vec3d2 = endCrystalEntity.getEntityPos();
                float f2 = ExplosionDamageUtil.crystalDamage(vec3d2, mc.player);
                if (!(f2 <= f)
                    && !(f2 <= ExplosionDamageUtil.crystalDamage(vec3d2, livingEntity))
                    && object2IntMap.getInt(endCrystalEntity.getBlockPos()) < 2
                    && isEnemyNear(vec3d2, 6.0)) {
                    vec3d = vec3d2;
                    blockPos2 = null;
                    f = f2;
                }
            }
        }

        for (BlockPos blockPos3 : blockPoses) {
            boolean flag2 = mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, blockPos3);
            if (!flag2 && !blockPoses2.contains(blockPos3)) {
                float f3 = ExplosionDamageUtil.crystalDamageAt(blockPos3, mc.player);
                if (!(f3 <= f)
                    && !(f3 <= ExplosionDamageUtil.crystalDamageAt(blockPos3, livingEntity))
                    && object2IntMap.getInt(blockPos3) < 2
                    && isEnemyNear(Vec3d.ofCenter(blockPos3), 7.0)) {
                    vec3d = Vec3d.ofCenter(blockPos3);
                    blockPos2 = blockPos3;
                    f = f3;
                }
            }
        }

        if (vec3d == null) {
            return null;
        } else {
            Vec3d vec3d3 = vec3d;
            BlockPos blockPos4 = blockPos2;
            ToDoubleFunction<BlockPos> toDoubleFunction = blockPos4 == null
                ? blockPos5 -> ExplosionDamageUtil.crystalDamageWithBlock(vec3d3, blockPos5, mc.player)
                : blockPos6 -> ExplosionDamageUtil.crystalDamageWithReplacedBlocks(blockPos4, blockPos6, mc.player);
            if (blockPos2 == null) {
                BlockPos blockPos7 = BlockPos.ofFloored(vec3d);
                return findBlockingPlacement(vec3d, blockPos7, f, toDoubleFunction, flag);
            } else {
                BlockPos blockPos8 = blockPos2;
                return findBlockingPlacement(vec3d, blockPos8, f, toDoubleFunction, flag);
            }
        }
    }

    private static boolean isEnemyNear(Vec3d vec3d, double d) {
        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if (isEnemyPlayer(playerEntity) && playerEntity.getEyePos().squaredDistanceTo(vec3d) <= d * d) {
                return true;
            }
        }

        return false;
    }

    private static CrystalPlacementUtil.BlockingPlacement findBlockingPlacement(
        Vec3d vec3d, BlockPos blockPos, float f, ToDoubleFunction<BlockPos> toDoubleFunction, boolean flag
    ) {
        Box box = mc.player.getBoundingBox();
        Vec3d vec3d2 = mc.player.getEyePos();
        Vec3d vec3d3 = box.getCenter();
        Vec3d vec3d4 = new Vec3d(vec3d3.x, box.minY + 0.1, vec3d3.z);
        double d = mc.player.getBlockInteractionRange();
        Box box2 = new Box(vec3d, vec3d2).union(box).expand(1.0);
        float f2 = f - 1.0F;
        CrystalPlacementUtil.BlockingPlacement blockingPlacement = null;

        for (BlockPos blockPos2 : BlockPos.iterate(BlockPos.ofFloored(box2.minX, box2.minY, box2.minZ), BlockPos.ofFloored(box2.maxX, box2.maxY, box2.maxZ))) {
            Box box3 = new Box(blockPos2);
            if (vec3d2.squaredDistanceTo(CombatUtil.clampToBox(vec3d2, box3)) <= d * d) {
                boolean flag2;
                if (!box3.raycast(vec3d, vec3d2).isPresent() && !box3.raycast(vec3d, vec3d3).isPresent()) {
                    flag2 = box3.raycast(vec3d, vec3d4).isPresent();
                } else {
                    flag2 = true;
                }

                BlockPos blockPos3 = blockPos2.toImmutable();
                if (flag2 && canPlaceGlowstone(blockPos3)) {
                    float f3 = (float)toDoubleFunction.applyAsDouble(blockPos3);
                    if (!(f3 > f2)) {
                        BlockHitResult blockHitResult = getPlaceHit(blockPos3, flag);
                        if (blockHitResult != null) {
                            f2 = f3;
                            blockingPlacement = new CrystalPlacementUtil.BlockingPlacement(blockPos, blockPos3, blockHitResult);
                        }
                    }
                }
            }
        }

        return blockingPlacement;
    }

    public static BlockHitResult findTopFaceHit(BlockPos blockPos, Box box) {
        Vec3d vec3d = mc.player.getEyePos();
        double d = blockPos.getY() + 1.0;
        if (vec3d.y <= d) {
            return null;
        }

        Vec3d vec3d2 = RaycastUtil.getDirection(mc.player.getYaw(), mc.player.getPitch());
        BlockHitResult blockHitResult = null;
        if (vec3d2.y < 0.0) {
            Vec3d vec3d3 = vec3d.add(vec3d2.multiply((d - vec3d.y) / vec3d2.y));
            boolean flag = vec3d3.x > blockPos.getX() && vec3d3.x < blockPos.getX() + 1.0 && vec3d3.z > blockPos.getZ() && vec3d3.z < blockPos.getZ() + 1.0;
            if (flag) {
                blockHitResult = raycastToPoint(blockPos, box, vec3d3);
            }

            if (blockHitResult != null) {
                return blockHitResult;
            }
        }

        double d2 = -2.0;

        for (int i = 0; i <= 2; i++) {
            for (int i2 = 0; i2 <= 2; i2++) {
                Vec3d vec3d4 = new Vec3d(blockPos.getX() + 0.1 + 0.4 * i, blockPos.getY() + 1.0, blockPos.getZ() + 0.1 + 0.4 * i2);
                double d3 = vec3d2.dotProduct(vec3d4.subtract(vec3d).normalize());
                if (!(d3 <= d2)) {
                    BlockHitResult blockHitResult2 = raycastToPoint(blockPos, box, vec3d4);
                    if (blockHitResult2 != null) {
                        blockHitResult = blockHitResult2;
                        d2 = d3;
                    }
                }
            }
        }

        return blockHitResult;
    }

    private static BlockHitResult raycastToPoint(BlockPos blockPos, Box box, Vec3d vec3d) {
        Vec3d vec3d2 = mc.player.getEyePos();
        if (getReachDistance(box, vec3d) > mc.player.getEntityInteractionRange()) {
            return null;
        } else {
            Vec2f vec2f = RotationUtil.getRotations(vec3d2, vec3d);
            HitResult hitResult = RaycastUtil.raycastBlocks(vec2f.x, vec2f.y, mc.player.getBlockInteractionRange(), false);
            if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == Type.BLOCK) {
                boolean flag = !mc.world.getBlockState(blockPos).isReplaceable();
                boolean flag2 = flag ? blockHitResult.getBlockPos().equals(blockPos) && blockHitResult.getSide() == Direction.UP : isPlacementHit(blockHitResult, blockPos);
                return flag2 ? blockHitResult : null;
            } else {
                return null;
            }
        }
    }

    private static double getReachDistance(Box box, Vec3d vec3d) {
        Vec3d vec3d2 = mc.player.getEyePos();
        return box.contains(vec3d2) ? 0.0 : box.raycast(vec3d2, vec3d).map(vec3d2::distanceTo).orElse(Double.MAX_VALUE);
    }

    private static Box getCrystalBox(BlockPos blockPos) {
        return EntityType.END_CRYSTAL.getDimensions().getBoxAt(Vec3d.ofBottomCenter(blockPos.up()));
    }

    public static Vec3d getAttackPoint(BlockPos blockPos, Vec3d vec3d) {
        Box box = getCrystalBox(blockPos);
        double d = mc.player.getEntityInteractionRange();
        if (getReachDistance(box, vec3d) <= d) {
            return vec3d;
        }

        Vec3d vec3d2 = findVisibleBoxPoint(box, d);
        return vec3d2 != null ? vec3d2 : vec3d;
    }

    public static BlockHitResult getInteractHit(BlockPos blockPos, boolean flag) {
        BlockHitResult blockHitResult = raycastCurrentRotation(blockPos);
        return blockHitResult != null && blockHitResult.getBlockPos().equals(blockPos) ? blockHitResult : AimPointUtil.getBlockHit(blockPos, (float)mc.player.getBlockInteractionRange(), flag);
    }

    private static BlockHitResult raycastCurrentRotation(BlockPos blockPos) {
        float f = mc.player.getYaw();
        float f2 = mc.player.getPitch();
        double d = mc.player.getBlockInteractionRange();
        Vec3d vec3d = mc.player.getEyePos();
        Vec3d vec3d2 = vec3d.add(RaycastUtil.getDirection(f, f2).multiply(d));
        if (new Box(blockPos).raycast(vec3d, vec3d2).isEmpty()) {
            return null;
        }

        HitResult hitResult = RaycastUtil.raycastBlocks(f, f2, d, false);
        return hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == Type.BLOCK ? blockHitResult : null;
    }

    private static boolean isPlacementHit(BlockHitResult blockHitResult, BlockPos blockPos) {
        BlockPos blockPos2 = blockHitResult.getBlockPos();
        return blockPos2.equals(blockPos)
            ? mc.world.getBlockState(blockPos).isReplaceable()
            : blockPos2.offset(blockHitResult.getSide()).equals(blockPos) && isSolidSupportFace(blockPos2, blockHitResult.getSide());
    }

    private static boolean isSolidSupportFace(BlockPos blockPos, Direction direction) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        return !blockState.isReplaceable()
            && !blockState.hasBlockEntity()
            && !blockState.isOf(Blocks.CRAFTING_TABLE)
            && !blockState.isOf(Blocks.RESPAWN_ANCHOR)
            && Block.isFaceFullSquare(blockState.getOutlineShape(mc.world, blockPos), direction);
    }

    public static boolean isReplaceablePlant(BlockState blockState) {
        return blockState.isOf(Blocks.KELP) || blockState.isOf(Blocks.KELP_PLANT);
    }

    private static BlockHitResult getPlaceHit(BlockPos blockPos, boolean flag) {
        BlockHitResult blockHitResult = raycastCurrentRotation(blockPos);
        if (blockHitResult != null && isPlacementHit(blockHitResult, blockPos)) {
            return blockHitResult;
        }

        float f = (float)mc.player.getBlockInteractionRange();
        boolean flag2 = !mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos).isEmpty();
        BlockHitResult blockHitResult2 = flag2 ? AimPointUtil.getBlockHit(blockPos, f, flag) : null;
        if (blockHitResult2 != null) {
            return blockHitResult2;
        }

        Vec3d vec3d = mc.player.getEyePos();
        BlockHitResult blockHitResult3 = null;
        double d = Double.MAX_VALUE;

        for (Direction direction : Direction.values()) {
            BlockPos blockPos2 = blockPos.offset(direction);
            Direction direction2 = direction.getOpposite();
            if (isSolidSupportFace(blockPos2, direction2)) {
                BlockHitResult blockHitResult4 = AimPointUtil.getBlockHit(blockPos2, direction2, f, flag);
                if (blockHitResult4 != null) {
                    double d2 = vec3d.squaredDistanceTo(blockHitResult4.getPos());
                    if (!(d2 >= d)) {
                        d = d2;
                        blockHitResult3 = blockHitResult4;
                    }
                }
            }
        }

        return blockHitResult3;
    }

    private static boolean canReachAnchorFace(BlockPos blockPos, BlockPos blockPos2, boolean flag) {
        float f = (float)mc.player.getBlockInteractionRange();
        return flag ? AimPointUtil.getBlockHit(blockPos, f, true) != null : AimPointUtil.getBlockHit(blockPos, f, new Box(blockPos2)) != null;
    }

    private static boolean canPlaceGlowstone(BlockPos blockPos) {
        return canPlaceItem(blockPos, Items.OBSIDIAN) && mc.world.getFluidState(blockPos).isEmpty();
    }

    private static boolean canPlaceAnchor(BlockPos blockPos) {
        boolean flag = isReplaceablePlant(mc.world.getBlockState(blockPos))
            ? isPlacementUnobstructed(blockPos, Items.RESPAWN_ANCHOR)
            : canPlaceItem(blockPos, Items.RESPAWN_ANCHOR);
        if (!flag) {
            return false;
        }

        FluidState fluidState = mc.world.getFluidState(blockPos);
        return fluidState.isEmpty() || fluidState.isIn(FluidTags.WATER);
    }

    private static boolean canPlaceItem(BlockPos blockPos, Item item) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        boolean flag;
        if (!blockState.isAir()) {
            if (blockState.isReplaceable()) {
                boolean flag2 = new ItemPlacementContext(
                        mc.world, mc.player, Hand.MAIN_HAND, new ItemStack(item), new BlockHitResult(Vec3d.ofCenter(blockPos), Direction.UP, blockPos, false)
                    )
                    .canReplaceExisting();
                return flag2 && isPlacementUnobstructed(blockPos, item);
            }

            flag = false;
        } else {
            flag = true;
        }

        boolean flag3 = flag;
        return flag3 && isPlacementUnobstructed(blockPos, item);
    }

    private static boolean isPlacementUnobstructed(BlockPos blockPos, Item item) {
        return mc.world.getWorldBorder().contains(blockPos)
            && !mc.world.isOutOfHeightLimit(blockPos)
            && mc.world.canPlace(Block.getBlockFromItem(item).getDefaultState(), blockPos, ShapeContext.absent());
    }

    private static boolean isSafeDamage(CrystalPlacementUtil.ExplosionDamage explosionDamage, LivingEntity livingEntity, CrystalPlacementUtil.DamageLimits damageLimits, boolean flag) {
        return explosionDamage.isSafe(mc.player.getHealth() + mc.player.getAbsorptionAmount(), livingEntity.getHealth() + livingEntity.getAbsorptionAmount(), damageLimits, flag);
    }

    private static boolean isSafeForSelf(CrystalPlacementUtil.ExplosionDamage explosionDamage, LivingEntity livingEntity, CrystalPlacementUtil.DamageLimits damageLimits) {
        return explosionDamage.isSafe(getEffectiveHealth(), livingEntity.getHealth() + livingEntity.getAbsorptionAmount(), damageLimits, false);
    }

    private static float getEffectiveHealth() {
        for (Hand hand : Hand.values()) {
            ItemStack itemStack = mc.player.getStackInHand(hand);
            boolean flag;
            if (itemStack.isOf(Items.TOTEM_OF_UNDYING)) {
                flag = !mc.player.getItemCooldownManager().isCoolingDown(itemStack);
            } else {
                flag = false;
            }

            if (flag) {
                return Float.MAX_VALUE;
            }
        }

        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    private static CrystalPlacementUtil.ExplosionDamage calculateExplosionDamage(Vec3d vec3d, LivingEntity livingEntity) {
        return new CrystalPlacementUtil.ExplosionDamage(getExplosionDamage(vec3d, livingEntity), getExplosionDamage(vec3d, mc.player));
    }

    public static float getExplosionDamage(Vec3d vec3d, LivingEntity livingEntity) {
        return livingEntity != null && mc.world != null ? ExplosionDamageUtil.crystalDamage(vec3d, livingEntity) : 0.0F;
    }

    private static boolean hasClearLine(Vec3d vec3d, Vec3d vec3d2) {
        HitResult hitResult = RaycastUtil.raycast(vec3d.add(0.0, 0.01, 0.0), vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, false);
        return hitResult.getType() != Type.BLOCK;
    }

    public static boolean hasTotemAvailable() {
        ItemStack itemStack = mc.player.getOffHandStack();
        boolean flag;
        if (itemStack.isOf(Items.TOTEM_OF_UNDYING)) {
            flag = !mc.player.getItemCooldownManager().isCoolingDown(itemStack);
        } else {
            flag = false;
        }

        if (flag) {
            return true;
        }

        int i = InventoryUtil.findHotbarIndex(Items.TOTEM_OF_UNDYING);
        if (i == -1) {
            return false;
        }

        ItemStack itemStack2 = mc.player.getInventory().getStack(i);
        return !mc.player.getItemCooldownManager().isCoolingDown(itemStack2);
    }

    public record AnchorBlockPlacement(BlockPos pos, BlockHitResult hit, CrystalPlacementUtil.ExplosionDamage damage) {
    }

    public record BlockingPlacement(BlockPos threat, BlockPos pos, BlockHitResult hit) {
    }

    public record CrystalAttack(EndCrystalEntity crystal, Vec3d aimPoint, CrystalPlacementUtil.ExplosionDamage damage) {
    }

    public record DamageLimits(float minTargetDamage, float maxSelfDamage) {
    }

    public record ExplosionDamage(float targetDamage, float selfDamage) {
        public boolean isSafe(float f, float f2, CrystalPlacementUtil.DamageLimits damageLimits, boolean flag) {
            boolean flag2 = this.targetDamage >= f2;
            boolean flag3 = flag || this.selfDamage < f - 0.5F;
            boolean flag4;
            if (!flag && !(this.selfDamage < this.targetDamage)) {
                flag4 = flag2;
            } else {
                flag4 = true;
            }

            boolean flag5 = flag || this.selfDamage <= damageLimits.maxSelfDamage();
            return flag3 && flag5 && (this.targetDamage >= damageLimits.minTargetDamage() || flag2) && flag4;
        }

        public boolean isBetterThan(CrystalPlacementUtil.ExplosionDamage explosionDamage) {
            if (explosionDamage == null) {
                return true;
            }

            int i = Float.compare(this.targetDamage, explosionDamage.targetDamage);
            return i > 0 || i == 0 && this.selfDamage < explosionDamage.selfDamage;
        }

        public boolean isBetterForAnchor(CrystalPlacementUtil.ExplosionDamage explosionDamage, float f) {
            if (explosionDamage == null) {
                return true;
            }

            boolean flag = this.targetDamage >= f;
            boolean flag2 = explosionDamage.targetDamage >= f;
            if (flag != flag2) {
                return flag;
            }

            int i = Float.compare(this.targetDamage - this.selfDamage, explosionDamage.targetDamage - explosionDamage.selfDamage);
            return i != 0 ? i > 0 : this.isBetterThan(explosionDamage);
        }
    }

    public record PlacePosition(BlockPos pos, BlockHitResult hit) {
    }

    public record PlacementResult(
        BlockPos pos, BlockHitResult hit, BlockHitResult supportHit, BlockHitResult mineHit, CrystalPlacementUtil.ExplosionDamage damage
    ) {
    }
}
