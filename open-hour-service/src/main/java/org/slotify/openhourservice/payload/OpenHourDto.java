package org.slotify.openhourservice.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "Coaches' available hours"
)
public class OpenHourDto {
    private UUID id;

    @Schema(
            description = "Coach's id in User table",
            example = "324243252"
    )
    private UUID coachId;

    @Schema(
            description = "The start of the slot",
            example = "2024-02-01T15:09:00.9920024"
    )
    @NotNull(message = "StartAt can't be empty")
//    @FutureOrPresent // It uses UTC time rather than the local time.
    private LocalDateTime startAt;

    @Schema(
            description = "The end of the slot",
            example = "2024-02-01T15:09:00.9920024"
    )
    @NotNull(message = "EndAt can't be empty")
    private LocalDateTime endAt;
}
