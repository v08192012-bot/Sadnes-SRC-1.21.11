package tech.sadnes.client.feature;

import ua.mintantileak.profile.Role;

public enum BetaScope {
    ALL(Role.DEFAULT),
    ONLY_BETA(Role.BETA),
    ONLY_ADMIN(Role.ALPHA);

    private final Role requiredRole;

    BetaScope(Role role) {
        this.requiredRole = role;
    }

    public boolean isAllowed(Role role) {
        return role != null && role.ordinal() >= this.requiredRole.ordinal();
    }
}
