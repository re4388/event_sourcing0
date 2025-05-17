package com.ben.es0.query;

import com.ben.es0.domain.Account;
import com.ben.es0.domain.events.AccountEvent;
import com.ben.es0.eventstore.EventStore;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountQueryService {
    // 查詢服務主要從 AccountReadModel 獲取數據，因為它針對查詢進行了優化。
    // 重建聚合狀態的方法 (reconstructAccountFromEvents) 更多是用於內部驗證或特定情況。

    private final EventStore eventStore;
    private final AccountReadModelRepository readModelRepository; // Query from read model

    @Autowired
    public AccountQueryService(EventStore eventStore, AccountReadModelRepository readModelRepository) {
        this.eventStore = eventStore;
        this.readModelRepository = readModelRepository;
    }

    // Method to get current state from the Read Model (optimized for queries)
    @Transactional
    public Optional<AccountReadModel> getAccountState(String accountId) {
        return readModelRepository.findById(accountId);
    }

    // Method to reconstruct state from events (for debugging or specific needs)
    @Transactional
    public Optional<Account> reconstructAccountFromEvents(String accountId) {
        List<AccountEvent> history = eventStore.getEventsForAggregate(accountId);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        Account account = new Account(accountId, history);
        return Optional.of(account);
    }

    // Method to get all events for an aggregate
    @Transactional
    public List<AccountEvent> getEventsForAccount(String accountId) {
        return eventStore.getEventsForAggregate(accountId);
    }
}