package org.slotify.userservice.payload.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "User"
)
public class UserDto {
    private UUID id;

    @Schema(
            description = "User's name",
            example = "Xiyuan Tu"
    )
    @NotNull()
    private String name;

    @Schema(
            description = "User's email",
            example = "xiyuan.tyler@gmail.com"
    )
    @NotNull()
    @Email
    private String email;

    @Schema(
            description = "User's avatar url",
            example = "https://lh3.googleusercontent.com/a/ACg8ocLJ3jmpfPUrzU7DtF_JfOXEaWuCD9PQRdwrMZ54SxUwV9xF3g=s96-c"
    )
    @NotNull()
    private String picture;
}