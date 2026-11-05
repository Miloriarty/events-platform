package org.example.notification.repository;

import org.example.notification.model.NotificationsModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationsRepository extends JpaRepository<NotificationsModel, Long> {

    Page<NotificationsModel> findByUserId(Long userId, Pageable pageable);

    Optional<NotificationsModel> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    boolean existsByMessageId(String messageId);

    @Modifying
    @Query("update NotificationsModel n set n.read = true where n.userId = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId);
}
