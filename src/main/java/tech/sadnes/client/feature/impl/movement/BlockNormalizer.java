package tech.sadnes.client.feature.impl.movement;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.BlockCollisionEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "BlockNormalizer",
    description = "feature.impl.movement.blocknormalizer",
    displayName = "feature.impl.movement.blocknormalizer",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_TERRAIN
)
public final class BlockNormalizer extends Feature {
    public final BooleanSetting normalize = new BooleanSetting("blocknormalizer.normalize", true);
    public final MultiSetting blocks = new MultiSetting(
            "blocknormalizer.blocks",
            BooleanSetting.of("ice", true),
            BooleanSetting.of("slime", true),
            BooleanSetting.of("soul_sand", true),
            BooleanSetting.of("honey", true)
        )
        .setVisibleWhen(this.normalize::getValue);
    public final BooleanSetting solidify = new BooleanSetting("blocknormalizer.solidify", true);
    public final MultiSetting solidBlocks = new MultiSetting(
            "blocknormalizer.solidblocks", BooleanSetting.of("snow_layer", true), BooleanSetting.of("powder_snow", true)
        )
        .setVisibleWhen(this.solidify::getValue);

    public static BlockNormalizer getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(BlockNormalizer.class);
    }

    public boolean shouldNormalize(String string) {
        boolean flag = this.isEnabled();
        BooleanSetting booleanSetting = this.normalize;
        if (flag) {
            boolean flag2 = booleanSetting.getValue();
            MultiSetting multiSetting = this.blocks;
            return flag2 ? multiSetting.has(string) : false;
        } else {
            return false;
        }
    }

    @EventHandler
    public void onBlockCollision(BlockCollisionEvent blockCollisionEvent) {
        if (this.solidify.getValue()) {
            BlockState blockState = blockCollisionEvent.getState();
            if (blockState.isOf(Blocks.SNOW) && this.solidBlocks.has("snow_layer")) {
                blockCollisionEvent.setState(Blocks.WHITE_CARPET.getDefaultState());
            } else {
                if (blockState.isOf(Blocks.POWDER_SNOW) && this.solidBlocks.has("powder_snow")) {
                    blockCollisionEvent.setState(Blocks.SNOW_BLOCK.getDefaultState());
                }
            }
        }
    }
}
