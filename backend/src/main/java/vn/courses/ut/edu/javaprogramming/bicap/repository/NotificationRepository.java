package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * C-3 fix: the guest-visible feed. Only platform-wide announcements flagged as
     * system notifications are returned — never other users' private notifications
     * (the previous implementation returned {@code findAll()} for unauthenticated
     * callers, leaking farm/retailer conversations).
     */
    List<Notification> findBySystemTrueOrderByCreatedAtDesc();

    long countByUserIdAndIsReadFalse(Long userId);

    /** Marks every unread notification of a user as read. Returns the number updated. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}