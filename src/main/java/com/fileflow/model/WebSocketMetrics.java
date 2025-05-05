package com.fileflow.model;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entity representing WebSocket metrics
 */
@Entity
@Table(name = "websocket_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "hour_of_day", nullable = false)
    private int hourOfDay;

    @Column(name = "active_connections", nullable = false)
    private int activeConnections = 0;

    @Column(name = "messages_sent", nullable = false)
    private int messagesSent = 0;

    @Column(name = "messages_received", nullable = false)
    private int messagesReceived = 0;

    @Column(name = "errors_count", nullable = false)
    private int errorsCount = 0;

    @Column(name = "average_message_size")
    private Integer averageMessageSize;

    /**
     * Increment the number of messages sent
     */
    public void incrementMessagesSent(int size) {
        this.messagesSent++;
        updateAverageMessageSize(size);
    }

    /**
     * Increment the number of messages received
     */
    public void incrementMessagesReceived(int size) {
        this.messagesReceived++;
        updateAverageMessageSize(size);
    }

    /**
     * Increment the number of errors
     */
    public void incrementErrorsCount() {
        this.errorsCount++;
    }

    /**
     * Update the average message size
     */
    private void updateAverageMessageSize(int size) {
        int totalMessages = this.messagesSent + this.messagesReceived;
        if (this.averageMessageSize == null) {
            this.averageMessageSize = size;
        } else {
            // Calculate new average: (oldAvg * (n-1) + newValue) / n
            this.averageMessageSize = (this.averageMessageSize * (totalMessages - 1) + size) / totalMessages;
        }
    }
}