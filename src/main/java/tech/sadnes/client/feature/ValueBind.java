package tech.sadnes.client.feature;


public class ValueBind<T> extends Bind {
    private T value;

    public ValueBind(int i, BindMode bindMode, T value2) {
        this(i, bindMode, (T)value2, true);
    }

    public ValueBind(int i, BindMode bindMode, T value2, boolean flag) {
        this.key = i;
        this.mode = bindMode != null ? bindMode : BindMode.HOLD;
        this.value = (T)value2;
        this.visible = flag;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value2) {
        this.value = (T)value2;
    }
}
