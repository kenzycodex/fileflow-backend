package com.fileflow.repository;

import com.fileflow.model.SyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueue, Long> {
    // Find all sync queue items by userId
    List<SyncQueue> findByUserId(Long userId);

    // Find all sync queue items by deviceId
    List<SyncQueue> findByDeviceId(Long deviceId);

    // Find all sync queue items by deviceId and status
    List<SyncQueue> findByDeviceIdAndStatus(Long deviceId, SyncQueue.Status status);

    // Find all sync queue items by userId and status
    List<SyncQueue> findByUserIdAndStatus(Long userId, SyncQueue.Status status);

    // Find all sync queue items with specific statuses and processed before a certain time
    List<SyncQueue> findByStatusInAndProcessedAtBefore(List<SyncQueue.Status> statuses, LocalDateTime dateTime);
}