package tech.sadnes.client.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.ValueBind;

public abstract class ValueSetting<T> extends Setting<T> {
    private final List<ValueBind<T>> binds = new ArrayList<>();
    private final List<ValueBind<T>> bindsView = Collections.unmodifiableList(this.binds);
    private final Map<ValueBind<T>, Long> activeBinds = new IdentityHashMap<>();
    private T defaultValue;
    private long bindCounter;
    private ValueBind<T> lastToggleBind;
    public ValueBind<T> editingBind;

    protected ValueSetting(String string, T value) {
        super(string, (T)value);
        this.defaultValue = (T)value;
    }

    public ValueSetting<T> setValue(T value) {
        this.defaultValue = this.sanitizeValue((T)value);
        this.updateValueFromBinds();
        return this;
    }

    public ValueSetting<T> setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public ValueSetting<T> setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public ValueSetting<T> setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public ValueSetting<T> mouseDragged(int i, BindMode bindMode, T value) {
        ValueBind valueBind = new ValueBind(i, bindMode, this.sanitizeValue((T)value));
        if (!this.hasDuplicateBind(valueBind.getKey(), valueBind.getMode(), (T)valueBind.getValue(), valueBind.getModifiers())) {
            this.binds.add(valueBind);
        }

        return this;
    }

    public ValueSetting<T> addBind(ValueBind<T> valueBind) {
        if (valueBind != null) {
            ValueBind valueBind2 = new ValueBind(valueBind.getKey(), valueBind.getMode(), this.sanitizeValue((T)valueBind.getValue()), valueBind.isVisible());
            valueBind2.setModifiers(valueBind.getModifiers());
            if (!this.hasDuplicateBind(valueBind2.getKey(), valueBind2.getMode(), (T)valueBind2.getValue(), valueBind2.getModifiers())) {
                this.binds.add(valueBind2);
            }
        }

        return this;
    }

    public ValueBind<T> createBind(T value) {
        ValueBind valueBind = new ValueBind(-1, BindMode.TOGGLE, this.sanitizeValue((T)value), true);
        this.binds.add(valueBind);
        return valueBind;
    }

    public ValueSetting<T> removeBind(ValueBind<T> valueBind) {
        if (valueBind == null) {
            return this;
        }

        this.binds.remove(valueBind);
        this.activeBinds.remove(valueBind);
        if (this.lastToggleBind == valueBind) {
            this.lastToggleBind = this.findLatestToggleBind();
        }

        this.updateValueFromBinds();
        return this;
    }

    public ValueSetting<T> clearBinds() {
        this.binds.clear();
        this.activeBinds.clear();
        this.lastToggleBind = null;
        this.updateValueFromBinds();
        return this;
    }

    public ValueSetting<T> setBindValue(ValueBind<T> valueBind, T value) {
        if (valueBind == null) {
            return this;
        }

        valueBind.setValue(this.sanitizeValue((T)value));
        this.updateValueFromBinds();
        return this;
    }

    public ValueSetting<T> setBinds(Collection<ValueBind<T>> valueBinds) {
        this.clearBinds();
        if (valueBinds == null) {
            return this;
        }

        for (ValueBind valueBind : valueBinds) {
            this.addBind(valueBind);
        }

        this.updateValueFromBinds();
        return this;
    }

    public List<ValueBind<T>> getBinds() {
        return this.bindsView;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isBindActive(ValueBind<?> valueBind) {
        if (valueBind != null && this.activeBinds.containsKey(valueBind)) {
            return valueBind.getMode() != BindMode.TOGGLE ? true : valueBind == this.lastToggleBind && !this.hasHeldBind();
        } else {
            return false;
        }
    }

    public void resetActiveBinds() {
        this.activeBinds.clear();
        this.lastToggleBind = null;
        this.updateValueFromBinds();
    }

    public boolean isToggleBindActive(ValueBind<?> valueBind) {
        return valueBind != null && valueBind.getMode() == BindMode.TOGGLE && this.activeBinds.containsKey(valueBind);
    }

    public void activateBind(int i) {
        if (i >= 0 && i < this.binds.size()) {
            ValueBind valueBind = this.binds.get(i);
            if (valueBind.getMode() == BindMode.TOGGLE && !this.activeBinds.containsKey(valueBind)) {
                this.activeBinds.put(valueBind, ++this.bindCounter);
                this.lastToggleBind = valueBind;
                this.updateValueFromBinds();
            }
        }
    }

    public String formatBindValue(ValueBind<T> valueBind) {
        if (valueBind != null) {
            return String.valueOf(valueBind.getValue());
        }

        Object object = this.defaultValue;
        return String.valueOf(object);
    }

    public float getBindRowHeight() {
        return 18.0F;
    }

    public boolean isBindDropdownOpen() {
        return false;
    }

    public void renderBindDropdown(DrawContext drawContext, int i, int i2, float f) {
        this.render(drawContext, i, i2, f);
    }

    @Override
    protected void onKey(int i, boolean flag) {
        boolean flag2 = false;

        for (ValueBind valueBind : this.binds) {
            if (valueBind.matches(i)) {
                if (valueBind.getMode() == BindMode.TOGGLE) {
                    if (flag) {
                        flag2 |= this.toggleBind(valueBind);
                    }
                } else if (flag) {
                    flag2 |= this.pressBind(valueBind);
                } else {
                    flag2 |= this.releaseBind(valueBind);
                }
            }
        }

        if (!this.activeBinds.isEmpty()) {
            for (ValueBind valueBind2 : this.binds) {
                if (valueBind2.getMode() == BindMode.HOLD && this.activeBinds.containsKey(valueBind2) && !valueBind2.isHeld()) {
                    flag2 |= this.releaseBind(valueBind2);
                }
            }
        }

        if (flag2) {
            this.updateValueFromBinds();
        }
    }

    @Override
    protected boolean hasBinds() {
        return !this.binds.isEmpty();
    }

    @Override
    protected boolean hasBind(int i) {
        for (ValueBind valueBind : this.binds) {
            if (valueBind.getKey() == i) {
                return true;
            }
        }

        return false;
    }

    protected T sanitizeValue(T value) {
        return (T)value;
    }

    private boolean toggleBind(ValueBind<T> valueBind) {
        if (this.activeBinds.containsKey(valueBind)) {
            this.activeBinds.remove(valueBind);
            if (this.lastToggleBind == valueBind) {
                this.lastToggleBind = this.findLatestToggleBind();
            }

            return true;
        } else {
            this.activeBinds.put(valueBind, ++this.bindCounter);
            this.lastToggleBind = valueBind;
            return true;
        }
    }

    private boolean pressBind(ValueBind<T> valueBind) {
        if (this.activeBinds.containsKey(valueBind)) {
            return false;
        }

        this.activeBinds.put(valueBind, ++this.bindCounter);
        return true;
    }

    private boolean releaseBind(ValueBind<T> valueBind) {
        return this.activeBinds.remove(valueBind) != null;
    }

    private boolean hasHeldBind() {
        for (ValueBind valueBind : this.activeBinds.keySet()) {
            if (valueBind.getMode() == BindMode.HOLD) {
                return true;
            }
        }

        return false;
    }

    private ValueBind<T> findLatestToggleBind() {
        ValueBind valueBind = null;
        long l = Long.MIN_VALUE;

        for (Entry entry : this.activeBinds.entrySet()) {
            if (((ValueBind)entry.getKey()).getMode() == BindMode.TOGGLE && (Long)entry.getValue() > l) {
                l = (Long)entry.getValue();
                valueBind = (ValueBind)entry.getKey();
            }
        }

        return valueBind;
    }

    private void updateValueFromBinds() {
        Object object = this.defaultValue;
        long l = Long.MIN_VALUE;

        for (Entry entry : this.activeBinds.entrySet()) {
            if ((Long)entry.getValue() > l) {
                l = (Long)entry.getValue();
                object = ((ValueBind)entry.getKey()).getValue();
            }
        }

        this.setValueInternal(this.sanitizeValue((T)object));
    }

    private boolean hasDuplicateBind(int i, BindMode bindMode, T value, int[] ints) {
        if (i == -1) {
            return false;
        }

        for (ValueBind valueBind : this.binds) {
            if (valueBind.getKey() == i && valueBind.getMode() == bindMode && Arrays.equals(valueBind.getModifiers(), ints)) {
                if (value == null) {
                    if (valueBind.getValue() == null) {
                        return true;
                    }
                } else if (value.equals(valueBind.getValue())) {
                    return true;
                }
            }
        }

        return false;
    }
}
