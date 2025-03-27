package org.slotify.userservice.repository;

import org.slotify.userservice.entity.user.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoachRepository extends JpaRepository<Coach, UUID> {
    Optional<Coach> findByEmail(String email);

    Optional<Coach> findByInvitationCode(String invitationCode);
}
