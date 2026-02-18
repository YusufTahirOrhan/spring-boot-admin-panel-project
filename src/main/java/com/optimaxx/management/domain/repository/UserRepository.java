package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndDeletedFalse(String username);
}
