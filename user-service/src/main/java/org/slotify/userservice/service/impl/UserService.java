package org.slotify.userservice.service.impl;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.slotify.userservice.entity.user.Coach;
import org.slotify.userservice.entity.user.Student;
import org.slotify.userservice.payload.user.StudentDto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface UserService {
    Student getStudentById(UUID studentId);

    Coach getCoachById(UUID coachId);

    @CanIgnoreReturnValue
    Student getStudentByEmail(String email);

    Set<Student> getStudentsByCoachId(UUID coachId);

    Map<Student, Long> getAvailableStudents(UUID coachId);

    Coach updateCoachById(UUID id, Coach coach);

    void deleteStudents(List<UUID> ids);

    void addCoachToStudent(UUID studentId, String invitationCode);

    Student updateStudent(StudentDto studentDto);

    Coach deleteStudentsFromCoach(UUID coachId, List<UUID> studentIds);
}
