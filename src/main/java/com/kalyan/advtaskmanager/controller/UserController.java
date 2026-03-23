package com.kalyan.advtaskmanager.controller;

import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Create a USER
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    // Get user by ID
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    //  Get all USERS
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // DELETE USER by ID
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "User with id " + id + " deleted successfully";
    }

    // DELETE ALL USERS (Admin operation)
    @DeleteMapping
    public String deleteAllUsers() {
        userService.deleteAllUsers();
        return "All users deleted successfully";
    }
}
