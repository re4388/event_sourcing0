package com.ben.es0.eventstore;

import com.ben.es0.domain.events.AccountEvent;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

public interface EventStore {
    // save events
    void save(String aggregateId, List<AccountEvent> events, int expectedVersion);

    // Load events for an aggregate
    List<AccountEvent> getEventsForAggregate(String aggregateId);

    // Get the last event to check version
    Optional<AccountEvent> getLastEventForAggregate(String aggregateId);

    @Transactional
    Optional<StoredEvent> getLastStoredEventForAggregate(String aggregateId);
}