package org.slotify.slotservice.service;

import org.slotify.slotservice.constant.FrontendSource;
import org.slotify.slotservice.constant.SlotStatus;
import org.slotify.slotservice.entity.Slot;

import java.util.List;
import java.util.UUID;

public interface SlotService {
    List<Slot> getSlotsByStudentIdAndCoachId(UUID studentId, UUID coachId, FrontendSource source);

    List<Slot> getSlotsByCoachId(UUID coachId, FrontendSource source);

    Slot getSlotById(UUID id);


    List<Slot> createSlots(List<Slot> slots);

    void deleteSlotById(UUID id, FrontendSource source);

    Slot updateSlotStatus(UUID id, SlotStatus status);

    String updateSlotStatusViaEmail(UUID id, String token, SlotStatus status);
}
