package tech.sadnes.client.neuro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import tech.sadnes.client.util.crypto.DataEncryption;

public final class NeuroDataset {
    private static final int MAGIC = 1397638227;
    private final String name;
    public final int featureSize;
    public final int labelSize;
    private final List<NeuroDataset.Session> sessions = new ArrayList<>();
    private List<float[]> currentFeatures;
    private List<float[]> currentLabels;

    public NeuroDataset(String string, int i, int i2) {
        this.name = string;
        this.featureSize = i;
        this.labelSize = i2;
    }

    public void beginSession() {
        this.currentFeatures = new ArrayList<>();
        this.currentLabels = new ArrayList<>();
    }

    public void addSample(float[] floats, float[] floats2) {
        if (this.currentFeatures == null) {
            this.beginSession();
        }

        this.currentFeatures.add((float[])floats.clone());
        this.currentLabels.add((float[])floats2.clone());
    }

    public void endSession() {
        if (this.currentFeatures != null) {
            if (this.currentFeatures.size() >= 8) {
                this.sessions.add(new NeuroDataset.Session(this.currentFeatures.toArray(new float[0][]), this.currentLabels.toArray(new float[0][]), 1.0F));
            }

            this.currentFeatures = null;
            this.currentLabels = null;
        }
    }

    public int getSessionCount() {
        return this.sessions.size();
    }

    public NeuroDataset copy() {
        NeuroDataset neuroDataset = new NeuroDataset(this.name, this.featureSize, this.labelSize);
        neuroDataset.sessions.addAll(this.sessions);
        return neuroDataset;
    }

    public int getSampleCount() {
        int i = this.currentFeatures != null ? this.currentFeatures.size() : 0;

        for (NeuroDataset.Session session : this.sessions) {
            i += session.features().length;
        }

        return i;
    }

    public NeuroDataset.Samples buildSamples(int i, boolean[] booleans) {
        ArrayList<float[]> floatses = new ArrayList<>();
        ArrayList<float[]> floatses2 = new ArrayList<>();
        ArrayList arrayList = new ArrayList();
        int i2 = this.sessions.size();

        for (int i3 = 0; i3 < i2; i3++) {
            if (booleans == null || booleans[i3]) {
                NeuroDataset.Session session = this.sessions.get(i3);
                float[][] floatses3 = session.features();
                float[][] floatses4 = session.labels();
                int i4 = 0;

                for (int i5 = floatses3.length; i4 < i5; i4++) {
                    float[] floats = new float[this.featureSize * i];

                    for (int i6 = 0; i6 < i; i6++) {
                        int i7 = i4 - (i - 1) + i6;
                        if (i7 < 0) {
                            i7 = 0;
                        }

                        System.arraycopy(floatses3[i7], 0, floats, i6 * this.featureSize, this.featureSize);
                    }

                    floatses.add(floats);
                    floatses2.add(floatses4[i4]);
                    arrayList.add(session.weight());
                }
            }
        }

        float[] floats2 = new float[arrayList.size()];
        int i8 = 0;

        for (int i9 = floats2.length; i8 < i9; i8++) {
            floats2[i8] = (Float)arrayList.get(i8);
        }

        return new NeuroDataset.Samples(floatses.toArray(new float[0][]), floatses2.toArray(new float[0][]), floats2);
    }

    public void save(File file) throws IOException {
        File file2 = file.getParentFile();
        if (file2 != null) {
            file2.mkdirs();
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.writeInt(1397638227);
            dataOutputStream.writeInt(this.featureSize);
            dataOutputStream.writeInt(this.labelSize);
            dataOutputStream.writeInt(this.sessions.size());

            for (NeuroDataset.Session session : this.sessions) {
                float[][] floatses = session.features();
                float[][] floatses2 = session.labels();
                dataOutputStream.writeInt(floatses.length);
                int i = 0;

                for (int i2 = floatses.length; i < i2; i++) {
                    for (float f : floatses[i]) {
                        dataOutputStream.writeFloat(f);
                    }

                    for (float f2 : floatses2[i]) {
                        dataOutputStream.writeFloat(f2);
                    }
                }
            }
        }

        Files.write(file.toPath(), DataEncryption.encrypt(byteArrayOutputStream.toByteArray()));
    }

    private static DataInputStream openStream(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length < 4 || ByteBuffer.wrap(bytes).getInt() != 1397638227) {
            try {
                bytes = DataEncryption.decrypt(bytes);
            } catch (IllegalStateException illegalStateException) {
                throw new IOException("dataset.incompatible", illegalStateException);
            }
        }

        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    public static int countSamples(File file) throws IOException {
        int i;
        try (DataInputStream dataInputStream = openStream(file)) {
            if (dataInputStream.readInt() != 1397638227) {
                throw new IOException("dataset.incompatible");
            }

            int i2 = dataInputStream.readInt();
            int i3 = dataInputStream.readInt();
            int i4 = dataInputStream.readInt();
            if (i2 < 1 || i3 < 1 || i4 < 0) {
                throw new IOException("dataset.incompatible");
            }

            int i5 = (i2 + i3) * 4;
            i = 0;

            for (int i6 = 0; i6 < i4; i6++) {
                int i7 = dataInputStream.readInt();
                if (i7 < 0) {
                    throw new IOException("dataset.incompatible");
                }

                i += i7;
                int i8 = i7 * i5;

                while (i8 > 0) {
                    int i9 = dataInputStream.skipBytes(i8);
                    if (i9 <= 0) {
                        throw new IOException("dataset.incompatible");
                    }

                    i8 -= i9;
                }
            }
        }

        return i;
    }

    public static NeuroDataset load(File file) throws IOException {
        String string = file.getName();
        String string2 = string.endsWith(".dataset") ? string.substring(0, string.length() - 8) : string;

        NeuroDataset neuroDataset;
        try (DataInputStream dataInputStream = openStream(file)) {
            if (dataInputStream.readInt() != 1397638227) {
                throw new IOException("dataset.incompatible");
            }

            int i = dataInputStream.readInt();
            int i2 = dataInputStream.readInt();
            int i3 = dataInputStream.readInt();
            neuroDataset = new NeuroDataset(string2, i, i2);

            for (int i4 = 0; i4 < i3; i4++) {
                int i5 = dataInputStream.readInt();
                neuroDataset.beginSession();

                for (int i6 = 0; i6 < i5; i6++) {
                    float[] floats = new float[i];
                    float[] floats2 = new float[i2];

                    for (int i7 = 0; i7 < i; i7++) {
                        floats[i7] = dataInputStream.readFloat();
                    }

                    for (int i8 = 0; i8 < i2; i8++) {
                        floats2[i8] = dataInputStream.readFloat();
                    }

                    neuroDataset.addSample(floats, floats2);
                }

                neuroDataset.endSession();
            }
        }

        return neuroDataset;
    }

    public String getName() {
        return this.name;
    }

    public static final class Samples {
        public final float[][] inputs;
        public final float[][] targets;
        public final float[] weights;

        public Samples(float[][] floatses, float[][] floatses2, float[] floats) {
            this.inputs = floatses;
            this.targets = floatses2;
            this.weights = floats;
        }

        public int size() {
            return this.inputs.length;
        }
    }

    public record Session(float[][] features, float[][] labels, float weight) {
    }
}
