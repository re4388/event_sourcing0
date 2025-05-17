package com.ben.es0.commandhandling;

import com.ben.es0.command.CreateAccountCommand;
import com.ben.es0.command.DepositCommand;
import com.ben.es0.command.WithdrawCommand;
import com.ben.es0.domain.Account;
import com.ben.es0.domain.events.AccountCreatedEvent;
import com.ben.es0.domain.events.AccountEvent;
import com.ben.es0.eventstore.EventStore;
import com.ben.es0.query.AccountReadModel;
import com.ben.es0.query.AccountReadModelRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class AccountCommandHandler {
    // 在實際的 CQRS+Event Sourcing 系統中，Command Handler 通常只負責載入聚合、處理命令、儲存事件。
    // 更新 Read Model 的邏輯會由單獨的 Event Handler/Projector 服務來完成，它會訂閱事件流並異步處理。這裡為了簡化範例，將其放在同一個事務中。

    private final EventStore eventStore;
    private final AccountReadModelRepository readModelRepository; // Inject read model repository

    @Autowired
    public AccountCommandHandler(EventStore eventStore, AccountReadModelRepository readModelRepository) {
        this.eventStore = eventStore;
        this.readModelRepository = readModelRepository;
    }

    @Transactional // Ensure atomicity of saving events and updating read model (in this simplified setup)
    public void handle(CreateAccountCommand command) {
        // Check if account already exists (optional, depends on domain rules)
        List<AccountEvent> existingEvents = eventStore.getEventsForAggregate(command.getAccountId());
        if (!existingEvents.isEmpty()) {
            throw new IllegalArgumentException("Account with ID " + command.getAccountId() + " already exists.");
        }

        // Create the initial event
        AccountCreatedEvent event = new AccountCreatedEvent(command.getAccountId(), command.getInitialBalance());

        // Save the event (expected version 0 for new aggregate)
        eventStore.save(command.getAccountId(), Collections.singletonList(event), 0);

        // --- Update Read Model (Simplified Projector Logic) ---
        // In a real system, this might be async and listen to the event stream
        AccountReadModel readModel = new AccountReadModel(command.getAccountId(), command.getInitialBalance(), 1); // Version is 1 after creation
        readModelRepository.save(readModel);
    }

    /**
     * - 根據命令中的聚合 ID，從事件儲存中**載入**該聚合的歷史事件。
     * - 通過重播這些歷史事件來**重建**聚合的當前狀態。
     * - 將命令發送給重建好的聚合，讓聚合**處理**該命令。
     * - 聚合處理命令後會**產生**一個或多個新的事件。
     * - 將這些新事件**儲存**到事件儲存中（通常會進行並發檢查）。
     */
    @Transactional
    public void handle(DepositCommand command) {
        // Load aggregate from event store
        List<AccountEvent> history = eventStore.getEventsForAggregate(command.getAccountId());
        if (history.isEmpty()) {
            throw new IllegalArgumentException("Account with ID " + command.getAccountId() + " not found.");
        }
        Account account = new Account(command.getAccountId(), history);

        // Handle the command on the aggregate to get the resulting event
        AccountEvent newEvent = account.handle(command.getAmount()); // Account::handle(double) returns MoneyDepositedEvent

        // Save the new event (use current aggregate version as expected version)
        eventStore.save(command.getAccountId(), Collections.singletonList(newEvent), account.getVersion());

        // --- Update Read Model (Simplified Projector Logic) ---
        // In a real system, this might be async
        Optional<AccountReadModel> optionalReadModel = readModelRepository.findById(command.getAccountId());
        if (optionalReadModel.isPresent()) {
            AccountReadModel readModel = optionalReadModel.get();
            readModel.setBalance(readModel.getBalance() + command.getAmount());
            readModel.setVersion(account.getVersion() + 1); // Update version
            readModelRepository.save(readModel);
        } else {
            // This case should ideally not happen if creation was successful
            System.err.println("Warning: Read model not found for account " + command.getAccountId() + " during deposit.");
        }
    }

    @Transactional
    public void handle(WithdrawCommand command) {
        // Load aggregate from event store
        List<AccountEvent> history = eventStore.getEventsForAggregate(command.getAccountId());
        if (history.isEmpty()) {
            throw new IllegalArgumentException("Account with ID " + command.getAccountId() + " not found.");
        }
        Account account = new Account(command.getAccountId(), history);

        // Handle the command on the aggregate to get the resulting event
        // This will throw InsufficientFundsException if balance is too low
        AccountEvent newEvent = account.handleWithdraw(command.getAmount()); // Account::handleWithdraw(double) returns MoneyWithdrawnEvent

        // Save the new event (use current aggregate version as expected version)
        eventStore.save(command.getAccountId(), Collections.singletonList(newEvent), account.getVersion());

        // --- Update Read Model (Simplified Projector Logic) ---
        // In a real system, this might be async
        Optional<AccountReadModel> optionalReadModel = readModelRepository.findById(command.getAccountId());
        if (optionalReadModel.isPresent()) {
            AccountReadModel readModel = optionalReadModel.get();
            readModel.setBalance(readModel.getBalance() - command.getAmount());
            readModel.setVersion(account.getVersion() + 1); // Update version
            readModelRepository.save(readModel);
        } else {
            System.err.println("Warning: Read model not found for account " + command.getAccountId() + " during withdrawal.");
        }
    }
}