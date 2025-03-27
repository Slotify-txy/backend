package org.slotify.userservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.userservice.entity.user.Coach;
import org.slotify.userservice.entity.user.Student;
import org.slotify.userservice.exception.CoachAlreadyAddedException;
import org.slotify.userservice.exception.ResourceNotFoundException;
import org.slotify.userservice.grpc.SlotServiceGrpcClient;
import org.slotify.userservice.payload.user.StudentDto;
import org.slotify.userservice.repository.CoachRepository;
import org.slotify.userservice.repository.StudentRepository;
import org.slotify.userservice.service.impl.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import slot.StudentAndCount;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final StudentRepository studentRepository;
    private final CoachRepository coachRepository;
    private final SlotServiceGrpcClient slotServiceGrpcClient;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public Student getStudentById(UUID studentId) {
        log.info("Get student by id: {}", studentId);
        return studentRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId.toString()));
    }

    @Override
    public Coach getCoachById(UUID coachId) {
        return coachRepository.findById(coachId).orElseThrow(() -> new ResourceNotFoundException("Student", "id", coachId.toString()));
    }

    @Override
    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Student", "email", email));
    }

    @Override
    public Set<Student> getStudentsByCoachId(UUID coachId) {
        return studentRepository.findByDefaultCoach_Id(coachId).orElseThrow(() -> new ResourceNotFoundException("Student", "coachId", coachId.toString()));
    }

    @Override
    public Map<Student, Long> getAvailableStudents(UUID coachId) {
        List<StudentAndCount> studentAndCountList = slotServiceGrpcClient.getAvailableStudents(coachId).getStudentAndCountList();
        Map<Student, Long> studentAndCountMap = new HashMap<>();
        studentAndCountList.forEach(studentAndCount -> {
            Student student = getStudentById(UUID.fromString(studentAndCount.getStudentId()));
            studentAndCountMap.put(student, studentAndCount.getCount());
        });

        return studentAndCountMap;
    }

    @Override
    public Coach updateCoachById(UUID id, Coach coach) {
        Coach existingCoach = coachRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student", "id", id.toString()));
        existingCoach.setInvitationCode(coach.getInvitationCode());
        return coachRepository.save(existingCoach);
    }

    @Override
    public void deleteStudents(List<UUID> ids) {
        ids.forEach(studentRepository::deleteById);
    }

    @Override
    public void addCoachToStudent(UUID studentId, String invitationCode) {
        Coach coach = coachRepository.findByInvitationCode(invitationCode).orElseThrow(() -> new ResourceNotFoundException("Coach", "invitation code", invitationCode));
        Student student = getStudentById(studentId);
        if (student.getCoaches().contains(coach)) {
            throw new CoachAlreadyAddedException(student.getName(), coach.getName());
        }
        coach.getStudents().add(student);
        coachRepository.save(coach);
        student.getCoaches().add(coach);
        if (student.getDefaultCoach() == null) {
            student.setDefaultCoach(coach);
        }
        studentRepository.save(student);
    }

    @Override
    public Student updateStudent(StudentDto studentDto) {
        Student student = getStudentById(studentDto.getId());
        student.setName(studentDto.getName());
        student.setEmail(studentDto.getEmail());
        student.setPicture(studentDto.getPicture());
        student.setDefaultCoach(getCoachById(studentDto.getDefaultCoachId()));
        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public Coach deleteStudentsFromCoach(UUID coachId, List<UUID> studentIds) {
        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new ResourceNotFoundException("Coach", "id", coachId.toString()));
        coach.getStudents().removeIf(student -> studentIds.contains(student.getId()));
        List<Student> students = studentIds.stream().map(studentId -> {
            Student student = getStudentById(studentId);
            student.getCoaches().remove(coach);
            if (student.getDefaultCoach().equals(coach)) {
                student.setDefaultCoach(null);
            }
            return student;
        }).toList();

        studentRepository.saveAll(students);
        return coachRepository.save(coach);
    }
}
