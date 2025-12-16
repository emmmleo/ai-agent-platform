# 多阶段构建 - 阶段1: 构建阶段
# 代理配置（可选）：如果需要代理，通过 --build-arg 传递
# 如果不需要代理，ARG 默认为空，不会影响构建
ARG HTTP_PROXY
ARG HTTPS_PROXY
FROM maven:3.9-eclipse-temurin-21 AS build
# 设置代理环境变量（如果未传递 ARG，值为空，不影响构建）
ENV HTTP_PROXY=${HTTP_PROXY}
ENV HTTPS_PROXY=${HTTPS_PROXY}

WORKDIR /app
COPY pom.xml . 
COPY src ./src
RUN mvn clean package -DskipTests

# 多阶段构建 - 阶段2: 运行阶段
FROM eclipse-temurin:21-jre-alpine
# 运行阶段不需要代理配置

WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/logs && chown -R spring:spring /app
USER spring
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
