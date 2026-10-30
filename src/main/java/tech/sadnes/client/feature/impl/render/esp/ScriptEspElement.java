package tech.sadnes.client.feature.impl.render.esp;

import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.setting.Setting;

public abstract class ScriptEspElement extends EspElement {
    private final Script script;
    private final String alias;
    private final Icon icon;

    protected ScriptEspElement(Script script2, String string, boolean flag, EspTargetType espTargetType, String string2, Icon icon2) {
        super(string, flag, espTargetType);
        this.script = script2;
        this.alias = string2;
        this.icon = icon2;
    }

    public Script getScript() {
        return this.script;
    }

    public void registerSetting(Setting<?> setting) {
        this.settings.add(setting);
    }

    @Override
    public boolean isActive() {
        return this.script.isActive() && super.isActive();
    }

    @Override
    public Icon getIcon() {
        return this.icon;
    }

    @Override
    public String getAlias() {
        return this.alias;
    }
}
