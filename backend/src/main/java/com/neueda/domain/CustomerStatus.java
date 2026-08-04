package com.neueda.domain;

/**
 * Customer profile status enumeration.
 * A customer profile can be either active or passive.
 */
public enum CustomerStatus {
    ACTIVE("Active"),
    PASSIVE("Passive");

    private final String displayName;

    CustomerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

