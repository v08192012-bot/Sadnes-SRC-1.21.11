package tech.sadnes.client.feature;

import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.util.Key;

public class Bind {
    public static final int NO_KEY = -1;
    public static final int[] NO_MODIFIERS = new int[0];
    public int key;
    public int[] modifiers = NO_MODIFIERS;
    public BindMode mode;
    public boolean visible;
    public final Animation animation = new Animation();

    public boolean hasModifiers() {
        return this.modifiers.length > 0;
    }

    public boolean matches(int i) {
        if (this.key != -1 && this.involvesKey(i)) {
            if (!Key.matches(this.key, i) && !Key.isPressed(this.key)) {
                return false;
            }

            for (int i2 : this.modifiers) {
                if (!Key.matches(i2, i) && !Key.isPressed(i2)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean isHeld() {
        if (this.key != -1 && Key.isPressed(this.key)) {
            for (int i : this.modifiers) {
                if (!Key.isPressed(i)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean involvesKey(int i) {
        if (Key.matches(this.key, i)) {
            return true;
        }

        for (int i2 : this.modifiers) {
            if (Key.matches(i2, i)) {
                return true;
            }
        }

        return false;
    }

    public String getDisplayString() {
        return Key.getComboName(this.modifiers, this.key);
    }

    public int getKey() {
        return this.key;
    }

    public int[] getModifiers() {
        return this.modifiers;
    }

    public BindMode getMode() {
        return this.mode;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public Animation getAnimation() {
        return this.animation;
    }

    public void setKey(int i) {
        this.key = i;
    }

    public void setModifiers(int[] ints) {
        this.modifiers = ints;
    }

    public void setMode(BindMode bindMode) {
        this.mode = bindMode;
    }

    public void setVisible(boolean flag) {
        this.visible = flag;
    }
}
