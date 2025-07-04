package podsProject.userService.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import podsProject.userService.entity.User;

import java.util.Optional;

@Repository

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByEmail(String email);
}
