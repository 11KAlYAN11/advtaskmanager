package com.kalyan.advtaskmanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "assignedTo", cascade = CascadeType.ALL)
    @JsonIgnore          // ← never serialize tasks on a User (stops N+1 on GET /api/users)
    @ToString.Exclude    // ← stops Lombok toString() from triggering the lazy load
    private List<Task> tasks;
}