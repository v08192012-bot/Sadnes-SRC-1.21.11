package tech.sadnes.client.util.color;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorTagUtil {
    public static final Pattern TAG_PATTERN = Pattern.compile("\\$\\{(rgba|rgb)\\((\\d{1,3}),(\\d{1,3}),(\\d{1,3})(?:,(\\d{1,3}))?\\)}|\\$\\{reset}", 2);

    private ColorTagUtil() {
    }

    public static String rgb(int i, int i2, int i3) {
        return new StringBuilder(20).append("${rgb(").append(i).append(',').append(i2).append(',').append(i3).append(")}").toString();
    }

    public static String rgba(int i, int i2, int i3, int i4) {
        return appendRgba(new StringBuilder(24), i, i2, i3, i4).toString();
    }

    public static String colorTag(int i) {
        return appendColorTag(new StringBuilder(24), i).toString();
    }

    public static StringBuilder appendColorTag(StringBuilder builder, int i) {
        return appendRgba(builder, ColorUtil.red(i), ColorUtil.green(i), ColorUtil.blue(i), ColorUtil.alpha(i));
    }

    private static StringBuilder appendRgba(StringBuilder builder, int i, int i2, int i3, int i4) {
        return builder.append("${rgba(").append(i).append(',').append(i2).append(',').append(i3).append(',').append(i4).append(")}");
    }

    public static int interpolate(int i, int i2, float f) {
        return ColorUtil.interpolate(i, i2, f);
    }

    public static String reset() {
        return "${reset}";
    }

    public static String stripTags(String string) {
        return TAG_PATTERN.matcher(string).replaceAll("");
    }

    public static String getRgbName() {
        return "rgb";
    }

    public static String getRgbaName() {
        return "rgba";
    }

    public static String replaceTagsRgb(String string, int i, int i2, int i3) {
        return TAG_PATTERN.matcher(string).replaceAll(Matcher.quoteReplacement(rgb(i, i2, i3)));
    }

    public static String replaceTagsRgba(String string, int i, int i2, int i3, int i4) {
        return TAG_PATTERN.matcher(string).replaceAll(Matcher.quoteReplacement(rgba(i, i2, i3, i4)));
    }
}
