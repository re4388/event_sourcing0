package com.ben.es0.eventstore;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
public class StoredEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateId;
    private Instant timestamp;
    private String eventType; // Store the class name of the event
    @Lob // Use Lob for larger text data
    private String eventData; // Store event data as JSON string
    private int version; // Version of the aggregate after this event

    public StoredEvent(String aggregateId, Instant timestamp, String eventType, String eventData, int version) {
        this.aggregateId = aggregateId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.eventData = eventData;
        this.version = version;
    }
}
