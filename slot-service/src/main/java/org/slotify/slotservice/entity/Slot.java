package org.slotify.slotservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.slotify.slotservice.constant.SlotStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot extends BaseEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.UUID
    )
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "class_id", nullable = false)
    private UUID classId = UUID.randomUUID();

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SlotStatus status;

    @Column(name = "coach_deleted", nullable = false)
    private Boolean coachDeleted;

    @Column(name = "student_deleted", nullable = false)
    private Boolean studentDeleted;
}
