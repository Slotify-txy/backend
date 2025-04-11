package org.slotify.openhourservice.service;


import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slotify.openhourservice.entity.OpenHour;

import java.util.List;
import java.util.UUID;

public interface OpenHourService {

    List<OpenHour> getOpenHoursByCoachId(UUID coachId);

    @CanIgnoreReturnValue
    OpenHour getOpenHourById(UUID id);

    List<OpenHour> createOpenHours(UUID coachId, List<OpenHour> openHours);

    void deleteOpenHourById(UUID id);

    void deleteOpenHoursByCoachId(UUID coachId);
}
