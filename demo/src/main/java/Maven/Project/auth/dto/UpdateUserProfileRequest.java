package Maven.Project.auth.dto;

import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String username;
    private String email;
    private String organization;
    private String phone;
    private String bio;
}


