package tech.sadnes.client.ui.clickgui.component;


public final class SliderDrop {
    private final float pos;
    private final float velocity;
    private final float hover;
    private final float press;
    private final float pulseAge;
    private final float waveAge;
    private final float waveAt;

    static float defaultPulseAge() {
        return 1.0F;
    }

    static float defaultWaveAge() {
        return 1.0F;
    }

    SliderDrop(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.pos = f;
        this.velocity = f2;
        this.hover = f3;
        this.press = f4;
        this.pulseAge = f5;
        this.waveAge = f6;
        this.waveAt = f7;
    }

    public static SliderDrop.Builder builder() {
        return new SliderDrop.Builder();
    }

    public float getPos() {
        return this.pos;
    }

    public float getVelocity() {
        return this.velocity;
    }

    public float getHover() {
        return this.hover;
    }

    public float getPress() {
        return this.press;
    }

    public float getPulseAge() {
        return this.pulseAge;
    }

    public float getWaveAge() {
        return this.waveAge;
    }

    public float getWaveAt() {
        return this.waveAt;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof SliderDrop sliderDrop)) {
            return false;
        } else if (Float.compare(this.getPos(), sliderDrop.getPos()) != 0) {
            return false;
        } else if (Float.compare(this.getVelocity(), sliderDrop.getVelocity()) != 0) {
            return false;
        } else if (Float.compare(this.getHover(), sliderDrop.getHover()) != 0) {
            return false;
        } else if (Float.compare(this.getPress(), sliderDrop.getPress()) != 0) {
            return false;
        } else if (Float.compare(this.getPulseAge(), sliderDrop.getPulseAge()) != 0) {
            return false;
        } else {
            return Float.compare(this.getWaveAge(), sliderDrop.getWaveAge()) != 0 ? false : Float.compare(this.getWaveAt(), sliderDrop.getWaveAt()) == 0;
        }
    }

    @Override
    public int hashCode() {
        return (
                    (
                                (
                                            (
                                                        ((59 + Float.floatToIntBits(this.getPos())) * 59 + Float.floatToIntBits(this.getVelocity())) * 59
                                                            + Float.floatToIntBits(this.getHover())
                                                    )
                                                    * 59
                                                + Float.floatToIntBits(this.getPress())
                                        )
                                        * 59
                                    + Float.floatToIntBits(this.getPulseAge())
                            )
                            * 59
                        + Float.floatToIntBits(this.getWaveAge())
                )
                * 59
            + Float.floatToIntBits(this.getWaveAt());
    }

    @Override
    public String toString() {
        return "SliderDrop(pos="
            + this.getPos()
            + ", velocity="
            + this.getVelocity()
            + ", hover="
            + this.getHover()
            + ", press="
            + this.getPress()
            + ", pulseAge="
            + this.getPulseAge()
            + ", waveAge="
            + this.getWaveAge()
            + ", waveAt="
            + this.getWaveAt()
            + ")";
    }

    public static class Builder {
        private float pos;
        private float velocity;
        private float hover;
        private float press;
        private boolean pulseAgeSet;
        private float pulseAgeValue;
        private boolean waveAgeSet;
        private float waveAgeValue;
        private float waveAt;

        Builder() {
        }

        public SliderDrop.Builder pos(float f) {
            this.pos = f;
            return this;
        }

        public SliderDrop.Builder velocity(float f) {
            this.velocity = f;
            return this;
        }

        public SliderDrop.Builder hover(float f) {
            this.hover = f;
            return this;
        }

        public SliderDrop.Builder press(float f) {
            this.press = f;
            return this;
        }

        public SliderDrop.Builder pulseAge(float f) {
            this.pulseAgeValue = f;
            this.pulseAgeSet = true;
            return this;
        }

        public SliderDrop.Builder waveAge(float f) {
            this.waveAgeValue = f;
            this.waveAgeSet = true;
            return this;
        }

        public SliderDrop.Builder waveAt(float f) {
            this.waveAt = f;
            return this;
        }

        public SliderDrop build() {
            float f = this.pulseAgeValue;
            if (!this.pulseAgeSet) {
                f = SliderDrop.defaultPulseAge();
            }

            float f2 = this.waveAgeValue;
            if (!this.waveAgeSet) {
                f2 = SliderDrop.defaultWaveAge();
            }

            return new SliderDrop(this.pos, this.velocity, this.hover, this.press, f, f2, this.waveAt);
        }

        @Override
        public String toString() {
            return "SliderDrop.SliderDropBuilder(pos="
                + this.pos
                + ", velocity="
                + this.velocity
                + ", hover="
                + this.hover
                + ", press="
                + this.press
                + ", pulseAge$value="
                + this.pulseAgeValue
                + ", waveAge$value="
                + this.waveAgeValue
                + ", waveAt="
                + this.waveAt
                + ")";
        }
    }
}
