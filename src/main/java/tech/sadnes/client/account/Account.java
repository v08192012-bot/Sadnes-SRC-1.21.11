package tech.sadnes.client.account;


public final class Account {
    private final Account.Type type;
    private String username;
    private String uuid;
    private String accessToken;
    private String msAuth;
    private boolean favorite;
    private final boolean cookieLogin;
    private final boolean refreshTokenLogin;

    public Account(Account.Type type2, String string, String string2, String string3, String string4) {
        this(type2, string, string2, string3, string4, false, false);
    }

    public Account(Account.Type type2, String string, String string2, String string3, String string4, boolean flag) {
        this(type2, string, string2, string3, string4, flag, false);
    }

    public Account(Account.Type type2, String string, String string2, String string3, String string4, boolean flag, boolean flag2) {
        this.type = type2;
        this.username = string;
        this.uuid = string2;
        this.accessToken = string3;
        this.msAuth = string4;
        this.cookieLogin = flag;
        this.refreshTokenLogin = flag2;
    }

    public static Account offline(String string) {
        Account.Type type2 = Account.Type.OFFLINE;
        String string2 = "0";
        return new Account(type2, string, null, string2, null);
    }

    public boolean isMicrosoft() {
        Account.Type type2 = this.type;
        return Account.Type.MICROSOFT == type2;
    }

    public boolean isCookieAccount() {
        boolean flag = this.isMicrosoft();
        boolean flag2 = this.cookieLogin;
        return flag && flag2;
    }

    public boolean isRefreshTokenAccount() {
        boolean flag = this.isMicrosoft();
        boolean flag2 = this.refreshTokenLogin;
        return flag && flag2;
    }

    public Account.Type getType() {
        return this.type;
    }

    public String getUsername() {
        return this.username;
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getMsAuth() {
        return this.msAuth;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public boolean isCookieLogin() {
        return this.cookieLogin;
    }

    public boolean isRefreshTokenLogin() {
        return this.refreshTokenLogin;
    }

    public void setUsername(String string) {
        this.username = string;
    }

    public void setUuid(String string) {
        this.uuid = string;
    }

    public void setAccessToken(String string) {
        this.accessToken = string;
    }

    public void setMsAuth(String string) {
        this.msAuth = string;
    }

    public void setFavorite(boolean flag) {
        this.favorite = flag;
    }

    public enum Type {
        OFFLINE,
        MICROSOFT;
    }
}
