package org.slotify.userservice.entity.user;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@EqualsAndHashCode(callSuper = true, exclude = "coaches")
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Student extends User {

    @ManyToOne
    @JoinColumn(name = "default_coach")
    private Coach defaultCoach;

    @ManyToMany(mappedBy = "students")
    private Set<Coach> coaches;
}
