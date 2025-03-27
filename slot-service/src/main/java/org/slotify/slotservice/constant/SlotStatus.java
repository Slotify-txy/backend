package org.slotify.slotservice.constant;

import slot.Status;

public enum SlotStatus {
    AVAILABLE, PENDING, APPOINTMENT, REJECTED, CANCELLED;

    public boolean isAfter(SlotStatus status) {
        return switch (this) {
            case AVAILABLE, PENDING -> false;
            case APPOINTMENT, REJECTED -> status == SlotStatus.PENDING;
            case CANCELLED -> status == SlotStatus.APPOINTMENT;
        };
    }

    public Status mapToProtoStatus() {
        return switch (this) {
            case AVAILABLE -> Status.AVAILABLE;
            case PENDING -> Status.PENDING;
            case APPOINTMENT -> Status.APPOINTMENT;
            case REJECTED -> Status.REJECTED;
            case CANCELLED -> Status.CANCELLED;
        };
    }
}
