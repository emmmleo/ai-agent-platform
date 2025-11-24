package Maven.Project.auth.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserEntity {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String organization;
    private String phone;
    private String bio;
    private String role;
    private LocalDateTime createdAt;
}

