package tech.sadnes.client.feature.impl.hud;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.feature.Beta;
import tech.sadnes.client.feature.BetaScope;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.util.color.ColorUtil;

@Beta(scope = BetaScope.ONLY_ADMIN)
@FeatureInfo(
    name = "Luna",
    description = "feature.impl.hud.luna",
    displayName = "feature.impl.hud.luna",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_INFORMATION
)
public class Luna extends Feature {
    private static final AnimatedIcon[] STICKERS = new AnimatedIcon[]{
        AnimatedIcon.LUNA, AnimatedIcon.LUNA_2, AnimatedIcon.LUNA_3, AnimatedIcon.LUNA_4, AnimatedIcon.LUNA_5
    };
    private final ModeSetting sticker = (ModeSetting)((Setting<?>)new ModeSetting("luna.sticker", new String[]{"1", "2", "3", "4", "5"})).setDisplayName("Sticker");
    private final NumberSetting size = (NumberSetting)new NumberSetting("luna.size", 60.0F, 20.0F, 200.0F, 1.0F).setDisplayName("Size");
    private final NumberSetting radius = (NumberSetting)new NumberSetting("luna.radius", 6.0F, 0.0F, 40.0F, 0.5F).setDisplayName("Radius");
    private final DragSetting draggable = new DragSetting(this.getInfo().name(), 20.0F, 40.0F).setSize(60.0F, 60.0F).onInput(this);

    public Luna() {
        this.draggable.onInput((drawContext, i, i2, f) -> this.renderSticker(drawContext));
        DragComponent.register(this.draggable);
    }

    private void renderSticker(DrawContext drawContext) {
        this.draggable.setDraggable(this::isEnabled);
        float f = this.size.getValue();
        this.draggable.setSize(f, f);
        if (this.isEnabled()) {
            new RenderUtil(drawContext, this.draggable.position2.x, this.draggable.position2.y, f, f)
                .spriteBlended(this.getSelectedSticker(), 0.0F, 0.0F, f, f, ColorUtil.WHITE, Radius.all(this.radius.getValue()));
        }
    }

    private AnimatedIcon getSelectedSticker() {
        int i = 0;

        try {
            i = Integer.parseInt(this.sticker.getValue()) - 1;
        } catch (NumberFormatException numberFormatException) {
        }

        if (i < 0) {
            i = 0;
        }

        if (i >= STICKERS.length) {
            i = STICKERS.length - 1;
        }

        return STICKERS[i];
    }
}
