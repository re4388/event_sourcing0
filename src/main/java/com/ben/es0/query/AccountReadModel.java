package com.ben.es0.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class AccountReadModel {
    @Id
    private String accountId;
    private double balance;
    private int version; // Keep track of the version processed

    public AccountReadModel(String accountId, double balance, int version) {
        this.accountId = accountId;
        this.balance = balance;
        this.version = version;
    }
}