# ---- 第一阶段：构建 ----
# 使用Maven和JDK的基础镜像来编译项目
FROM maven:3.8.4-openjdk-17-slim AS build
# 设置容器内的工作目录
WORKDIR /app
# 将本地的pom.xml和src目录复制到容器的工作目录
COPY pom.xml .
COPY src ./src
# 运行Maven命令，清理、打包（跳过测试）生成jar文件
RUN mvn clean package -DskipTests

# ---- 第二阶段：运行 ----
# 使用Eclipse Temurin的JDK 17镜像（官方推荐的OpenJDK构建）
FROM eclipse-temurin:17-jdk-jammy
# 设置容器内的工作目录
WORKDIR /app
# 从构建阶段的容器中，将生成的jar文件复制到当前运行阶段的容器中，并重命名为app.jar
COPY --from=build /app/target/*.jar app.jar

# 声明容器运行时监听的端口（Spring Boot默认是8080）
EXPOSE 8080

# 容器启动时执行的命令：运行app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
