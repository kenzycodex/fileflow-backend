package com.fileflow.repository;

import com.fileflow.model.Share;
import com.fileflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {
    List<Share> findByOwner(User owner);

    Page<Share> findByOwner(User owner, Pageable pageable);

    List<Share> findByRecipient(User recipient);

    Page<Share> findByRecipient(User recipient, Pageable pageable);

    List<Share> findByRecipientEmail(String recipientEmail);

    Optional<Share> findByShareLink(String shareLink);

    @Query("SELECT s FROM Share s WHERE s.owner = :user OR s.recipient = :user")
    Page<Share> findAllUserShares(@Param("user") User user, Pageable pageable);

    List<Share> findByItemIdAndItemType(Long itemId, Share.ItemType itemType);

    List<Share> findByExpiryDateBefore(LocalDateTime expiryDate);

    @Query("SELECT s FROM Share s WHERE s.owner = :owner AND s.itemId = :itemId AND s.itemType = :itemType")
    Optional<Share> findByOwnerAndItemIdAndItemType(
            @Param("owner") User owner,
            @Param("itemId") Long itemId,
            @Param("itemType") Share.ItemType itemType);

    @Query("SELECT s FROM Share s WHERE s.owner = :user AND s.createdAt > :date")
    List<Share> findByOwnerAndCreatedAtAfter(@Param("user") User user, @Param("date") LocalDateTime date);
}