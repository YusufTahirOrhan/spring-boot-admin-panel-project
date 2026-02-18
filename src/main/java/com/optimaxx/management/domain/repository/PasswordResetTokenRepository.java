package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.PasswordResetToken;
import com.optimaxx.management.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(String tokenHash, Instant now);

    List<PasswordResetToken> findByUserAndUsedFalseAndExpiresAtAfter(User user, Instant now);
}
