package org.slotify.slotservice.repository;

import org.slotify.slotservice.constant.SlotStatus;
import org.slotify.slotservice.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlotRepository extends JpaRepository<Slot, UUID> {

    Optional<List<Slot>> findSlotsByStudentIdAndCoachId(UUID studentId, UUID coachId);

    Optional<List<Slot>> findSlotsByCoachId(UUID coachId);

    void deleteSlotsByStudentIdAndCoachIdAndStatus(UUID studentId, UUID coachId, SlotStatus status);

    @Query("""
                SELECT s.studentId, COUNT(DISTINCT s.classId)
                FROM Slot s
                WHERE s.coachId = :coachId
                AND s.startAt >= CURRENT_TIMESTAMP
                AND NOT EXISTS (
                  SELECT 1
                  FROM Slot s2
                  WHERE s2.classId = s.classId
                    AND (s2.status = 'PENDING' or s2.status = 'APPOINTMENT')
                )
                GROUP BY s.studentId
            """)
    Optional<List<Object[]>> findAvailableStudents(
            @Param("coachId") UUID coachId
    );

    void deleteSlotsByEndAtBefore(LocalDateTime cutoffDate);
}
