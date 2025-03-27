package org.slotify.userservice.config;

import org.modelmapper.ModelMapper;
import org.slotify.userservice.entity.user.Coach;
import org.slotify.userservice.entity.user.Student;
import org.slotify.userservice.payload.user.CoachDto;
import org.slotify.userservice.payload.user.StudentDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Custom mapping for Coach to CoachDto
        modelMapper.typeMap(Coach.class, CoachDto.class).addMappings(mapper ->
                mapper.using(context -> {
                    Set<Student> students = (Set<Student>) context.getSource(); // Explicit cast
                    if (students == null || students.isEmpty()) {
                        return Collections.emptySet();
                    }
                    return students.stream().map(Student::getId).collect(Collectors.toSet());
                }).map(Coach::getStudents, CoachDto::setStudentIds)
        );

        // Custom mapping for Student to StudentDto
        modelMapper.typeMap(Student.class, StudentDto.class)
                .addMappings(mapper ->
                        mapper.map(src -> src.getDefaultCoach().getId(), StudentDto::setDefaultCoachId)
                )
                .addMappings(mapper ->
                        mapper.using(context -> {
                            Set<Coach> coaches = (Set<Coach>) context.getSource();
                            if (coaches == null || coaches.isEmpty()) {
                                return Collections.emptySet();
                            }
                            return coaches.stream().map(Coach::getId).collect(Collectors.toSet());
                        }).map(Student::getCoaches, StudentDto::setCoachIds)
        );
        return modelMapper;
    }

    @Bean
    public LocaleResolver localeResolver() {
        // Force english for error messages
        return new FixedLocaleResolver(Locale.ENGLISH);
    }
}
