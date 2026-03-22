package com.fraud_detection.notification.repository;

import com.fraud_detection.notification.entity.UserDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface NotificationRepository extends JpaRepository<UserDetailsEntity, String> {
    // Custom query methods can be defined here if needed
    // For example, to find a user by their email ID or account number
    UserDetailsEntity findById(Long id);

}
