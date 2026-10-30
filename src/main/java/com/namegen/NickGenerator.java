package com.namegen;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class NickGenerator {
    public static final String BUNDLED_MODEL_RESOURCE = "/com/namegen/namegen-model.bin";
    private final CharMLP model;
    private final Vocabulary vocab;
    private final int boundary;
    private final boolean[] isDigit;
    private final boolean[] isUnderscore;
    private final Random rnd;
    private final Set<String> seen = new HashSet<>();
    private final Set<String> blocklist = new HashSet<>();
    private double temperature = 0.9;
    private int topK = 0;
    private double topP = 0.92;
    private int minLength = 4;
    private int maxLength = 14;
    private boolean allowDigits = true;
    private boolean allowUnderscore = true;
    private boolean requireNameLike = true;
    private boolean caseInsensitiveUniqueness = true;
    private int maxAttempts = 200;

    private NickGenerator(CharMLP charMLP, Vocabulary vocabulary, long l) {
        this.model = charMLP;
        this.vocab = vocabulary;
        this.boundary = vocabulary.boundaryIndex();
        this.rnd = new Random(l);
        int i = vocabulary.size();
        this.isDigit = new boolean[i];
        this.isUnderscore = new boolean[i];

        for (int i2 = 0; i2 < i; i2++) {
            char c = vocabulary.charOf(i2);
            this.isDigit[i2] = c >= '0' && c <= '9';
            this.isUnderscore[i2] = c == '_';
        }
    }

    public static NickGenerator fromBundledModel() {
        return fromBundledModel(new Random().nextLong());
    }

    public static NickGenerator fromBundledModel(long l) {
        try {
            InputStream inputStream = NickGenerator.class.getResourceAsStream("/com/namegen/namegen-model.bin");

            NickGenerator nickGenerator;
            try {
                if (inputStream == null) {
                    throw new IOException("Bundled model resource not found: /com/namegen/namegen-model.bin (train one with the 'train' task first)");
                }

                ModelIO.Bundle bundle = ModelIO.load(inputStream);
                nickGenerator = new NickGenerator(bundle.model(), bundle.vocab(), l);
            } catch (Throwable throwable) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }

                throw throwable;
            }

            inputStream.close();
            return nickGenerator;
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    public static NickGenerator fromFile(Path path, long l) throws IOException {
        ModelIO.Bundle bundle = ModelIO.load(path);
        return new NickGenerator(bundle.model(), bundle.vocab(), l);
    }

    public static NickGenerator of(CharMLP charMLP, Vocabulary vocabulary, long l) {
        return new NickGenerator(charMLP, vocabulary, l);
    }

    public NickGenerator temperature(double d) {
        this.temperature = d;
        return this;
    }

    public NickGenerator topK(int i) {
        this.topK = Math.max(0, i);
        return this;
    }

    public NickGenerator topP(double d) {
        if (!(d <= 0.0) && !(d > 1.0)) {
            this.topP = d;
            return this;
        } else {
            this.topP = 1.0;
            return this;
        }
    }

    public NickGenerator lengthRange(int i, int i2) {
        if (i >= 1 && i2 >= i) {
            this.minLength = i;
            this.maxLength = i2;
            return this;
        } else {
            throw new IllegalArgumentException("Invalid length range: " + i + ".." + i2);
        }
    }

    public NickGenerator allowDigits(boolean flag) {
        this.allowDigits = flag;
        return this;
    }

    public NickGenerator allowUnderscore(boolean flag) {
        this.allowUnderscore = flag;
        return this;
    }

    public NickGenerator requireNameLike(boolean flag) {
        this.requireNameLike = flag;
        return this;
    }

    public NickGenerator caseInsensitiveUniqueness(boolean flag) {
        this.caseInsensitiveUniqueness = flag;
        return this;
    }

    public NickGenerator maxAttempts(int i) {
        this.maxAttempts = Math.max(1, i);
        return this;
    }

    public NickGenerator exclude(Collection<String> strings) {
        for (String string : strings) {
            this.blocklist.add(this.key(string));
        }

        return this;
    }

    public void reset() {
        this.seen.clear();
    }

    public String generate() {
        for (int i = 0; i < this.maxAttempts; i++) {
            String string = this.sampleOne();
            if (string != null && this.acceptable(string) && this.claim(string)) {
                return string;
            }
        }

        return this.forceUnique();
    }

    public List<String> generate(int i) {
        ArrayList arrayList = new ArrayList(i);

        for (int i2 = 0; i2 < i; i2++) {
            arrayList.add(this.generate());
        }

        return arrayList;
    }

    private String sampleOne() {
        int i = this.model.blockSize;
        int[] ints = new int[i];
        Arrays.fill(ints, this.boundary);
        double[] doubles = new double[this.vocab.size()];
        StringBuilder builder = new StringBuilder(this.maxLength);

        while (builder.length() < this.maxLength) {
            this.model.nextDistribution(ints, this.temperature, doubles);
            boolean flag = builder.length() >= this.minLength;
            int i2 = this.sampleMasked(doubles, flag);
            if (i2 == this.boundary) {
                break;
            }

            builder.append(this.vocab.charOf(i2));
            System.arraycopy(ints, 1, ints, 0, i - 1);
            ints[i - 1] = i2;
        }

        return builder.length() < this.minLength ? null : builder.toString();
    }

    private boolean acceptable(String string) {
        if (!this.requireNameLike) {
            return true;
        }

        int i = 0;
        int i2 = 0;

        for (int i3 = 0; i3 < string.length(); i3++) {
            char c = Character.toLowerCase(string.charAt(i3));
            if (c >= 'a' && c <= 'z') {
                i++;
                switch (c) {
                    case 'a':
                    case 'e':
                    case 'i':
                    case 'o':
                    case 'u':
                        i2++;
                }
            }
        }

        return i >= 2 && i2 >= 1;
    }

    private int sampleMasked(double[] doubles, boolean flag) {
        double d = 0.0;

        for (int i = 0; i < doubles.length; i++) {
            double d2 = doubles[i];
            if (i == this.boundary) {
                if (!flag) {
                    d2 = 0.0;
                }
            } else if (!this.allowDigits && this.isDigit[i] || !this.allowUnderscore && this.isUnderscore[i]) {
                d2 = 0.0;
            }

            doubles[i] = d2;
            d += d2;
        }

        if (d <= 0.0) {
            return this.boundary;
        }

        if (this.topK > 0 || this.topP < 1.0) {
            d = this.applyTopKTopP(doubles, d);
        }

        double d3 = this.rnd.nextDouble() * d;
        double d4 = 0.0;

        for (int i2 = 0; i2 < doubles.length; i2++) {
            d4 += doubles[i2];
            if (d3 < d4) {
                return i2;
            }
        }

        return doubles.length - 1;
    }

    private double applyTopKTopP(double[] doubles, double d) {
        int i = doubles.length;
        int[] ints = new int[i];
        int i2 = 0;

        while (i2 < i) {
            ints[i2] = i2++;
        }

        for (int i3 = 1; i3 < i; i3++) {
            int i4 = ints[i3];
            double d2 = doubles[i4];

            int i5;
            for (i5 = i3 - 1; i5 >= 0 && doubles[ints[i5]] < d2; i5--) {
                ints[i5 + 1] = ints[i5];
            }

            ints[i5 + 1] = i4;
        }

        double d3 = this.topP * d;
        double d4 = 0.0;
        int i6 = 0;
        int i7 = 0;

        double d5;
        int i8;
        while (true) {
            if (i7 >= i) {
                d5 = 0.0;
                i8 = 0;
                break;
            }

            if (doubles[ints[i7]] <= 0.0) {
                d5 = 0.0;
                i8 = 0;
                break;
            }

            d4 += doubles[ints[i7]];
            i6 = i7 + 1;
            if (this.topK > 0 && i6 >= this.topK) {
                d5 = 0.0;
                i8 = 0;
                break;
            }

            if (this.topP < 1.0 && d4 >= d3) {
                d5 = 0.0;
                i8 = 0;
                break;
            }

            i7++;
        }

        while (i8 < i6) {
            d5 += doubles[ints[i8]];
            i8++;
        }

        for (int i9 = i6; i9 < i; i9++) {
            doubles[ints[i9]] = 0.0;
        }

        return d5;
    }

    private boolean claim(String string) {
        String string2 = this.key(string);
        if (!this.blocklist.contains(string2) && !this.seen.contains(string2)) {
            this.seen.add(string2);
            return true;
        } else {
            return false;
        }
    }

    private String forceUnique() {
        for (int i = 0; i < 10000; i++) {
            String string = this.sampleOne();
            if (string == null) {
                string = this.fallbackName();
            }

            String string2 = string;
            if (!this.claim(string)) {
                string2 = string + (this.rnd.nextInt(900) + 100);
            }

            if (this.claim(string2)) {
                return string2;
            }
        }

        String string3;
        do {
            string3 = "nick" + this.rnd.nextInt(Integer.MAX_VALUE);
        } while (!this.claim(string3));

        return string3;
    }

    private String fallbackName() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < this.minLength; i++) {
            int i2 = 1 + this.rnd.nextInt(this.vocab.size() - 1);
            builder.append(this.vocab.charOf(i2));
        }

        return builder.toString();
    }

    private String key(String string) {
        return this.caseInsensitiveUniqueness ? string.toLowerCase(Locale.ROOT) : string;
    }
}
