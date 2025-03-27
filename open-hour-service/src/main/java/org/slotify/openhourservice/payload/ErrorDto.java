package org.slotify.openhourservice.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
@Schema(
        name = "Error"
)
public class ErrorDto {

        @Schema(
                description = "When the error occurs"
        )
        private LocalDateTime timestamp;
        @Schema(
                description = "API path invoked by client"
        )
        private String uri;

        @Schema(
                description = "Error message"
        )
        private String message;
}
