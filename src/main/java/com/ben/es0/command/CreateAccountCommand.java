package com.ben.es0.command;

import lombok.Data; // Using Lombok for simplicity

@Data
public class CreateAccountCommand {
    private String accountId;
    private double initialBalance;
}