package tech.sadnes.client.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import local.sadnes.OfflineRouting;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.msa.data.MsaEnvironment;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaRefreshTokenRequest;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MicrosoftAuthenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger("MicrosoftAuth");
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final MsaApplicationConfig APP_CONFIG = new MsaApplicationConfig("00000000402b5328", "service::user.auth.xboxlive.com::MBI_SSL");
    private static final Duration TIMEOUT = Duration.ofSeconds(20L);
    private static final int MAX_REDIRECTS = 8;
    private volatile MicrosoftAuthenticator.State state = MicrosoftAuthenticator.State.IDLE;
    private volatile String userCode = "";
    private volatile String verificationUri = "";
    private volatile String directVerificationUri = "";
    private volatile String error = "";
    private volatile Account result;
    private Thread thread;

    public MicrosoftAuthenticator.State getState() {
        return this.state;
    }

    public String getUserCode() {
        return this.userCode;
    }

    public String getVerificationUri() {
        return this.verificationUri;
    }

    public String getDirectVerificationUri() {
        return this.directVerificationUri;
    }

    public String getError() {
        return this.error;
    }

    public boolean isBusy() {
        return this.state == MicrosoftAuthenticator.State.SELECTING_FILE
            || this.state == MicrosoftAuthenticator.State.REQUESTING
            || this.state == MicrosoftAuthenticator.State.AUTHENTICATING_COOKIE
            || this.state == MicrosoftAuthenticator.State.AUTHENTICATING_REFRESH_TOKEN
            || this.state == MicrosoftAuthenticator.State.AWAITING_USER;
    }

    public void startDeviceCodeLogin() {
        if (!this.isBusy()) {
            this.state = MicrosoftAuthenticator.State.REQUESTING;
            this.userCode = "";
            this.verificationUri = "";
            this.directVerificationUri = "";
            this.error = "";
            this.result = null;
            this.thread = new Thread(this::runDeviceCodeLogin, "Sadnes-MS-Auth");
            this.thread.setDaemon(true);
            this.thread.start();
        }
    }

    public void startFileLogin(String string, String string2) {
        if (!this.isBusy()) {
            this.userCode = "";
            this.verificationUri = "";
            this.directVerificationUri = "";
            this.error = "";
            this.result = null;
            this.state = MicrosoftAuthenticator.State.SELECTING_FILE;
            this.thread = new Thread(() -> this.runFileLogin(string, string2), "Sadnes-MS-File-Auth");
            this.thread.setDaemon(true);
            this.thread.start();
        }
    }

    private void runDeviceCodeLogin() {
        try {
            HttpClient httpClient = MinecraftAuth.createHttpClient();
            Consumer<net.raphimc.minecraftauth.msa.model.MsaDeviceCode> consumer = msaDeviceCode -> {
                this.userCode = msaDeviceCode.getUserCode();
                this.verificationUri = msaDeviceCode.getVerificationUri();
                this.directVerificationUri = msaDeviceCode.getDirectVerificationUri();
                this.state = MicrosoftAuthenticator.State.AWAITING_USER;
            };
            JavaAuthManager javaAuthManager = JavaAuthManager.create(httpClient).login(DeviceCodeMsaAuthService::new, consumer);
            MinecraftProfile minecraftProfile = javaAuthManager.getMinecraftProfile().getUpToDate();
            String string = javaAuthManager.getMinecraftToken().getUpToDate().getToken();
            String string2 = JavaAuthManager.toJson(javaAuthManager).toString();
            this.result = new Account(Account.Type.MICROSOFT, minecraftProfile.getName(), minecraftProfile.getId().toString(), string, string2);
            this.state = MicrosoftAuthenticator.State.SUCCESS;
        } catch (Exception exception) {
            LOGGER.error("Microsoft device-code login failed", exception);
            this.error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            this.state = MicrosoftAuthenticator.State.ERROR;
        }
    }

    private void runFileLogin(String string, String string2) {
        try {
            Path path = openFileDialog(string, string2);
            if (path == null) {
                this.state = MicrosoftAuthenticator.State.IDLE;
                return;
            }

            String string3 = readRefreshToken(path);
            if (Thread.currentThread().isInterrupted() || this.state == MicrosoftAuthenticator.State.IDLE) {
                this.state = MicrosoftAuthenticator.State.IDLE;
                return;
            }

            HttpClient httpClient = MinecraftAuth.createHttpClient();
            boolean flag = string3 != null;
            JavaAuthManager javaAuthManager;
            if (flag) {
                this.state = MicrosoftAuthenticator.State.AUTHENTICATING_REFRESH_TOKEN;
                javaAuthManager = loginWithRefreshToken(httpClient, string3);
            } else {
                this.state = MicrosoftAuthenticator.State.AUTHENTICATING_COOKIE;
                javaAuthManager = loginWithCookies(httpClient, readCookies(path));
            }

            MinecraftProfile minecraftProfile = javaAuthManager.getMinecraftProfile().getUpToDate();
            String string4 = javaAuthManager.getMinecraftToken().getUpToDate().getToken();
            String string5 = JavaAuthManager.toJson(javaAuthManager).toString();
            if (Thread.currentThread().isInterrupted() || this.state == MicrosoftAuthenticator.State.IDLE) {
                return;
            }

            this.result = new Account(Account.Type.MICROSOFT, minecraftProfile.getName(), minecraftProfile.getId().toString(), string4, string5, !flag, flag);
            this.state = MicrosoftAuthenticator.State.SUCCESS;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            if (this.state != MicrosoftAuthenticator.State.IDLE) {
                this.failFileLogin(interruptedException);
            }
        } catch (Exception exception) {
            if (this.state != MicrosoftAuthenticator.State.IDLE) {
                this.failFileLogin(exception);
            }
        }
    }

    private static Path openFileDialog(String string, String string2) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            PointerBuffer pointerBuffer = memoryStack.mallocPointer(2);
            pointerBuffer.put(memoryStack.UTF8("*.txt"));
            pointerBuffer.put(memoryStack.UTF8("*.*"));
            pointerBuffer.flip();
            Path path = Path.of(System.getProperty("user.home"), "Downloads", "AyuGram Desktop");
            Path path2 = Path.of(System.getProperty("user.home"), "Desktop");
            String string3 = Files.isDirectory(path) ? path.toString() : (Files.isDirectory(path2) ? path2.toString() : System.getProperty("user.home"));
            String string4 = TinyFileDialogs.tinyfd_openFileDialog(string, string3, pointerBuffer, string2, false);
            return string4 != null && !string4.isBlank() ? Path.of(string4) : null;
        }
    }

    private void failFileLogin(Exception exception) {
        LOGGER.error("Microsoft file login failed", exception);
        this.error = "Microsoft file login failed";
        this.state = MicrosoftAuthenticator.State.ERROR;
    }

    private static JavaAuthManager loginWithRefreshToken(HttpClient httpClient, String string) throws IOException {
        MsaToken msaToken = httpClient.executeAndHandle(new MsaRefreshTokenRequest(APP_CONFIG, string));
        return JavaAuthManager.create(httpClient).msaApplicationConfig(APP_CONFIG).login(msaToken);
    }

    private static String readRefreshToken(Path path) throws IOException {
        if (Files.isRegularFile(path) && Files.size(path) <= 1000000L) {
            String string = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (string.startsWith("\ufeff")) {
                string = string.substring(1).trim();
            }

            if (string.startsWith("{")) {
                try {
                    JsonObject jsonObject = JsonParser.parseString(string).getAsJsonObject();
                    if (jsonObject.has("refresh_token")) {
                        return validateRefreshToken(jsonObject.get("refresh_token").getAsString());
                    }

                    if (jsonObject.has("refreshToken")) {
                        return validateRefreshToken(jsonObject.get("refreshToken").getAsString());
                    }
                } catch (RuntimeException exception) {
                }

                return null;
            } else if (string.indexOf(10) < 0 && string.indexOf(13) < 0 && string.indexOf(9) < 0) {
                String string2 = string;
                int i = string.indexOf(58);
                if (i > 0) {
                    string2 = string.substring(i + 1).trim();
                }

                if (string2.startsWith("refresh_token=")) {
                    string2 = string2.substring("refresh_token=".length()).trim();
                }

                return validateRefreshToken(string2);
            } else {
                return null;
            }
        } else {
            throw new IOException("Invalid Microsoft auth file");
        }
    }

    private static String validateRefreshToken(String string) {
        if (string == null) {
            return null;
        } else {
            String string2 = string.trim();
            boolean flag = string2.length() >= 32 && string2.length() <= 8192;
            if (!string2.chars().noneMatch(Character::isWhitespace) || string2.indexOf(61) >= 0 || string2.indexOf(59) >= 0) {
                return null;
            } else {
                return flag ? string2 : null;
            }
        }
    }

    private static JavaAuthManager loginWithCookies(HttpClient httpClient, List<MicrosoftAuthenticator.Cookie> cookies) throws IOException, InterruptedException {
        java.net.http.HttpClient httpClient2 = java.net.http.HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .cookieHandler(createCookieManager(cookies))
            .followRedirects(Redirect.NEVER)
            .build();
        String string = fetchAuthorizationCode(httpClient2);
        MsaToken msaToken = exchangeCode(httpClient2, string);
        return JavaAuthManager.create(httpClient).login(msaToken);
    }

    private static List<MicrosoftAuthenticator.Cookie> readCookies(Path path) throws IOException {
        if (Files.isRegularFile(path) && Files.size(path) <= 1000000L) {
            return parseCookies(Files.readAllLines(path, StandardCharsets.UTF_8));
        } else {
            throw new IOException("Invalid cookie file");
        }
    }

    private static List<MicrosoftAuthenticator.Cookie> parseCookies(List<String> strings) throws IOException {
        ArrayList arrayList = new ArrayList();
        long l = Instant.now().getEpochSecond();

        for (String string : strings) {
            MicrosoftAuthenticator.Cookie cookie = parseNetscapeCookie(string, l);
            if (cookie != null) {
                arrayList.add(cookie);
            }
        }

        if (arrayList.isEmpty()) {
            for (String string2 : strings) {
                MicrosoftAuthenticator.Cookie cookie2 = parseSimpleCookie(string2);
                if (cookie2 != null) {
                    arrayList.add(cookie2);
                }
            }
        }

        if (arrayList.isEmpty()) {
            throw new IOException("No supported Microsoft cookies found");
        } else {
            return arrayList;
        }
    }

    private static MicrosoftAuthenticator.Cookie parseSimpleCookie(String string) {
        if (string != null && !string.isBlank()) {
            String string2 = string.trim();
            if (!string2.startsWith("#") && !string2.startsWith("//")) {
                if (string2.startsWith("http")) {
                    return null;
                }

                int i = string2.indexOf(61);
                if (i <= 0) {
                    return null;
                }

                String string3 = string2.substring(0, i).trim();
                String string4 = string2.substring(i + 1).trim();
                if (string4.endsWith(";")) {
                    string4 = string4.substring(0, string4.length() - 1).trim();
                }

                if (!string3.isEmpty() && !string4.isEmpty()) {
                    if (string3.contains(" ")) {
                        return null;
                    }

                    return new MicrosoftAuthenticator.Cookie("login.live.com", true, "/", true, 0L, string3, string4);
                }
            }
        }

        return null;
    }

    private static MicrosoftAuthenticator.Cookie parseNetscapeCookie(String string, long l) {
        if (string != null && !string.isBlank()) {
            String string2 = string.trim();
            if (string2.startsWith("#HttpOnly_")) {
                string2 = string2.substring("#HttpOnly_".length());
            } else if (string2.startsWith("#")) {
                return null;
            }

            String[] strings = string2.split("\\t", -1);
            if (strings.length < 7) {
                strings = string2.split("\\s+", 7);
            }

            if (strings.length >= 7) {
                if (!isMicrosoftDomain(strings[0])) {
                    return null;
                }

                long l2 = parseLong(strings[4]);
                if (l2 > 0L && l2 < l) {
                    return null;
                }

                String string3 = strings[5].trim();
                String string4 = strings[6].trim();
                if (isValidCookieName(string3) && string4.indexOf(59) < 0 && string4.indexOf(13) < 0) {
                    if (string4.indexOf(10) >= 0) {
                        return null;
                    }

                    String string5 = normalizeDomain(strings[0]);
                    String string6 = strings[2].isBlank() ? "/" : strings[2];
                    return new MicrosoftAuthenticator.Cookie(string5, "TRUE".equalsIgnoreCase(strings[1]), string6, "TRUE".equalsIgnoreCase(strings[3]), l2, string3, string4);
                }
            }
        }

        return null;
    }

    private static long parseLong(String string) {
        try {
            return Long.parseLong(string.trim());
        } catch (NumberFormatException numberFormatException) {
            return 0L;
        }
    }

    private static boolean isMicrosoftDomain(String string) {
        String string2 = normalizeDomain(string);
        return string2.equals("live.com") || string2.endsWith(".live.com") || string2.equals("microsoftonline.com") || string2.endsWith(".microsoftonline.com");
    }

    private static String normalizeDomain(String string) {
        String string2 = string.trim().toLowerCase(Locale.ROOT);
        return string2.startsWith(".") ? string2.substring(1) : string2;
    }

    private static boolean isValidCookieName(String string) {
        int i = 0;

        while (i < string.length()) {
            char c = string.charAt(i);
            if (Character.isWhitespace(c)) {
                return false;
            }

            switch (c) {
                case ',':
                case ';':
                case '=':
                    return false;
                default:
                    i++;
            }
        }

        return true;
    }

    private static CookieManager createCookieManager(List<MicrosoftAuthenticator.Cookie> cookies) {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        long l = Instant.now().getEpochSecond();

        for (MicrosoftAuthenticator.Cookie cookie : cookies) {
            HttpCookie httpCookie = new HttpCookie(cookie.name(), cookie.value());
            httpCookie.setPath(cookie.path());
            httpCookie.setSecure(cookie.secure());
            httpCookie.setDomain(cookie.includeSubdomains() ? "." + cookie.domain() : cookie.domain());
            if (cookie.expiresAt() > 0L) {
                httpCookie.setMaxAge(Math.max(1L, cookie.expiresAt() - l));
            }

            cookieManager.getCookieStore().add(URI.create("https://" + cookie.domain() + cookie.path()), httpCookie);
        }

        return cookieManager;
    }

    private static String fetchAuthorizationCode(java.net.http.HttpClient httpClient) throws IOException, InterruptedException {
        URI uri = buildAuthorizeUri();

        for (int i = 0; i < 8; i++) {
            HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(uri))
                .timeout(TIMEOUT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
            HttpResponse httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
            String string = httpResponse.headers().firstValue("location").orElse(null);
            if (string == null) {
                throw new IOException("Microsoft did not return an authorization redirect");
            }

            URI uri2 = resolveRedirect(uri, string);
            if (!isMicrosoftHost(uri2.getHost())) {
                throw new IOException("Microsoft rejected the supplied cookies");
            }

            String string2 = getQueryParam(uri2, "code");
            if (string2 != null && !string2.isBlank()) {
                return string2;
            }

            if (getQueryParam(uri2, "error") != null) {
                throw new IOException("Microsoft rejected the supplied cookies");
            }

            uri = uri2;
        }

        throw new IOException("Too many Microsoft authorization redirects");
    }

    private static URI buildAuthorizeUri() {
        String string = APP_CONFIG.getEnvironment().getAuthorizeUrl();
        String string2 = "client_id="
            + urlEncode(APP_CONFIG.getClientId())
            + "&scope="
            + urlEncode(APP_CONFIG.getScope())
            + "&response_type=code&response_mode=query";
        return URI.create(string + "?" + string2);
    }

    private static URI resolveRedirect(URI uri, String string) throws IOException {
        try {
            return uri.resolve(new URI(string));
        } catch (URISyntaxException uriSyntaxException) {
            throw new IOException("Invalid Microsoft authorization redirect", uriSyntaxException);
        }
    }

    private static boolean isMicrosoftHost(String string) {
        if (string == null) {
            return false;
        }

        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.equals("live.com") || string2.endsWith(".live.com") || string2.equals("microsoftonline.com") || string2.endsWith(".microsoftonline.com");
    }

    private static MsaToken exchangeCode(java.net.http.HttpClient httpClient, String string) throws IOException, InterruptedException {
        HashMap hashMap = new HashMap();
        hashMap.put("client_id", APP_CONFIG.getClientId());
        hashMap.put("scope", APP_CONFIG.getScope());
        hashMap.put("grant_type", "authorization_code");
        hashMap.put("code", string);
        HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(URI.create(MsaEnvironment.LIVE.getTokenUrl())))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(encodeForm(hashMap)))
            .build();
        HttpResponse httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (httpResponse.statusCode() != 200) {
            throw new IOException("Microsoft rejected the authorization code");
        }

        JsonObject jsonObject;
        try {
            jsonObject = JsonParser.parseString((String)httpResponse.body()).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid Microsoft token response", exception);
        }

        String string2 = jsonObject.has("access_token") ? jsonObject.get("access_token").getAsString() : "";
        if (string2.isBlank()) {
            throw new IOException("Microsoft did not return an access token");
        }

        String string3 = jsonObject.has("refresh_token") ? jsonObject.get("refresh_token").getAsString() : null;
        long l = jsonObject.has("expires_in") ? jsonObject.get("expires_in").getAsLong() : 3600L;
        return new MsaToken(System.currentTimeMillis() + Math.max(l, 1L) * 1000L, string2, string3);
    }

    private static String encodeForm(Map<String, String> map) {
        StringBuilder builder = new StringBuilder();

        for (Entry entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }

            builder.append(urlEncode((String)entry.getKey())).append('=').append(urlEncode((String)entry.getValue()));
        }

        return builder.toString();
    }

    private static String urlEncode(String string) {
        return URLEncoder.encode(string, StandardCharsets.UTF_8);
    }

    private static String getQueryParam(URI uri, String string) {
        String string2 = uri.getRawQuery();
        if (string2 == null) {
            return null;
        }

        for (String string3 : string2.split("&")) {
            int i = string3.indexOf(61);
            String string4 = i < 0 ? string3 : string3.substring(0, i);
            if (string.equals(urlDecode(string4))) {
                if (i < 0) {
                    return urlDecode("");
                }

                return urlDecode(string3.substring(i + 1));
            }
        }

        return null;
    }

    private static String urlDecode(String string) {
        return URLDecoder.decode(string, StandardCharsets.UTF_8);
    }

    public Account takeResult() {
        Account account = this.result;
        this.result = null;
        if (account != null) {
            this.state = MicrosoftAuthenticator.State.IDLE;
        }

        return account;
    }

    public void reset() {
        if (!this.isBusy()) {
            this.state = MicrosoftAuthenticator.State.IDLE;
            this.error = "";
            this.userCode = "";
            this.verificationUri = "";
            this.directVerificationUri = "";
        }
    }

    public void cancel() {
        if (this.thread != null) {
            this.thread.interrupt();
        }

        this.state = MicrosoftAuthenticator.State.IDLE;
    }

    public static boolean refresh(Account account) {
        if (account != null && account.getMsAuth() != null) {
            try {
                HttpClient httpClient = MinecraftAuth.createHttpClient();
                JsonObject jsonObject = JsonParser.parseString(account.getMsAuth()).getAsJsonObject();
                JavaAuthManager javaAuthManager = JavaAuthManager.fromJson(httpClient, jsonObject);
                MinecraftProfile minecraftProfile = javaAuthManager.getMinecraftProfile().getUpToDate();
                account.setUsername(minecraftProfile.getName());
                account.setUuid(minecraftProfile.getId().toString());
                account.setAccessToken(javaAuthManager.getMinecraftToken().getUpToDate().getToken());
                account.setMsAuth(JavaAuthManager.toJson(javaAuthManager).toString());
                return true;
            } catch (Exception exception) {
                LOGGER.error("Microsoft token refresh failed", exception);
                return false;
            }
        } else {
            return false;
        }
    }

    record Cookie(String domain, boolean includeSubdomains, String path, boolean secure, long expiresAt, String name, String value) {
    }

    public enum State {
        IDLE,
        SELECTING_FILE,
        REQUESTING,
        AUTHENTICATING_COOKIE,
        AUTHENTICATING_REFRESH_TOKEN,
        AWAITING_USER,
        SUCCESS,
        ERROR;
    }
}
