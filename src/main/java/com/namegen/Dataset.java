package com.namegen;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

public final class Dataset {
    public final Vocabulary vocab;
    public final int blockSize;
    public final int[] x;
    public final int[] y;
    public final int count;

    private Dataset(Vocabulary vocabulary, int i, int[] ints, int[] ints2, int i2) {
        this.vocab = vocabulary;
        this.blockSize = i;
        this.x = ints;
        this.y = ints2;
        this.count = i2;
    }

    public static List<String> loadLogins(Path path, int i, int i2, int i3, boolean flag, long l) throws IOException {
        Random random = new Random(l);
        String[] strings = new String[i3];
        long l2 = 0L;

        String string;
        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            while ((string = bufferedReader.readLine()) != null) {
                int i4 = string.indexOf(58);
                String string2 = i4 >= 0 ? string.substring(0, i4) : string;
                if (isValid(string2, i, i2) && (!flag || isNameLike(string2))) {
                    if (l2 < i3) {
                        strings[(int)l2] = string2;
                    } else {
                        long l3 = Math.floorMod(random.nextLong(), l2 + 1L);
                        if (l3 < i3) {
                            strings[(int)l3] = string2;
                        }
                    }

                    l2++;
                }
            }
        }

        int i5 = (int)Math.min(l2, i3);
        LinkedHashSet linkedHashSet = new LinkedHashSet();

        for (int i6 = 0; i6 < i5; i6++) {
            linkedHashSet.add(strings[i6]);
        }

        return new ArrayList<>(linkedHashSet);
    }

    private static boolean isValid(String string, int i, int i2) {
        int i3 = string.length();
        if (i3 >= i && i3 <= i2) {
            for (int i4 = 0; i4 < i3; i4++) {
                char c = string.charAt(i4);
                if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_') {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private static boolean isNameLike(String string) {
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 1;
        int i5 = 1;

        for (int i6 = 0; i6 < string.length(); i6++) {
            char c = string.charAt(i6);
            char c2 = Character.toLowerCase(c);
            switch (c2) {
                case 'a':
                case 'e':
                case 'i':
                case 'o':
                case 'u':
                    i++;
            }

            if (c >= '0' && c <= '9') {
                i2++;
            } else if (c2 >= 'a' && c2 <= 'z') {
                i3++;
            }

            if (i6 > 0 && c == string.charAt(i6 - 1)) {
                if (++i5 > i4) {
                    i4 = i5;
                }
            } else {
                i5 = 1;
            }
        }

        return i3 >= 2 && i >= 1 && i2 <= string.length() / 2 && i4 <= 3;
    }

    public static Dataset build(List<String> strings, int i) {
        return build(strings, i, Vocabulary.fromNames(strings));
    }

    public static Dataset build(List<String> strings, int i, Vocabulary vocabulary) {
        int i2 = 0;

        for (String string : strings) {
            i2 += string.length() + 1;
        }

        int[] ints = new int[i2 * i];
        int[] ints2 = new int[i2];
        int i3 = 0;
        int i4 = vocabulary.boundaryIndex();
        int[] ints3 = new int[i];

        for (String string2 : strings) {
            Arrays.fill(ints3, i4);

            for (int i5 = 0; i5 <= string2.length(); i5++) {
                int i6 = i5 < string2.length() ? vocabulary.indexOf(string2.charAt(i5)) : i4;
                System.arraycopy(ints3, 0, ints, i3 * i, i);
                ints2[i3] = i6;
                i3++;
                System.arraycopy(ints3, 1, ints3, 0, i - 1);
                ints3[i - 1] = i6;
            }
        }

        return new Dataset(vocabulary, i, ints, ints2, i3);
    }
}
