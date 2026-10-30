package tech.sadnes.client.network;

import tech.sadnes.client.network.packet.UserRole;

public record UserProfile(int uid, String username, UserRole role, String hwid, String subscriptionEndDate, String avatarUrl) {
    public static UserProfile of(int i, String string, UserRole userRole) {
        return new UserProfile(i, string, userRole, "", "", "");
    }
}
