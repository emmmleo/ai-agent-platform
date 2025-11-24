// src/main/java/Maven.Project.app/mapper/AppMapper.java
package Maven.Project.app.mapper;

import Maven.Project.app.entity.AppEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper  // 标识为 MyBatis Mapper 接口
public interface AppMapper {
    // 查询所有应用，按 ID 升序排列
    List<AppEntity> findAllOrderById();

    // 根据 ID 查询单个应用
    AppEntity findById(Long id);

    // 根据分类查询应用
    List<AppEntity> findByCategory(String category);

    // 查询所有应用，支持排序参数
    List<AppEntity> findAllWithSort(@Param("sortBy") String sortBy, @Param("sortOrder") String sortOrder);
}
