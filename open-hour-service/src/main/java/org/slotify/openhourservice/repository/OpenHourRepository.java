package org.slotify.openhourservice.repository;

import org.slotify.openhourservice.entity.OpenHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpenHourRepository extends JpaRepository<OpenHour, UUID> {
    Optional<List<OpenHour>> findOpenHoursByCoachId(UUID coachId);

    @Transactional
    void deleteOpenHoursByCoachId(UUID coachId);

}
