package org.slotify.openhourservice.exception;

import org.slotify.openhourservice.payload.ErrorDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        List<ObjectError> validationErrorList = ex.getBindingResult().getAllErrors();

        validationErrorList.forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String validationMsg = error.getDefaultMessage();
            validationErrors.put(fieldName, validationMsg);
        });
        return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleResourceNotFoundException(ResourceNotFoundException resourceNotFoundException, WebRequest webRequest) {
        ErrorDto ErrorDto = new ErrorDto(LocalDateTime.now(), webRequest.getDescription(false), resourceNotFoundException.getMessage());
        return new ResponseEntity<>(ErrorDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SlotStatusStaleException.class)
    public ResponseEntity<ErrorDto> handleSlotStatusStaleException(SlotStatusStaleException slotStatusStaleException, WebRequest webRequest) {
        ErrorDto ErrorDto = new ErrorDto(LocalDateTime.now(), webRequest.getDescription(false), slotStatusStaleException.getMessage());
        return new ResponseEntity<>(ErrorDto, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CoachAlreadyAddedException.class)
    public ResponseEntity<ErrorDto> handleCoachAlreadyAddedException(CoachAlreadyAddedException coachAlreadyAddedException, WebRequest webRequest) {
        ErrorDto ErrorDto = new ErrorDto(LocalDateTime.now(), webRequest.getDescription(false), coachAlreadyAddedException.getMessage());
        return new ResponseEntity<>(ErrorDto, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleRuntimeException(Exception exception, WebRequest webRequest) {
        ErrorDto errorDto = new ErrorDto(LocalDateTime.now(),  webRequest.getDescription(false), exception.getMessage());
        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
