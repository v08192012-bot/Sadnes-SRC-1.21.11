package tech.sadnes.client.feature.impl.render.esp;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptEspRegistry;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;

public final class ScriptedEspElement extends ScriptEspElement {
    private final ScriptedEspElement.Definition definition;
    private NativeObject context;
    private List<ItemStack> items;

    public ScriptedEspElement(ScriptedEspElement.Definition definition2, EspTargetType espTargetType) {
        super(definition2.script(), definition2.name(), definition2.enabledByDefault(), espTargetType, definition2.replaces(), definition2.icon());
        this.definition = definition2;
        this.createLayoutSetting(definition2.anchor());
    }

    @Override
    public boolean usesItems() {
        return this.definition.hidesVanillaName();
    }

    @Override
    public void updateItems(List<ItemStack> itemStacks) {
        this.items = itemStacks;
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
        if (this.getScript().isActive()) {
            this.context = ScriptEspRegistry.createContext(entity, this.getTarget(), flag);
            if (entity instanceof ItemEntity itemEntity) {
                List list = !flag && this.items != null ? this.items : List.of(itemEntity.getStack());
                if (list.isEmpty()) {
                    return;
                }

                ScriptEspRegistry.putItems(this.context, list);
            }

            if ((this.definition.size() instanceof Function function ? this.getScript().call(function, this.context) : this.definition.size()) instanceof Scriptable scriptable
                )
             {
                this.width = ScriptEspRegistry.getFloatProperty(scriptable, "w", 0.0F);
                this.height = ScriptEspRegistry.getFloatProperty(scriptable, "h", 0.0F);
                if (this.width > 0.0F && this.height > 0.0F) {
                    this.measured = true;
                } else {
                    this.measured = false;
                }
            }
        }
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        if (this.measured && !(f8 <= 0.001F)) {
            this.renderScript(drawContext, f3, f4, f5, f6, f8);
        }
    }

    @Override
    public void renderOverlay(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured) {
            this.renderScript(drawContext, f, f2, f3 - f, f4 - f2, 1.0F);
        }
    }

    private void renderScript(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        ScriptEspRegistry.putBounds(this.context, this.getLayoutX(), this.getLayoutY(), this.width, this.height, f5);
        ScriptEspRegistry.putBox(this.context, f, f2, f3, f4);
        ScriptEspRegistry.invokeDraw(this.getScript(), drawContext, f5, this.definition.render(), this.context);
    }

    public record Definition(
        Script script,
        String name,
        boolean enabledByDefault,
        EspAnchor anchor,
        Icon icon,
        Object size,
        Function render,
        String replaces,
        boolean hidesVanillaName
    ) {
    }
}
