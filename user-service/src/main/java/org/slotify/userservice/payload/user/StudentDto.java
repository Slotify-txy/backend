package org.slotify.userservice.payload.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Student"
)
public class StudentDto extends UserDto {
    private UUID defaultCoachId;
    private Set<UUID> coachIds;
    private Long numOfClassCanBeScheduled;
}
