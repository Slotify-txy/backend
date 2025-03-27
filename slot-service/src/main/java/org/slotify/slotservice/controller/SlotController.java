package org.slotify.slotservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.slotservice.constant.FrontendSource;
import org.slotify.slotservice.constant.SlotStatus;
import org.slotify.slotservice.entity.Slot;
import org.slotify.slotservice.exception.ResourceNotFoundException;
import org.slotify.slotservice.payload.ErrorDto;
import org.slotify.slotservice.payload.SlotDto;
import org.slotify.slotservice.service.SlotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/slot", produces = {MediaType.APPLICATION_JSON_VALUE})
@RequiredArgsConstructor
@Validated
@Tag(
        name = "CRUD for time slots"
)
public class SlotController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SlotService slotService;
    private final ModelMapper modelMapper;

    @Value("${client_id.student}")
    private String studentClientId;

    @Value("${client_id.coach}")
    private String coachClientId;

    @GetMapping("/student/{studentId}/coach/{coachId}")
    @Operation(
            summary = "Get all the time slots for some student and some coach"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Time slots fetched"
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
    public ResponseEntity<List<SlotDto>> getSlotsByStudentIdAndCoachId(@RequestHeader("X-Client-Id") String clientId, @PathVariable UUID studentId, @PathVariable UUID coachId) {

        if (!studentClientId.equals(clientId) && !coachClientId.equals(clientId)) {
            throw new ResourceNotFoundException("Request Source", "X-Client-Id", clientId);

        }
        FrontendSource source = studentClientId.equals(clientId) ? FrontendSource.STUDENT : FrontendSource.COACH;

        List<Slot> slots = slotService.getSlotsByStudentIdAndCoachId(studentId, coachId, source);
        List<SlotDto> slotDtos = slots.stream().map(slot -> modelMapper.map(slot, SlotDto.class)).toList();
        return new ResponseEntity<>(slotDtos, HttpStatus.OK);
    }

    @GetMapping("/coach/{coachId}")
    @Operation(
            summary = "Get all the time slots for some coach"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Time slots fetched"
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
    public ResponseEntity<List<SlotDto>> getSlotsByCoachId(@RequestHeader("X-Client-Id") String clientId, @PathVariable UUID coachId) {
        log.info("getSlotsByCoachId invoked, clientId: {}, coachId: {}", clientId, coachId);
        if (!studentClientId.equals(clientId) && !coachClientId.equals(clientId)) {
            throw new ResourceNotFoundException("Request Source", "X-Client-Id", clientId);
        }
        FrontendSource source = studentClientId.equals(clientId) ? FrontendSource.STUDENT : FrontendSource.COACH;
        List<Slot> slots = slotService.getSlotsByCoachId(coachId, source);
        List<SlotDto> slotDtos = slots.stream().map(slot -> modelMapper.map(slot, SlotDto.class)).toList();
        return new ResponseEntity<>(slotDtos, HttpStatus.OK);
    }

    @PostMapping("")
    @Operation(
            summary = "Create time slots"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Time slots created"
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
    public ResponseEntity<List<SlotDto>> createSlots(@Valid @RequestBody List<SlotDto> slotDtos) {
        List<Slot> slots = slotDtos.stream().map(slotDto -> modelMapper.map(slotDto, Slot.class)).toList();
        List<Slot> savedSlots = slotService.createSlots(slots);
        List<SlotDto> slotDtoResponse = savedSlots.stream().map(slot -> modelMapper.map(slot, SlotDto.class)).toList();
        return new ResponseEntity<>(slotDtoResponse, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a time slot"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Time slot deleted"
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
    public ResponseEntity<String> deleteSlotById(@RequestHeader("X-Client-Id") String clientId, @PathVariable UUID id) {
        if (!studentClientId.equals(clientId) && !coachClientId.equals(clientId)) {
            throw new ResourceNotFoundException("Request Source", "X-Client-Id", clientId);

        }
        FrontendSource source = studentClientId.equals(clientId) ? FrontendSource.STUDENT : FrontendSource.COACH;
        slotService.deleteSlotById(id, source);
        return new ResponseEntity<>("Deleted slot successfully", HttpStatus.OK);
    }


    @PutMapping("/{id}")
    @Operation(
            summary = "Update a time slot's status"
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
    public ResponseEntity<SlotDto> updateSlotStatus(@PathVariable UUID id, @RequestParam SlotStatus status) {
        Slot slot = slotService.updateSlotStatus(id, status);
        return new ResponseEntity<>(modelMapper.map(slot, SlotDto.class), HttpStatus.OK);
    }
}
