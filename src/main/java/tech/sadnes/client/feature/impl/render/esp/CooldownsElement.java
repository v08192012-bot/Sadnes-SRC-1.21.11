package tech.sadnes.client.feature.impl.render.esp;

import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import tech.sadnes.client.feature.component.ItemCooldownTrackerComponent;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.server.CooldownItem;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.color.Theme;

public final class CooldownsElement extends EspElement {
    private static final float ICON_SIZE = 8.0F;
    public final BooleanSetting showIcon = this.addSetting(new BooleanSetting("Show icon", true));
    private final TextSetting layout = this.createLayoutSetting(EspAnchor.LEFT);
    private final IconLabelStack labels = new IconLabelStack();
    private final StringBuilder textBuilder = new StringBuilder(8);

    public CooldownsElement(EspTargetType espTargetType) {
        super("Cooldowns", false, espTargetType);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public boolean isAttachable() {
        return true;
    }

    @Override
    public void measure(Entity entity, boolean flag) {
        this.measured = false;
        this.labels.begin(6.0F, this.showIcon.getValue() ? 8.0F : 0.0F);
        if (flag) {
            this.addCooldown(CooldownItem.ENCHANTED_GOLDEN_APPLE, 92400L);
            this.addCooldown(CooldownItem.GOLDEN_APPLE, 8400L);
        } else if (entity instanceof PlayerEntity playerEntity) {
            ItemCooldownTrackerComponent itemCooldownTrackerComponent = ItemCooldownTrackerComponent.getInstance();
            if (itemCooldownTrackerComponent == null) {
                return;
            }

            for (Entry entry : itemCooldownTrackerComponent.getCooldowns(playerEntity).entrySet()) {
                this.addCooldown((CooldownItem)entry.getKey(), (Long)entry.getValue());
            }
        }

        if (this.labels.finish()) {
            this.width = this.labels.getWidth();
            this.height = this.labels.getHeight();
            this.measured = true;
        }
    }

    @Override
    public void renderOverlay(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured) {
            IconLabelStack iconLabelStack = this.labels;
            float f6 = this.getLayoutX();
            float f7 = this.getLayoutY();
            if (this.pivotY > 0.5F) {
                iconLabelStack.render(drawContext, f6, f7, this.pivotX, true, 1.0F);
            } else {
                iconLabelStack.render(drawContext, f6, f7, this.pivotX, false, 1.0F);
            }
        }
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        if (this.measured && !(f8 <= 0.001F)) {
            IconLabelStack iconLabelStack = this.labels;
            float f9 = this.getLayoutX();
            float f10 = this.getLayoutY();
            if (this.pivotY > 0.5F) {
                iconLabelStack.render(drawContext, f9, f10, this.pivotX, true, f8);
            } else {
                iconLabelStack.render(drawContext, f9, f10, this.pivotX, false, f8);
            }
        }
    }

    private void addCooldown(CooldownItem cooldownItem, long l) {
        this.textBuilder.setLength(0);
        int i = (int)Math.ceil(l / 100.0);
        if (i >= 100) {
            this.textBuilder.append((i + 9) / 10);
        } else {
            this.textBuilder.append(i / 10).append('.').append(i % 10);
        }

        this.textBuilder.append('s');
        int i2 = Theme.getTextColor();
        if (this.showIcon.getValue()) {
            this.labels.addItemLine(this.textBuilder.toString(), i2, cooldownItem.getIcon());
        } else {
            this.labels.addLine(this.textBuilder.toString(), i2);
        }
    }
}
