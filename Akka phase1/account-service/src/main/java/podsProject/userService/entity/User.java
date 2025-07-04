package podsProject.userService.entity;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "app_user")
public class User {

    @Id
    Integer id;  // This allows auto-generation of ID unless set manually

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "email", unique = true)
    String email;

    @Column(name = "discount_availed", nullable = false)
    Boolean discountAvailed = false;


    }


