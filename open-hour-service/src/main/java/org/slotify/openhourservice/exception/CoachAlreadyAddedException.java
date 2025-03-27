package org.slotify.openhourservice.exception;

import lombok.Getter;

@Getter
public class CoachAlreadyAddedException extends RuntimeException {

    private final String studentName;
    private final String coachName;

    public CoachAlreadyAddedException(String studentName, String coachName) {
        super(String.format("%s has already been added to %s's coach list.", coachName, studentName));
        this.studentName = studentName;
        this.coachName = coachName;
    }
}
