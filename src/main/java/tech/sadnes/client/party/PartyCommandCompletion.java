package tech.sadnes.client.party;

import java.util.ArrayList;
import java.util.List;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.network.BackendService;

public final class PartyCommandCompletion {
    private static final List<String[]> COMMAND_PREFIXES = List.of(
        new String[]{"tpa"}, new String[]{"call"}, new String[]{"msg"}, new String[]{"m"}, new String[]{"clan", "invite"}
    );

    private PartyCommandCompletion() {
    }

    public static PartyCommandCompletion.Suggestion suggest(String string, int i) {
        if (string != null && string.startsWith("/")) {
            List list = tokenize(string);
            int i2 = findTokenAt(list, i);
            if (i2 >= 0) {
                if (i2 != matchCommandPrefix(string, list)) {
                    return null;
                }

                String string2 = string.substring(((int[])list.get(i2))[0], i);
                if (string2.isEmpty()) {
                    return null;
                }

                PartyManager partyManager = getPartyManager();
                if (partyManager != null) {
                    if (!partyManager.inParty()) {
                        return null;
                    }

                    List list2 = partyManager.suggestMembers(string2);
                    if (list2.isEmpty()) {
                        return null;
                    }

                    return new PartyCommandCompletion.Suggestion(((int[])list.get(i2))[0], i, list2);
                }
            }
        }

        return null;
    }

    public static String resolveMemberName(String string) {
        if (string != null && string.startsWith("/")) {
            List list = tokenize(string);
            int i = matchCommandPrefix(string, list);
            if (i >= 0) {
                if (i >= list.size()) {
                    return string;
                }

                PartyManager partyManager = getPartyManager();
                if (partyManager != null) {
                    if (!partyManager.inParty()) {
                        return string;
                    }

                    int[] ints = (int[])list.get(i);
                    String string2 = string.substring(ints[0], ints[1]);
                    String string3 = partyManager.getMemberServer(string2);
                    if (!string3.isEmpty()) {
                        if (string3.equalsIgnoreCase(string2)) {
                            return string;
                        }

                        return string.substring(0, ints[0]) + string3 + string.substring(ints[1]);
                    }
                }
            }
        }

        return string;
    }

    private static int matchCommandPrefix(String string, List<int[]> intses) {
        for (String[] strings : COMMAND_PREFIXES) {
            if (intses.size() >= strings.length) {
                boolean flag = true;
                int i = 0;

                for (int i2 = strings.length; i < i2 && flag; i++) {
                    int[] ints = (int[])intses.get(i);
                    flag = string.regionMatches(true, ints[0], strings[i], 0, strings[i].length()) && ints[1] - ints[0] == strings[i].length();
                }

                if (flag) {
                    return strings.length;
                }
            }
        }

        return -1;
    }

    private static List<int[]> tokenize(String string) {
        ArrayList arrayList = new ArrayList();
        int i = string.length();
        int i2 = 1;

        while (i2 < i) {
            while (i2 < i && string.charAt(i2) == ' ') {
                i2++;
            }

            int i3 = i2;

            while (i2 < i && string.charAt(i2) != ' ') {
                i2++;
            }

            if (i2 > i3) {
                arrayList.add(new int[]{i3, i2});
            }
        }

        return arrayList;
    }

    private static int findTokenAt(List<int[]> intses, int i) {
        int i2 = 0;

        for (int i3 = intses.size(); i2 < i3; i2++) {
            int[] ints = (int[])intses.get(i2);
            if (i > ints[0] && i <= ints[1]) {
                return i2;
            }
        }

        return -1;
    }

    private static PartyManager getPartyManager() {
        BackendService backendService = Sadnes.getInstance().getBackend();
        return backendService == null ? null : backendService.getPartyManager();
    }

    public record Suggestion(int start, int end, List<String> logins) {
    }
}
