package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.RefreshToken;
import com.optimaxx.management.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(String tokenHash, Instant now);

    List<RefreshToken> findByUserAndRevokedFalseAndExpiresAtAfter(User user, Instant now);

    List<RefreshToken> findByUserAndDeviceIdAndRevokedFalseAndExpiresAtAfter(User user, String deviceId, Instant now);
}
