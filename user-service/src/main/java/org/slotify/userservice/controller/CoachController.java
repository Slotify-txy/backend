package org.slotify.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slotify.userservice.entity.user.Coach;
import org.slotify.userservice.entity.user.Student;
import org.slotify.userservice.payload.ErrorDto;
import org.slotify.userservice.payload.user.CoachDto;
import org.slotify.userservice.service.impl.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/coach", produces = {MediaType.APPLICATION_JSON_VALUE})
@AllArgsConstructor
@Validated
@Tag(
        name = "CRUD for coach"
)
public class CoachController {
    private final ModelMapper modelMapper;
    private final UserService userService;

    @GetMapping("/{coachId}")
    @Operation(
            summary = "Get coach by coachId"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Coach fetched"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Coach not found"
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
    public ResponseEntity<CoachDto> getCoach(@PathVariable UUID coachId) {
        Coach coach = userService.getCoachById(coachId);
        CoachDto coachDtoResponse = modelMapper.map(coach, CoachDto.class);
        return new ResponseEntity<>(coachDtoResponse, HttpStatus.OK);
    }

    @GetMapping("/student/{studentId}")
    @Operation(
            summary = "Get coaches by studentId"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Coaches fetched"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student not found"
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
    public ResponseEntity<List<CoachDto>> getCoaches(@PathVariable UUID studentId) {
        Student student = userService.getStudentById(studentId);
        List<CoachDto> coachDtos = student.getCoaches().stream().map(coach -> modelMapper.map(coach, CoachDto.class)).toList();
        return new ResponseEntity<>(coachDtos, HttpStatus.OK);
    }

    @PutMapping("/student/{studentId}")
    @Operation(
            summary = "Update coach info"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Coach info updated"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Coach not found"
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
    public ResponseEntity<CoachDto> updateCoach(@PathVariable UUID studentId, @RequestBody CoachDto coachDto) {
        Coach coach = modelMapper.map(coachDto, Coach.class);
        Coach updatedCoach = userService.updateCoachById(studentId, coach);
        CoachDto coachDtoResponse = modelMapper.map(updatedCoach, CoachDto.class);
        return new ResponseEntity<>(coachDtoResponse, HttpStatus.OK);
    }

    @PutMapping("/{coachId}/student")
    @Operation(
            summary = "Delete students from coach"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Students deleted"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Coach not found/Students not found"
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
    public ResponseEntity<CoachDto> deleteStudentsFromCoach(@PathVariable UUID coachId, @RequestBody List<UUID> studentIds) {
        Coach updatedCoach = userService.deleteStudentsFromCoach(coachId, studentIds);
        CoachDto coachDtoResponse = modelMapper.map(updatedCoach, CoachDto.class);
        return new ResponseEntity<>(coachDtoResponse, HttpStatus.OK);
    }


}
