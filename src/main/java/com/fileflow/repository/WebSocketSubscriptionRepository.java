package com.fileflow.repository;

import com.fileflow.model.User;
import com.fileflow.model.WebSocketSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WebSocket subscriptions
 */
@Repository
public interface WebSocketSubscriptionRepository extends JpaRepository<WebSocketSubscription, Long> {

    /**
     * Find subscription by user, item ID, and item type
     */
    Optional<WebSocketSubscription> findByUserAndItemIdAndItemType(
            User user, Long itemId, WebSocketSubscription.ItemType itemType);

    /**
     * Find subscription by user ID, item ID, and item type
     */
    Optional<WebSocketSubscription> findByUser_IdAndItemIdAndItemType(
            Long userId, Long itemId, WebSocketSubscription.ItemType itemType);

    /**
     * Find all active subscriptions for a user
     */
    List<WebSocketSubscription> findByUserAndIsActiveTrue(User user);

    /**
     * Find all active subscriptions by user ID
     */
    List<WebSocketSubscription> findByUser_IdAndIsActiveTrue(Long userId);

    /**
     * Find all active file subscriptions for a user
     */
    List<WebSocketSubscription> findByUserAndItemTypeAndIsActiveTrue(
            User user, WebSocketSubscription.ItemType itemType);

    /**
     * Find all active subscriptions for an item
     */
    List<WebSocketSubscription> findByItemIdAndItemTypeAndIsActiveTrue(
            Long itemId, WebSocketSubscription.ItemType itemType);

    /**
     * Deactivate all subscriptions for a user
     */
    @Modifying
    @Query("UPDATE WebSocketSubscription s SET s.isActive = false, s.updatedAt = :timestamp WHERE s.user.id = :userId")
    void deactivateAllForUser(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Deactivate a specific subscription
     */
    @Modifying
    @Query("UPDATE WebSocketSubscription s SET s.isActive = false, s.updatedAt = :timestamp " +
            "WHERE s.user.id = :userId AND s.itemId = :itemId AND s.itemType = :itemType")
    void deactivateSubscription(
            @Param("userId") Long userId,
            @Param("itemId") Long itemId,
            @Param("itemType") WebSocketSubscription.ItemType itemType,
            @Param("timestamp") LocalDateTime timestamp);

    /**
     * Delete inactive subscriptions older than a given time
     */
    void deleteByIsActiveFalseAndUpdatedAtBefore(LocalDateTime timestamp);
}