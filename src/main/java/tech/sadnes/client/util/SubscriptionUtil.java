package tech.sadnes.client.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import tech.sadnes.client.lang.Language;
import ua.mintantileak.profile.Profile;

public final class SubscriptionUtil {
    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
        DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    private SubscriptionUtil() {
    }

    public static String getSubscriptionText() {
        String string = Profile.getSubscriptionEndDate();
        LocalDate localDate = parseDate(string);
        if (localDate == null) {
            return string;
        }

        LocalDate localDate2 = LocalDate.now();
        if (!localDate.isAfter(localDate2)) {
            return Language.translate("profile.sub.expired", "expired");
        }

        String string2 = Language.translate("profile.month." + localDate.getMonthValue(), String.valueOf(localDate.getMonthValue()));
        boolean flag = localDate.getYear() == localDate2.getYear();
        String string3 = flag
            ? Language.translate("profile.sub.date", "until {month} {day}")
            : Language.translate("profile.sub.dateYear", "until {month} {day}, {year}");
        return string3.replace("{day}", String.valueOf(localDate.getDayOfMonth())).replace("{month}", string2).replace("{year}", String.valueOf(localDate.getYear()));
    }

    private static LocalDate parseDate(String string) {
        if (string != null && !string.isEmpty()) {
            for (DateTimeFormatter dateTimeFormatter : DATE_FORMATTERS) {
                try {
                    return LocalDate.parse(string, dateTimeFormatter);
                } catch (DateTimeParseException dateTimeParseException) {
                }
            }

            return null;
        } else {
            return null;
        }
    }
}
