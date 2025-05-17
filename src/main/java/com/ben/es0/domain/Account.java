package com.ben.es0.domain;

import com.ben.es0.domain.events.AccountCreatedEvent;
import com.ben.es0.domain.events.AccountEvent;
import com.ben.es0.domain.events.MoneyDepositedEvent;
import com.ben.es0.domain.events.MoneyWithdrawnEvent;

import java.util.ArrayList;
import java.util.List;

public class Account {
    private String accountId;
    private double balance;
    private int version; // Track version for concurrency (simplified)

    // Constructor to create a new account (from command)
    public Account(String accountId, double initialBalance) {
        // Validate command if needed
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        // Apply the creation event
        apply(new AccountCreatedEvent(accountId, initialBalance));
    }

    // Constructor to reconstruct state from events
    public Account(String accountId, List<AccountEvent> history) {
        this.accountId = accountId;
        this.balance = 0; // Start from initial state
        this.version = 0;
        // Replay events
        history.forEach(this::apply);
    }

    // apply 方法負責根據事件改變聚合的內部狀態。重建聚合時，只呼叫 apply 方法。
    // Apply events to change state
    private void apply(AccountEvent event) {
        if (event instanceof AccountCreatedEvent) {
            apply((AccountCreatedEvent) event);
        } else if (event instanceof MoneyDepositedEvent) {
            apply((MoneyDepositedEvent) event);
        } else if (event instanceof MoneyWithdrawnEvent) {
            apply((MoneyWithdrawnEvent) event);
        }
        this.version++; // Increment version after applying each event
    }

    private void apply(AccountCreatedEvent event) {
        this.accountId = event.getAccountId();
        this.balance = event.getInitialBalance();
    }

    private void apply(MoneyDepositedEvent event) {
        this.balance += event.getAmount();
    }

    private void apply(MoneyWithdrawnEvent event) {
        if (this.balance < event.getAmount()) {
            // This validation should ideally happen *before* generating the event
            // In a real system, command handling would check this.
            // For simplicity here, we handle it during apply, but it's less ideal.
            System.err.println("Warning: Attempted to withdraw more than balance. This event should not have been generated.");
            // Or throw an exception, but applying events shouldn't usually fail
        } else {
            this.balance -= event.getAmount();
        }
    }

    // 聚合的 handle 方法負責驗證命令並產生事件
    // --- Command Handling Methods (return events, don't change state directly) ---

    public MoneyDepositedEvent handle(double amount) { // Handles DepositCommand
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        return new MoneyDepositedEvent(this.accountId, amount);
    }

    public MoneyWithdrawnEvent handleWithdraw(double amount) { // Handles WithdrawCommand
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }
        if (this.balance < amount) {
            throw new IllegalStateException("Insufficient funds"); // Validate before generating event
        }
        return new MoneyWithdrawnEvent(this.accountId, amount);
    }


    // --- Getters for current state (for Read Model or Query Service) ---
    public String getAccountId() {
        return accountId;
    }

    public double getBalance() {
        return balance;
    }

    public int getVersion() {
        return version;
    }
}
