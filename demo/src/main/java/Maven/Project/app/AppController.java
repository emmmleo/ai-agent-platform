// src/main/java/Maven.Project.app/AppController.java
package Maven.Project.app;

import Maven.Project.common.ApiResponse;
import Maven.Project.app.vo.AppVO;
import Maven.Project.app.entity.AppEntity;
import Maven.Project.app.mapper.AppMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/apps")
public class AppController {
    private final AppMapper appMapper;

    public AppController(AppMapper appMapper) { 
        this.appMapper = appMapper; 
    }

    @GetMapping
    public ApiResponse<List<AppVO>> list(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortOrder) {
        // 使用 MyBatis 查询数据库，支持排序
        List<AppEntity> entities;
        
        // 如果指定了排序字段，使用带排序的查询
        if (sortBy != null && (sortBy.equals("rating") || sortBy.equals("downloads"))) {
            // 验证排序方向
            if (!sortOrder.equals("ASC") && !sortOrder.equals("DESC")) {
                sortOrder = "DESC";
            }
            entities = appMapper.findAllWithSort(sortBy, sortOrder);
        } else {
            // 默认按 ID 排序
            entities = appMapper.findAllOrderById();
        }

        // 转换为 VO 对象
        List<AppVO> out = new ArrayList<>();
        for (AppEntity e : entities) {
            AppVO vo = AppVO.builder()
                    .id(e.getId())
                    .name(e.getName())
                    .description(e.getDescription())
                    .fullDescription(e.getFullDescription())
                    .avatar(e.getAvatar())
                    .category(e.getCategory())
                    .price(e.getPrice())
                    .rating(e.getRating())
                    .downloads(e.getDownloads())
                    .reviews(e.getReviews())
                    .author(e.getAuthor())
                    .publishedAt(e.getPublishedAt())
                    .build();
            out.add(vo);
        }
        return ApiResponse.ok(out);
    }

    // 新增：根据分类查询应用
    @GetMapping("/category/{category}")
    public ApiResponse<List<AppVO>> listByCategory(@PathVariable String category) {
        List<AppEntity> entities = appMapper.findByCategory(category);
        List<AppVO> out = new ArrayList<>();
        for (AppEntity e : entities) {
            AppVO vo = AppVO.builder()
                    .id(e.getId())
                    .name(e.getName())
                    .description(e.getDescription())
                    .fullDescription(e.getFullDescription())
                    .avatar(e.getAvatar())
                    .category(e.getCategory())
                    .price(e.getPrice())
                    .rating(e.getRating())
                    .downloads(e.getDownloads())
                    .reviews(e.getReviews())
                    .author(e.getAuthor())
                    .publishedAt(e.getPublishedAt())
                    .build();
            out.add(vo);
        }
        return ApiResponse.ok(out);
    }
}
