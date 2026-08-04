package com.neueda.domain;

/**
 * Bank account status enumeration.
 * Supports the account lifecycle states required by the platform.
 */
public enum AccountStatus {
    ACTIVE("Active"),
    PASSIVE("Passive"),
    DORMANT("Dormant"),
    SUSPICIOUS("Suspicious");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

