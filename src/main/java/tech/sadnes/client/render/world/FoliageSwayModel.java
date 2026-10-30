package tech.sadnes.client.render.world;

import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBakeBlock.Context;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.minecraft.block.AzaleaBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerbedBlock;
import net.minecraft.block.LeafLitterBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SeaPickleBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public final class FoliageSwayModel extends WrapperBlockStateModel {
    private final FoliageSwayModel.FoliageType foliageType;
    private final QuadTransform swayTransform = this::applySway;

    private FoliageSwayModel(BlockStateModel blockStateModel, FoliageSwayModel.FoliageType foliageType2) {
        super(blockStateModel);
        this.foliageType = foliageType2;
    }

    public static BlockStateModel wrap(BlockStateModel blockStateModel, Context context) {
        FoliageSwayModel.FoliageType foliageType2 = FoliageSwayModel.FoliageType.fromState(context.state());
        return foliageType2 == FoliageSwayModel.FoliageType.NONE ? blockStateModel : new FoliageSwayModel(blockStateModel, foliageType2);
    }

    @Override
    public void emitQuads(QuadEmitter quadEmitter, BlockRenderView blockRenderView, BlockPos blockPos, BlockState blockState, Random random, Predicate<Direction> predicate) {
        quadEmitter.pushTransform(this.swayTransform);

        try {
            this.wrapped.emitQuads(quadEmitter, blockRenderView, blockPos, blockState, random, predicate);
        } finally {
            quadEmitter.popTransform();
        }
    }

    private boolean applySway(MutableQuadView mutableQuadView) {
        if (this.foliageType.isPlant() && Math.abs(mutableQuadView.faceNormal().y()) > 0.9F) {
            return true;
        }

        for (int i = 0; i < 4; i++) {
            int i2 = mutableQuadView.color(i);
            if (i2 >>> 24 == 255) {
                mutableQuadView.color(i, i2 & 16777215 | 255 - this.foliageType.getSwayWeight(mutableQuadView.y(i)) << 24);
            }
        }

        return true;
    }

    enum FoliageType {
        NONE,
        PLANT,
        LOWER_HALF,
        UPPER_HALF,
        LEAVES,
        VINE;

        int getSwayWeight(float f) {
            float f2 = MathHelper.clamp(f, 0.0F, 1.0F);
            switch (this) {
                case NONE:
                    return 0;
                case PLANT:
                    return 1 + Math.round(f2 * 10.0F);
                case LOWER_HALF:
                    return 1 + Math.round(f2 * 5.0F);
                case UPPER_HALF:
                    return 6 + Math.round(f2 * 5.0F);
                case LEAVES:
                    return 13;
                case VINE:
                    return 14;
                default:
                    throw new MatchException(null, null);
            }
        }

        boolean isPlant() {
            return this == PLANT || this == LOWER_HALF || this == UPPER_HALF;
        }

        static FoliageSwayModel.FoliageType fromState(BlockState blockState) {
            if (!blockState.getFluidState().isEmpty()) {
                return NONE;
            }

            Block block = blockState.getBlock();
            if (block instanceof LeavesBlock) {
                return LEAVES;
            }

            if (block instanceof VineBlock) {
                return VINE;
            }

            if (block instanceof TallPlantBlock) {
                return blockState.get(TallPlantBlock.HALF) == DoubleBlockHalf.LOWER ? LOWER_HALF : UPPER_HALF;
            }

            boolean flag;
            if (!(block instanceof LilyPadBlock)
                && !(block instanceof FlowerbedBlock)
                && !(block instanceof LeafLitterBlock)
                && !(block instanceof MushroomPlantBlock)
                && !(block instanceof AzaleaBlock)) {
                flag = block instanceof SeaPickleBlock;
            } else {
                flag = true;
            }

            return block instanceof PlantBlock && !flag ? PLANT : NONE;
        }
    }
}
