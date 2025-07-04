package podsProject.userService.DTO;

import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDTO {
    private Integer id;  // Make sure this is included
    private String name;
    private String email;
    private boolean discountAvailed;
    public Boolean getDiscountAvailed() {
        return discountAvailed;
    }
}





