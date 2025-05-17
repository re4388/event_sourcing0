package com.ben.es0.domain.events;

public class AccountCreatedEvent extends AccountEvent {
    private double initialBalance;

    // For Jackson deserialization
    public AccountCreatedEvent() {}

    public AccountCreatedEvent(String accountId, double initialBalance) {
        super(accountId);
        this.initialBalance = initialBalance;
    }

    public double getInitialBalance() {
        return initialBalance;
    }
}