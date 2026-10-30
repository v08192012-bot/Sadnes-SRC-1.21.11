package com.namegen.cli;

import com.namegen.NickGenerator;

public final class GenerateMain {
    public static void main(String[] strings) {
        int i = strings.length >= 1 ? Integer.parseInt(strings[0]) : 20;
        double d = strings.length >= 2 ? Double.parseDouble(strings[1]) : 0.9;
        int i2 = strings.length >= 3 ? Integer.parseInt(strings[2]) : 0;
        double d2 = strings.length >= 4 ? Double.parseDouble(strings[3]) : 0.92;
        NickGenerator nickGenerator = NickGenerator.fromBundledModel().temperature(d).topK(i2).topP(d2).lengthRange(4, 14);

        for (String string : nickGenerator.generate(i)) {
            System.out.println(string);
        }
    }

    private GenerateMain() {
    }
}
