# 个人信息模块发布说明

## 数据库
- 执行 `backend/src/main/resources/migrations/V2__add_user_profile_fields.sql` 添加 `school`,`phone`,`email`,`bio` 列。

## 后端
- 读取：`GET /v1/user/profile` 返回扩展信息。
- 更新：`PATCH /v1/user/profile` 需认证，仅允许更新本人资料。
- 菜单：`DataInitializer` 路径更新为 `/account/profile`。

## 前端
- 新增路由：`/account/profile`。
- 页面：`views/AccountProfile.vue` 表单编辑并保存后刷新用户状态。

## 回滚
- 前端隐藏入口或恢复旧路径。
- 后端保留接口，限制更新或忽略新字段。
- 数据库列保留不删。
