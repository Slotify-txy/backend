package org.slotify.slotservice.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class SlotStatusStaleException extends RuntimeException {

    private final UUID id;

    public SlotStatusStaleException(UUID id) {
        super(String.format("The status of Slot %s is stale.", id.toString()));
        this.id = id;
    }
}
