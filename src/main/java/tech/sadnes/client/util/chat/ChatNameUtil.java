package tech.sadnes.client.util.chat;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

public final class ChatNameUtil {
    private ChatNameUtil() {
    }

    public static String normalize(String string) {
        if (string != null && !string.isBlank()) {
            String string2 = Normalizer.normalize(stripFormatting(string), Form.NFKD).toLowerCase(Locale.ROOT);
            StringBuilder builder = new StringBuilder(string2.length());
            boolean flag = true;
            int i = 0;
            int i2 = string2.length();

            while (i < i2) {
                int i3 = string2.codePointAt(i);
                i += Character.charCount(i3);
                if (!isCombiningMark(i3) && !isInvisibleChar(i3)) {
                    int i4 = foldConfusable(i3);
                    boolean flag2 = i4 >= 97 && i4 <= 122 || i4 >= 48 && i4 <= 57;
                    if (flag2) {
                        builder.appendCodePoint(i4);
                        flag = false;
                    } else if (!flag) {
                        builder.append(' ');
                        flag = true;
                    }
                }
            }

            int i5 = builder.length();
            if (i5 > 0 && builder.charAt(i5 - 1) == ' ') {
                builder.setLength(i5 - 1);
            }

            return builder.toString();
        } else {
            return "";
        }
    }

    public static boolean containsName(String string, String string2) {
        String string3 = stripFormatting(string == null ? "" : string).toLowerCase(Locale.ROOT);
        String string4 = stripFormatting(string2 == null ? "" : string2).trim().toLowerCase(Locale.ROOT);
        if (!string4.matches("[a-z0-9_]{1,16}")) {
            String string5 = normalize(string);
            String string6 = normalize(string2);
            if (!string5.isBlank() && !string6.isBlank()) {
                if (!string5.equals(string6) && !(" " + string5 + " ").contains(" " + string6 + " ")) {
                    return false;
                }

                return true;
            }
        } else {
            int i = 0;

            while (i <= string3.length() - string4.length()) {
                int i2 = string3.indexOf(string4, i);
                if (i2 < 0) {
                    return false;
                }

                int i3 = i2 + string4.length();
                boolean flag;
                if (i2 != 0) {
                    flag = !isNameChar(string3.charAt(i2 - 1));
                } else {
                    flag = true;
                }

                boolean flag2;
                if (i3 != string3.length()) {
                    flag2 = !isNameChar(string3.charAt(i3));
                } else {
                    flag2 = true;
                }

                if (flag && flag2) {
                    return true;
                }

                i = i2 + 1;
            }
        }

        return false;
    }

    public static boolean isLeaveMessage(String string, String string2) {
        if (!containsName(string, string2)) {
            return false;
        }

        String string3 = normalize(string2);
        if (string3.isBlank()) {
            return false;
        }

        String string4 = " " + normalize(string) + " ";
        String string5 = " " + string3 + " ";
        return string4.contains(string5 + "left the game ")
            || string4.contains(string5 + "left ")
            || string4.contains(string5 + "has left ")
            || string4.contains(string5 + "quit ")
            || string4.contains(string5 + "has quit ")
            || string4.contains(string5 + "disconnected ")
            || string4.contains(string5 + "logged out ");
    }

    public static boolean isValidUsername(String string) {
        if (string == null) {
            return false;
        }

        String string2 = string.trim();
        int i = string2.length();
        return i >= 3 && i <= 16 && string2.indexOf(32) < 0;
    }

    public static boolean isValidNameToken(String string) {
        if (string == null) {
            return false;
        }

        int i = string.length();
        if (i >= 2 && i <= 32) {
            boolean flag = false;
            int i2 = 0;

            while (i2 < i) {
                char c = string.charAt(i2);
                if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9')) {
                    switch (c) {
                        case ' ':
                        case '.':
                        case '_':
                            i2++;
                            break;
                        default:
                            return false;
                    }
                } else {
                    flag = true;
                    i2++;
                }
            }

            return flag;
        } else {
            return false;
        }
    }

    private static boolean isNameChar(char c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_';
    }

    private static boolean isCombiningMark(int i) {
        int i2 = Character.getType(i);
        switch (i2) {
            case 6:
            case 7:
            case 8:
                return true;
            default:
                return false;
        }
    }

    private static boolean isInvisibleChar(int i) {
        switch (i) {
            case 167:
            case 8203:
            case 8204:
            case 8205:
            case 65279:
                return true;
            default:
                return false;
        }
    }

    private static String stripFormatting(String string) {
        if (string != null && !string.isEmpty()) {
            StringBuilder builder = new StringBuilder(string.length());
            int i = string.length();
            int i2 = 0;

            while (i2 < i) {
                char c = string.charAt(i2);
                if (c == 167 && i2 + 1 < i && "0123456789abcdefklmnorx".indexOf(Character.toLowerCase(string.charAt(i2 + 1))) >= 0) {
                    i2++;
                    i2++;
                } else {
                    builder.append(c);
                    i2++;
                }
            }

            return builder.toString();
        } else {
            return "";
        }
    }

    private static int foldConfusable(int i) {
        switch (i) {
            case 593:
            case 945:
            case 1072:
            case 7424:
                return 97;
            case 609:
            case 610:
                return 103;
            case 618:
            case 953:
            case 1110:
                return 105;
            case 628:
            case 1400:
                return 110;
            case 640:
                return 114;
            case 655:
            case 1091:
                return 121;
            case 665:
            case 946:
            case 1074:
                return 98;
            case 668:
            case 1085:
                return 104;
            case 671:
                return 108;
            case 949:
            case 1077:
            case 7431:
                return 101;
            case 954:
            case 1082:
            case 7435:
                return 107;
            case 957:
            case 7456:
                return 118;
            case 959:
            case 1086:
            case 7439:
                return 111;
            case 961:
            case 1088:
            case 7448:
                return 112;
            case 964:
            case 1090:
            case 7451:
                return 116;
            case 965:
            case 7452:
                return 117;
            case 1010:
            case 1089:
            case 7428:
                return 99;
            case 1084:
            case 7437:
                return 109;
            case 1093:
                return 120;
            case 1109:
            case 42801:
                return 115;
            case 1112:
            case 7434:
                return 106;
            case 1121:
            case 7457:
                return 119;
            case 1281:
            case 7429:
                return 100;
            case 1307:
                return 113;
            case 7458:
            case 7459:
                return 122;
            case 42800:
                return 102;
            default:
                return i;
        }
    }
}
