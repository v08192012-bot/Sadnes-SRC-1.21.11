package tech.sadnes.client.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.ResourceUtil;

public final class Language {
    public static final String DEFAULT_LOCALE = "ru_ru";
    public static final List<Language.LocaleEntry> LOCALES = List.of(
        new Language.LocaleEntry("ru_ru", "Русский"),
        new Language.LocaleEntry("en_us", "English"),
        new Language.LocaleEntry("uk_ua", "Українська"),
        new Language.LocaleEntry("pl_pl", "Polski")
    );
    private static final Logger LOGGER = LoggerFactory.getLogger("Lang");
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = (new TypeToken<LinkedHashMap<String, String>>() {}).getType();
    private static final Map<String, Map<String, String>> TRANSLATIONS = new LinkedHashMap<>();
    private static String currentLocale = "ru_ru";

    private Language() {
    }

    public static void load() {
        TRANSLATIONS.clear();

        for (Language.LocaleEntry localeEntry : LOCALES) {
            TRANSLATIONS.put(localeEntry.code(), loadLocale(localeEntry.code()));
        }

        String string = readSavedLocale();
        if (isSupported(string)) {
            currentLocale = string;
        } else {
            currentLocale = "ru_ru";
        }
    }

    public static String translate(String string) {
        return translate(string, string);
    }

    public static String translate(String string, String string2) {
        if (string == null) {
            return string2;
        }

        String string3 = lookup(currentLocale, string);
        if (string3 != null) {
            return string3;
        }

        if (!"ru_ru".equals(currentLocale)) {
            String string4 = lookup("ru_ru", string);
            if (string4 != null) {
                return string4;
            }
        }

        return string2;
    }

    public static String translateOption(String string, String string2) {
        return string != null && string2 != null ? translate(string + "." + string2, string2) : string2;
    }

    public static String getLocale() {
        return currentLocale;
    }

    public static void setLocale(String string) {
        if (string != null && !string.equals(currentLocale) && isSupported(string)) {
            currentLocale = string;
            saveLocale(string);
        }
    }

    public static String getNativeName(String string) {
        for (Language.LocaleEntry localeEntry : LOCALES) {
            if (localeEntry.code().equals(string)) {
                return localeEntry.nativeName();
            }
        }

        return string;
    }

    public static String getCodeByNativeName(String string) {
        for (Language.LocaleEntry localeEntry : LOCALES) {
            if (localeEntry.nativeName().equals(string)) {
                return localeEntry.code();
            }
        }

        return "ru_ru";
    }

    private static String lookup(String string, String string2) {
        Map map = TRANSLATIONS.get(string);
        return map == null ? null : (String)map.get(string2);
    }

    private static boolean isSupported(String string) {
        for (Language.LocaleEntry localeEntry : LOCALES) {
            if (localeEntry.code().equals(string)) {
                return true;
            }
        }

        return false;
    }

    private static Map<String, String> loadLocale(String string) {
        try {
            String string2 = ResourceUtil.readText(Identifier.of(ClientInfo.MOD_ID, "lang/" + string + ".json"));
            if (string2 != null && !string2.isEmpty()) {
                Map map = GSON.fromJson(string2, MAP_TYPE);
                return map != null ? map : new LinkedHashMap<>();
            } else {
                return new LinkedHashMap<>();
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load lang '{}': {}", string, exception.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private static Path getLocaleFile() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("lang.txt");
    }

    private static String readSavedLocale() {
        try {
            Path path = getLocaleFile();
            return !Files.exists(path) ? "ru_ru" : Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (Exception exception) {
            return "ru_ru";
        }
    }

    private static void saveLocale(String string) {
        try {
            Path path = getLocaleFile();
            Files.createDirectories(path.getParent());
            Files.writeString(path, string, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save locale: {}", exception.getMessage());
        }
    }

    public record LocaleEntry(String code, String nativeName) {
    }
}
