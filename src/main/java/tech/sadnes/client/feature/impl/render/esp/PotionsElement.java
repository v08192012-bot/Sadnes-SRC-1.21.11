package tech.sadnes.client.feature.impl.render.esp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Atlases;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.color.ColorUtil;

public final class PotionsElement extends EspElement {
    private static final float ICON_SIZE = 8.0F;
    private static final float MAX_SATURATION = 0.8F;
    private static final float MIN_BRIGHTNESS = 0.8F;
    public final BooleanSetting showDuration = this.addSetting(new BooleanSetting("Show duration", true));
    public final BooleanSetting showIcon = this.addSetting(new BooleanSetting("Show icon", false));
    private final TextSetting layout = this.createLayoutSetting(EspAnchor.RIGHT);
    private final IconLabelStack lineList = new IconLabelStack();
    private final StringBuilder textBuilder = new StringBuilder(24);

    public PotionsElement(EspTargetType espTargetType) {
        super("Potions", false, espTargetType);
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
        this.lineList.begin(6.0F, this.showIcon.getValue() ? 8.0F : 0.0F);
        if (flag) {
            this.addEffectLine(StatusEffects.STRENGTH, 1, 1660);
            this.addEffectLine(StatusEffects.SPEED, 0, 900);
        } else if (entity instanceof LivingEntity livingEntity) {
            for (StatusEffectInstance statusEffectInstance : livingEntity.getStatusEffects()) {
                this.addEffectLine(statusEffectInstance.getEffectType(), statusEffectInstance.getAmplifier(), statusEffectInstance.isInfinite() ? -1 : statusEffectInstance.getDuration());
            }
        }

        if (this.lineList.finish()) {
            this.width = this.lineList.getWidth();
            this.height = this.lineList.getHeight();
            this.measured = true;
        }
    }

    @Override
    public void renderOverlay(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured) {
            IconLabelStack iconLabelStack = this.lineList;
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
            IconLabelStack iconLabelStack = this.lineList;
            float f9 = this.getLayoutX();
            float f10 = this.getLayoutY();
            if (this.pivotY > 0.5F) {
                iconLabelStack.render(drawContext, f9, f10, this.pivotX, true, f8);
            } else {
                iconLabelStack.render(drawContext, f9, f10, this.pivotX, false, f8);
            }
        }
    }

    private void addEffectLine(RegistryEntry<StatusEffect> registryEntry, int i, int i2) {
        this.textBuilder.setLength(0);
        this.textBuilder.append(((StatusEffect)registryEntry.value()).getName().getString());
        if (i > 0) {
            this.textBuilder.append(' ').append(i + 1);
        }

        if (this.showDuration.getValue()) {
            this.textBuilder.append(' ');
            appendDuration(this.textBuilder, i2);
        }

        int i3 = getEffectColor(registryEntry);
        if (this.showIcon.getValue()) {
            this.lineList.addSpriteLine(this.textBuilder.toString(), i3, getEffectSprite(registryEntry));
        } else {
            this.lineList.addLine(this.textBuilder.toString(), i3);
        }
    }

    private static int getEffectColor(RegistryEntry<StatusEffect> registryEntry) {
        float[] floats = ColorUtil.toHsb(((StatusEffect)registryEntry.value()).getColor());
        return ColorUtil.fromHsb(floats[0], Math.min(floats[1], 0.8F), Math.max(floats[2], 0.8F));
    }

    private static Sprite getEffectSprite(RegistryEntry<StatusEffect> registryEntry) {
        SpriteAtlasTexture spriteAtlasTexture = MinecraftClient.getInstance().getAtlasManager().getAtlasTexture(Atlases.GUI);
        return spriteAtlasTexture == null ? null : spriteAtlasTexture.getSprite(InGameHud.getEffectTexture(registryEntry));
    }

    private static void appendDuration(StringBuilder builder, int i) {
        if (i < 0) {
            builder.append("INF");
        } else {
            int i2 = i / 20;
            int i3 = i2 / 60;
            int i4 = i2 % 60;
            if (i3 < 10) {
                builder.append('0');
            }

            builder.append(i3).append(':');
            if (i4 < 10) {
                builder.append('0');
            }

            builder.append(i4);
        }
    }
}
