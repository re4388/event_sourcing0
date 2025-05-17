package com.ben.es0.domain.events;

public class MoneyWithdrawnEvent extends AccountEvent {
    private double amount;

    // For Jackson deserialization
    public MoneyWithdrawnEvent() {}

    public MoneyWithdrawnEvent(String accountId, double amount) {
        super(accountId);
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }
}