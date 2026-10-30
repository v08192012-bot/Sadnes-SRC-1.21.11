package ua.mintantileak.profile;

public final class Profile {
    public static String username = "StalinNew";
    public static int uid = 1;
    public static Role role = Role.OWNER;
    public static String hwid = "a9fddefc337e2143ae2f4dee2fb8ae89";
    public static String subscriptionEndDate = "StalinNew";
    public static String avatarUrl = "file:///C:/Users/Elytr/IdeaProjects/sadnes21/run/sadnes/avatar.png";

    public static String getUsername() {
        return username;
    }

    public static int getUid() {
        return uid;
    }

    public static Role getRole() {
        return role;
    }

    public static String getHwid() {
        return hwid;
    }

    public static String getSubscriptionEndDate() {
        return subscriptionEndDate;
    }

    public static String getAvatarUrl() {
        return avatarUrl;
    }
}
