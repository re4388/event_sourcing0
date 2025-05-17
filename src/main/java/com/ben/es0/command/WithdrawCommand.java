package com.ben.es0.command;

import lombok.Data;
@Data
public class WithdrawCommand {
    private String accountId;
    private double amount;
}