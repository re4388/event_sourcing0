package com.ben.es0.eventstore;

import com.ben.es0.domain.events.AccountEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JpaEventStore implements EventStore {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public JpaEventStore() {
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper to handle polymorphism (deserialize correct event type)
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.eventsourcing.domain.events") // Allow specific package
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(new JavaTimeModule()); // Support Java 8 Date/Time
    }

    @Override
    @Transactional
    public void save(String aggregateId, List<AccountEvent> events, int expectedVersion) {
        // Basic concurrency check (optimistic concurrency)
        // In a real system, you'd likely use a version column and check/increment it atomically
        // or use database-specific locking/versioning features.
        // This simple example fetches the last event's version.
        Optional<StoredEvent> lastEvent = getLastStoredEventForAggregate(aggregateId);
        int currentVersion = lastEvent.map(StoredEvent::getVersion).orElse(0);

        if (currentVersion != expectedVersion) {
            throw new RuntimeException("Concurrency conflict: Expected version " + expectedVersion + ", but found " + currentVersion);
        }

        int nextVersion = currentVersion + 1;
        for (AccountEvent event : events) {
            try {
                String eventData = objectMapper.writeValueAsString(event);
                StoredEvent storedEvent = new StoredEvent(
                        aggregateId,
                        event.getTimestamp(),
                        event.getClass().getName(),
                        eventData,
                        nextVersion++ // Increment version for each new event saved
                );
                entityManager.persist(storedEvent);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing event", e);
            }
        }
    }

    @Override
    @Transactional
    public List<AccountEvent> getEventsForAggregate(String aggregateId) {
        List<StoredEvent> storedEvents = entityManager.createQuery(
                        "SELECT se FROM StoredEvent se WHERE se.aggregateId = :aggregateId ORDER BY se.timestamp ASC, se.id ASC", StoredEvent.class)
                .setParameter("aggregateId", aggregateId)
                .getResultList();

        return storedEvents.stream()
                .map(this::deserializeEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Optional<AccountEvent> getLastEventForAggregate(String aggregateId) {
        Optional<StoredEvent> lastStoredEvent = getLastStoredEventForAggregate(aggregateId);
        return lastStoredEvent.map(this::deserializeEvent);
    }

    @Override
    @Transactional
    public Optional<StoredEvent> getLastStoredEventForAggregate(String aggregateId) {
        // Use pessimistic lock to prevent concurrent reads/writes during version check (simplified)
        // In a real system, optimistic locking with version column is more common for writes.
        List<StoredEvent> storedEvents = entityManager.createQuery(
                        "SELECT se FROM StoredEvent se WHERE se.aggregateId = :aggregateId ORDER BY se.version DESC", StoredEvent.class)
                .setParameter("aggregateId", aggregateId)
                .setMaxResults(1)
                // .setLockMode(LockModeType.PESSIMISTIC_WRITE) // Consider locking in real write scenarios
                .getResultList();

        return storedEvents.stream().findFirst();
    }


    private AccountEvent deserializeEvent(StoredEvent storedEvent) {
        try {
            Class<?> eventClass = Class.forName(storedEvent.getEventType());
            return (AccountEvent) objectMapper.readValue(storedEvent.getEventData(), eventClass);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error deserializing event", e);
        }
    }
}