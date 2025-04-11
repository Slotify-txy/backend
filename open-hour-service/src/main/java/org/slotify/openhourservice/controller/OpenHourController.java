package org.slotify.openhourservice.controller;

import com.google.protobuf.InvalidProtocolBufferException;
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
import org.slotify.openhourservice.entity.OpenHour;
import org.slotify.openhourservice.payload.ErrorDto;
import org.slotify.openhourservice.payload.OpenHourDto;
import org.slotify.openhourservice.service.OpenHourService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/open-hour", produces = {MediaType.APPLICATION_JSON_VALUE})
@RequiredArgsConstructor
@Validated
@Tag(
        name = "CRUD for open hours"
)
public class OpenHourController {
    private static final Logger log = LoggerFactory.getLogger(OpenHourController.class);
    private final OpenHourService openHourService;
    private final ModelMapper modelMapper;

    @GetMapping("/coach/{coachId}")
    @Operation(
            summary = "Get all the open hours for some coach"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Open hours fetched"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Open hours not found"
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
    public ResponseEntity<List<OpenHourDto>> getOpenHoursByCoachId(@PathVariable UUID coachId) {
        List<OpenHour> openHours = openHourService.getOpenHoursByCoachId(coachId);
        List<OpenHourDto> openHourDtos = openHours.stream().map(openHour -> modelMapper.map(openHour, OpenHourDto.class)).toList();
        return new ResponseEntity<>(openHourDtos, HttpStatus.OK);
    }

    @PostMapping("/coach/{coachId}")
    @Operation(
            summary = "Create open hours"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Open hours created"
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
    public ResponseEntity<List<OpenHourDto>> createOpenHours(@PathVariable UUID coachId, @Valid @RequestBody List<OpenHourDto> openHourDtos) {
        log.info("Create open hours: {}", openHourDtos);
        List<OpenHour> openHours = openHourDtos.stream().map(openHourDto -> {
            openHourDto.setCoachId(coachId);
            return modelMapper.map(openHourDto, OpenHour.class);
        }).toList();
        List<OpenHour> savedOpenHours = openHourService.createOpenHours(coachId, openHours);
        List<OpenHourDto> openHourDtoResponse = savedOpenHours.stream().map(openHour -> modelMapper.map(openHour, OpenHourDto.class)).toList();
        return new ResponseEntity<>(openHourDtoResponse, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete an open hour"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Open hour deleted"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Open hour not found"
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
    public ResponseEntity<String> deleteOpenHourById(@PathVariable UUID id) {
        openHourService.deleteOpenHourById(id);
        return new ResponseEntity<>("Deleted open hour successfully", HttpStatus.OK);
    }

    @DeleteMapping("/coach/{coachId}")
    @Operation(
            summary = "Delete open hours by coachId"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Open hours deleted"
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
    public ResponseEntity<String> deleteOpenHoursByCoachId(@PathVariable UUID coachId) {
        openHourService.deleteOpenHoursByCoachId(coachId);
        return new ResponseEntity<>("Deleted open hours successfully", HttpStatus.OK);
    }
}
