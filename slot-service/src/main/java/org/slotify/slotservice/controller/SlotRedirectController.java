package org.slotify.slotservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slotify.slotservice.constant.SlotStatus;
import org.slotify.slotservice.payload.ErrorDto;
import org.slotify.slotservice.service.SlotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/api/v1/slot")
@AllArgsConstructor
@Validated
@Tag(
        name = "CRUD for time slots"
)
public class SlotRedirectController {
    private final SlotService slotService;

    @GetMapping("/{id}/token/{token}")
    @Operation(
            summary = "Update a time slot's status via email"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Time slot updated"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Time slot not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorDto.class)
                    )
            )
    }
    )
    public String updateSlotStatusViaEmail(@PathVariable UUID id, @PathVariable String token, @RequestParam SlotStatus status, Model model) {
        String message = slotService.updateSlotStatusViaEmail(id, token, status);
        if (message != null) {
            model.addAttribute("message", message);
            return "action-success";
        }
        return "token-invalid";
    }
}
