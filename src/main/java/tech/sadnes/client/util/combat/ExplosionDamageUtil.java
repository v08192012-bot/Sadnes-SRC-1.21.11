package tech.sadnes.client.util.combat;

import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class ExplosionDamageUtil {
    private ExplosionDamageUtil() {
    }

    public static float crystalDamage(Vec3d vec3d, LivingEntity livingEntity) {
        return computeDamage(vec3d, livingEntity, Vec3d.ZERO, livingEntity.getEntityWorld(), 6.0F);
    }

    public static float anchorDamage(BlockPos blockPos, LivingEntity livingEntity) {
        return anchorDamage(blockPos, livingEntity, Vec3d.ZERO);
    }

    public static float anchorDamage(BlockPos blockPos, LivingEntity livingEntity, Vec3d vec3d) {
        ExplosionDamageUtil.SimulatedBlockView simulatedBlockView = new ExplosionDamageUtil.SimulatedBlockView(livingEntity.getEntityWorld(), blockPos, Blocks.OBSIDIAN.getDefaultState());
        return computeDamage(Vec3d.ofCenter(blockPos).add(0.0, 0.5, 0.0), livingEntity, vec3d, simulatedBlockView, 6.0F);
    }

    public static float crystalDamageWithBlock(Vec3d vec3d, BlockPos blockPos, LivingEntity livingEntity) {
        ExplosionDamageUtil.SimulatedBlockView simulatedBlockView = new ExplosionDamageUtil.SimulatedBlockView(livingEntity.getEntityWorld(), blockPos, Blocks.OBSIDIAN.getDefaultState());
        return computeDamage(vec3d, livingEntity, Vec3d.ZERO, simulatedBlockView, 6.0F);
    }

    public static float crystalDamageAt(BlockPos blockPos, LivingEntity livingEntity) {
        return crystalDamageAt(blockPos, livingEntity, Vec3d.ZERO);
    }

    public static float crystalDamageAt(BlockPos blockPos, LivingEntity livingEntity, Vec3d vec3d) {
        ExplosionDamageUtil.SimulatedBlockView simulatedBlockView = new ExplosionDamageUtil.SimulatedBlockView(livingEntity.getEntityWorld(), blockPos, Blocks.AIR.getDefaultState());
        return computeDamage(Vec3d.ofCenter(blockPos), livingEntity, vec3d, simulatedBlockView, 5.0F);
    }

    public static float crystalDamageWithReplacedBlocks(BlockPos blockPos, BlockPos blockPos2, LivingEntity livingEntity) {
        ExplosionDamageUtil.SimulatedBlockView simulatedBlockView = new ExplosionDamageUtil.SimulatedBlockView(livingEntity.getEntityWorld(), blockPos, Blocks.AIR.getDefaultState());
        ExplosionDamageUtil.SimulatedBlockView simulatedBlockView2 = new ExplosionDamageUtil.SimulatedBlockView(simulatedBlockView, blockPos2, Blocks.OBSIDIAN.getDefaultState());
        return computeDamage(Vec3d.ofCenter(blockPos), livingEntity, Vec3d.ZERO, simulatedBlockView2, 5.0F);
    }

    public static float maxExplosionDamage(LivingEntity livingEntity) {
        return applyReductions(livingEntity, 1.0, 1.0, 6.0F);
    }

    private static float computeDamage(Vec3d vec3d, LivingEntity livingEntity, Vec3d vec3d2, BlockView blockView, float f) {
        if (!(livingEntity instanceof PlayerEntity playerEntity && (playerEntity.isCreative() || playerEntity.isSpectator()))) {
            double d = livingEntity.getEntityPos().add(vec3d2).distanceTo(vec3d);
            if (d > f * 2.0) {
                return 0.0F;
            }

            Box box = livingEntity.getBoundingBox().offset(vec3d2);
            return applyReductions(livingEntity, d, exposure(blockView, vec3d, box, ShapeContext.of(livingEntity)), f);
        } else {
            return 0.0F;
        }
    }

    private static float applyReductions(LivingEntity livingEntity, double d, double d2, float f) {
        if (!(livingEntity instanceof PlayerEntity playerEntity && (playerEntity.isCreative() || playerEntity.isSpectator()))) {
            Difficulty difficulty = livingEntity instanceof PlayerEntity ? livingEntity.getEntityWorld().getDifficulty() : Difficulty.NORMAL;
            float f2 = rawDamage(d, d2, f, difficulty);
            float f3 = livingEntity.getArmor();
            float f4 = (float)livingEntity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
            float f5 = MathHelper.clamp(f3 - f2 / (2.0F + f4 / 4.0F), f3 * 0.2F, 20.0F);
            float f6 = f2 * (1.0F - f5 / 25.0F);
            StatusEffectInstance statusEffectInstance = livingEntity.getStatusEffect(StatusEffects.RESISTANCE);
            if (statusEffectInstance != null) {
                f6 *= Math.max(0.0F, 1.0F - (statusEffectInstance.getAmplifier() + 1) * 0.2F);
            }

            return Math.max(0.0F, f6 * (1.0F - getProtectionLevel(livingEntity) / 25.0F));
        } else {
            return 0.0F;
        }
    }

    static float rawDamage(double d, double d2, float f, Difficulty difficulty) {
        if (d > f * 2.0) {
            return 0.0F;
        }

        double d3 = (1.0 - d / (f * 2.0)) * d2;
        float f2 = (float)((d3 * d3 + d3) * 7.0 * f + 1.0);
        switch (difficulty) {
            case PEACEFUL:
                return 0.0F;
            case EASY:
                return Math.min(f2 / 2.0F + 1.0F, f2);
            case NORMAL:
                return f2;
            case HARD:
                return f2 * 1.5F;
            default:
                throw new MatchException(null, null);
        }
    }

    static double exposure(BlockView blockView, Vec3d vec3d, Box box, ShapeContext shapeContext) {
        double d = 1.0 / (box.getLengthX() * 2.0 + 1.0);
        double d2 = 1.0 / (box.getLengthY() * 2.0 + 1.0);
        double d3 = 1.0 / (box.getLengthZ() * 2.0 + 1.0);
        double d4 = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double d5 = (1.0 - Math.floor(1.0 / d3) * d3) / 2.0;
        int i = 0;
        int i2 = 0;

        for (double d6 = 0.0; d6 <= 1.0; d6 += d) {
            for (double d7 = 0.0; d7 <= 1.0; d7 += d2) {
                for (double d8 = 0.0; d8 <= 1.0; d8 += d3) {
                    Vec3d vec3d2 = new Vec3d(
                        MathHelper.lerp(d6, box.minX, box.maxX) + d4,
                        MathHelper.lerp(d7, box.minY, box.maxY),
                        MathHelper.lerp(d8, box.minZ, box.maxZ) + d5
                    );
                    BlockHitResult blockHitResult = blockView.raycast(new RaycastContext(vec3d2, vec3d, ShapeType.COLLIDER, FluidHandling.NONE, shapeContext));
                    if (blockHitResult.getType() == Type.MISS) {
                        i2++;
                    }

                    i++;
                }
            }
        }

        return i == 0 ? 0.0 : (double)i2 / i;
    }

    private static int getProtectionLevel(LivingEntity livingEntity) {
        Registry registry = livingEntity.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Optional optional = registry.getOptional(Enchantments.PROTECTION);
        Optional optional2 = registry.getOptional(Enchantments.BLAST_PROTECTION);
        int i = 0;

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (equipmentSlot.getType() == net.minecraft.entity.EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack itemStack = livingEntity.getEquippedStack(equipmentSlot);
                if (!itemStack.isEmpty()) {
                    if (optional.isPresent()) {
                        i += EnchantmentHelper.getLevel((RegistryEntry<Enchantment>)optional.get(), itemStack);
                    }

                    if (optional2.isPresent()) {
                        i += EnchantmentHelper.getLevel((RegistryEntry<Enchantment>)optional2.get(), itemStack) * 2;
                    }
                }
            }
        }

        return MathHelper.clamp(i, 0, 20);
    }

    record SimulatedBlockView(BlockView world, BlockPos position, BlockState replacement) implements BlockView {
        @Override
        public BlockState getBlockState(BlockPos blockPos) {
            return this.position.equals(blockPos) ? this.replacement : this.world.getBlockState(blockPos);
        }

        @Override
        public FluidState getFluidState(BlockPos blockPos) {
            return this.getBlockState(blockPos).getFluidState();
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos blockPos) {
            return this.position.equals(blockPos) ? null : this.world.getBlockEntity(blockPos);
        }

        @Override
        public int getHeight() {
            return this.world.getHeight();
        }

        @Override
        public int getBottomY() {
            return this.world.getBottomY();
        }
    }
}
