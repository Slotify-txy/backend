package org.slotify.userservice.repository;

import org.slotify.userservice.entity.user.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByEmail(String email);

    Optional<Set<Student>> findByDefaultCoach_Id(UUID coachId);
    @Transactional
    void deleteByEmail(String email);
}
