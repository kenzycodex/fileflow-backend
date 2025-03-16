package com.fileflow.repository;

import com.fileflow.model.SyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueue, Long> {
    List<SyncQueue> findByUserId(Long userId);

    List<SyncQueue> findByDeviceId(Long deviceId);

    List<SyncQueue> findByDeviceIdAndStatus(Long deviceId, SyncQueue.Status status);

    List<SyncQueue> findByStatusInAndProcessedAtBefore(List<SyncQueue.Status> statuses, LocalDateTime dateTime);
}