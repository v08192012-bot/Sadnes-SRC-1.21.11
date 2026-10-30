package tech.sadnes.client.cosmetic;

final class CosmeticAnimation {
    private final float length;
    private final CosmeticAnimation.Track[] rotationTracks;
    private final CosmeticAnimation.Track[] positionTracks;
    private final CosmeticAnimation.Track[] scaleTracks;

    CosmeticAnimation(float f, CosmeticAnimation.Track[] tracks, CosmeticAnimation.Track[] tracks2, CosmeticAnimation.Track[] tracks3) {
        this.length = f;
        this.rotationTracks = tracks;
        this.positionTracks = tracks2;
        this.scaleTracks = tracks3;
    }

    float getTime(double d) {
        return this.length <= 0.0F ? 0.0F : (float)(d % this.length);
    }

    CosmeticAnimation.Track getRotationTrack(int i) {
        return this.rotationTracks[i];
    }

    CosmeticAnimation.Track getPositionTrack(int i) {
        return this.positionTracks[i];
    }

    CosmeticAnimation.Track getScaleTrack(int i) {
        return this.scaleTracks[i];
    }

    record Track(float[] times, float[] pre, float[] post, boolean[] smooth) {
        void sample(float f, float[] floats) {
            int i = this.times.length - 1;
            if (f < this.times[0]) {
                System.arraycopy(this.pre, 0, floats, 0, 3);
            } else if (f >= this.times[i]) {
                System.arraycopy(this.post, i * 3, floats, 0, 3);
            } else {
                int i2 = 0;

                while (this.times[i2 + 1] <= f) {
                    i2++;
                }

                float f2 = (f - this.times[i2]) / (this.times[i2 + 1] - this.times[i2]);
                int i3 = i2 * 3;
                int i4 = i3 + 3;
                boolean flag = this.smooth[i2] || this.smooth[i2 + 1];
                int i5 = Math.max(i2 - 1, 0) * 3;
                int i6 = Math.min(i2 + 2, i) * 3;

                for (int i7 = 0; i7 < 3; i7++) {
                    float f3 = this.post[i3 + i7];
                    float f4 = this.pre[i4 + i7];
                    floats[i7] = flag ? catmullRom(this.post[i5 + i7], f3, f4, this.pre[i6 + i7], f2) : f3 + (f4 - f3) * f2;
                }
            }
        }

        private static float catmullRom(float f, float f2, float f3, float f4, float f5) {
            float f6 = f5 * f5;
            return 0.5F
                * (
                    2.0F * f2
                        + (f3 - f) * f5
                        + (2.0F * f - 5.0F * f2 + 4.0F * f3 - f4) * f6
                        + (3.0F * f2 - f - 3.0F * f3 + f4) * f6 * f5
                );
        }
    }
}
