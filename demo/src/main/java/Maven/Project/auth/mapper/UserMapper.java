package Maven.Project.auth.mapper;

import Maven.Project.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    UserEntity findByUsername(@Param("username") String username);
    
    void insert(UserEntity user);
    
    UserEntity findById(@Param("id") Long id);
    
    List<UserEntity> findAllNonAdminUsers();
    
    void deleteById(@Param("id") Long id);

    int countByUsernameExcludingId(@Param("username") String username, @Param("excludeId") Long excludeId);

    void updateProfile(UserEntity user);
}

