package com.ben.es0.domain.events;

import java.time.Instant;

public abstract class AccountEvent {
    private String accountId;
    private Instant timestamp;

    // For Jackson deserialization
    public AccountEvent() {}

    public AccountEvent(String accountId) {
        this.accountId = accountId;
        this.timestamp = Instant.now();
    }

    public String getAccountId() {
        return accountId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    // Getters for specific event data will be in subclasses
}