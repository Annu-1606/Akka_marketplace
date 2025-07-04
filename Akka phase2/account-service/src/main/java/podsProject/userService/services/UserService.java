package podsProject.userService.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import podsProject.userService.DTO.UserDTO;
import podsProject.userService.entity.User;
import podsProject.userService.repository.UserRepository;


import java.util.Optional;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

   public User createUser(UserDTO userDTO) {
        String email = userDTO.getEmail();
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("User with email " + email + " already exists");
        }

        // Create user object
        User user = new User();
        user.setName(userDTO.getName());
        user.setEmail(email);
        user.setDiscountAvailed(false);

        // get id from userDTO
        if (userDTO.getId() != null) {
            user.setId(userDTO.getId());
        }

        // save id 
        return userRepository.save(user);
    }

    public User getUserById(Integer userId) {
        return userRepository.findById(userId).orElse(null);
    }
    public User updateDiscountAvailed(Integer userId, Boolean discountAvailed) {
        User user = getUserById(userId);
        user.setDiscountAvailed(discountAvailed);
        return userRepository.save(user);
    }

    public boolean deleteUser(Integer userId) {
        User user=userRepository.findById(userId).orElse(null);

        if (user==null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

// Delete orders
        try {
            restTemplate.delete("http://marketplace-service:8081/marketplace/users/"+ userId);
        } catch (HttpClientErrorException.NotFound e) {
            // Ignore the 404 error if the user doesn't have any bookings
        }

// Delete wallet
        try {
            restTemplate.delete("http://wallet-service:8082/wallets/"+ userId);
        } catch (HttpClientErrorException.NotFound e) {
            // Ignore the 404 error if the user doesn't have any bookings
        }
        userRepository.deleteById(userId);
        return ResponseEntity.ok("User and associated data deleted successfully").hasBody();
    }

  public void deleteAllUsers() {
    try {
        restTemplate.delete("http://marketplace-service:8081/marketplace");
    } catch (Exception e) {
        System.err.println("⚠️ Warning: Marketplace Service is unavailable. Skipping order deletion.");
    }

    try {
        restTemplate.delete("http://wallet-service/wallets");
    } catch (Exception e) {
        System.err.println("⚠️ Warning: Wallet Service is unavailable. Skipping wallet deletion.");
    }

    userRepository.deleteAll();  // Always delete users
}


    }

