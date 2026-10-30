package tech.sadnes.client.util;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.SimpleOption.DoubleSliderCallbacks;
import net.minecraft.client.option.SimpleOption.MaxSuppliableIntCallbacks;
import net.minecraft.client.option.SimpleOption.ValidatingIntSliderCallbacks;
import net.minecraft.sound.SoundCategory;
import tech.sadnes.api.mixin.accessor.SimpleOptionAccessor;

public final class OptionsUtil {
    private OptionsUtil() {
    }

    public static <T> void resetToDefault(SimpleOption<T> simpleOption) {
        @SuppressWarnings("unchecked")
        T value2 = (T)((SimpleOptionAccessor)(Object)simpleOption).sadnes$getDefaultValue();
        simpleOption.setValue(value2);
    }

    public static <T> boolean isDefault(SimpleOption<T> simpleOption) {
        Object object = ((SimpleOptionAccessor)(Object)simpleOption).sadnes$getDefaultValue();
        return Objects.equals(simpleOption.getValue(), object);
    }

    public static OptionsUtil.SliderOption soundVolume(MinecraftClient minecraftClient, SoundCategory soundCategory) {
        final SimpleOption simpleOption = minecraftClient.options.getSoundVolumeOption(soundCategory);
        return new OptionsUtil.SliderOption() {
            @Override
            public float get() {
                return ((Double)simpleOption.getValue()).floatValue();
            }

            @Override
            public void set(float f) {
                simpleOption.setValue((double)Math.max(0.0F, Math.min(1.0F, f)));
            }

            @Override
            public void reset() {
                OptionsUtil.resetToDefault(simpleOption);
            }

            @Override
            public boolean isDefault() {
                return OptionsUtil.isDefault(simpleOption);
            }
        };
    }

    public static OptionsUtil.SliderOption doubleSlider(final SimpleOption<Double> simpleOption) {
        return new OptionsUtil.SliderOption() {
            @Override
            public float get() {
                return ((Double)simpleOption.getValue()).floatValue();
            }

            @Override
            public void set(float f) {
                simpleOption.setValue((double)f);
            }

            @Override
            public void reset() {
                OptionsUtil.resetToDefault(simpleOption);
            }

            @Override
            public boolean isDefault() {
                return OptionsUtil.isDefault(simpleOption);
            }
        };
    }

    public static OptionsUtil.SliderOption intSlider(final SimpleOption<Integer> simpleOption) {
        return new OptionsUtil.SliderOption() {
            @Override
            public float get() {
                return ((Integer)simpleOption.getValue()).floatValue();
            }

            @Override
            public void set(float f) {
                simpleOption.setValue(Math.round(f));
            }

            @Override
            public void reset() {
                OptionsUtil.resetToDefault(simpleOption);
            }

            @Override
            public boolean isDefault() {
                return OptionsUtil.isDefault(simpleOption);
            }
        };
    }

    public static OptionsUtil.SliderOption deferredIntSlider(final SimpleOption<Integer> simpleOption) {
        return new OptionsUtil.SliderOption() {
            private Integer pending;

            @Override
            public float get() {
                return this.pending != null ? this.pending.floatValue() : ((Integer)simpleOption.getValue()).floatValue();
            }

            @Override
            public void set(float f) {
                this.pending = Math.round(f);
            }

            @Override
            public void commit() {
                if (this.pending != null) {
                    int i = this.pending;
                    this.pending = null;
                    simpleOption.setValue(i);
                }
            }

            @Override
            public void reset() {
                this.pending = null;
                OptionsUtil.resetToDefault(simpleOption);
            }

            @Override
            public boolean isDefault() {
                return this.pending == null && OptionsUtil.isDefault(simpleOption);
            }
        };
    }

    public static int[] getIntRange(SimpleOption<Integer> simpleOption, int i, int i2) {
        Object object = simpleOption.getCallbacks();
        if (object instanceof ValidatingIntSliderCallbacks validatingIntSliderCallbacks) {
            return new int[]{validatingIntSliderCallbacks.minInclusive(), validatingIntSliderCallbacks.maxInclusive()};
        } else {
            return object instanceof MaxSuppliableIntCallbacks maxSuppliableIntCallbacks ? new int[]{maxSuppliableIntCallbacks.minInclusive(), maxSuppliableIntCallbacks.maxInclusive()} : new int[]{i, i2};
        }
    }

    public static boolean isUnitRange(SimpleOption<Double> simpleOption) {
        Object object = simpleOption.getCallbacks();
        return object instanceof DoubleSliderCallbacks;
    }

    public static OptionsUtil.BoolOption toggle(final SimpleOption<Boolean> simpleOption) {
        return new OptionsUtil.BoolOption() {
            @Override
            public boolean get() {
                return (Boolean)simpleOption.getValue();
            }

            @Override
            public void set(boolean flag) {
                simpleOption.setValue(flag);
            }

            @Override
            public void reset() {
                OptionsUtil.resetToDefault(simpleOption);
            }

            @Override
            public boolean isDefault() {
                return OptionsUtil.isDefault(simpleOption);
            }
        };
    }

    public interface BoolOption {
        boolean get();

        void set(boolean flag);

        default void reset() {
        }

        default boolean isDefault() {
            return true;
        }
    }

    public interface SliderOption {
        float get();

        void set(float f);

        default void reset() {
        }

        default boolean isDefault() {
            return true;
        }

        default void commit() {
        }
    }
}
