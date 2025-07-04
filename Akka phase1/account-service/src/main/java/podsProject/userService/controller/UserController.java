package podsProject.userService.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import podsProject.userService.DTO.UserDTO;
import podsProject.userService.entity.User;
import podsProject.userService.services.UserService;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody UserDTO userDTO) {
        try {
            User user = userService.createUser(userDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable("userId") Integer userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with id " + userId + " not found");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(user);
        }
    }
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable("userId") Integer userId) {
        boolean deleted = userService.deleteUser(userId);
        if (deleted) {
            return ResponseEntity.status(HttpStatus.OK).body("User with id " + userId + " deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with id " + userId + " not found");
        }
    }

    @DeleteMapping("/users")
    public ResponseEntity<?> deleteAllUsers() {
        try {
            userService.deleteAllUsers();
            return ResponseEntity.status(HttpStatus.OK).body("All users and their bookings deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PutMapping("/users/{userId}/discount")
    public ResponseEntity<?> updateDiscountAvailed(@PathVariable Integer userId, @RequestBody UserDTO userDTO) {
        User updatedUser = userService.updateDiscountAvailed(userId, userDTO.getDiscountAvailed());
        return ResponseEntity.ok(updatedUser);
    }
}
