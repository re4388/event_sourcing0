package com.ben.es0.domain.events;

public class MoneyDepositedEvent extends AccountEvent {
    private double amount;

    // For Jackson deserialization
    public MoneyDepositedEvent() {}

    public MoneyDepositedEvent(String accountId, double amount) {
        super(accountId);
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }
}