# 1. 使用官方 Java 运行镜像
FROM openjdk:17-jdk-alpine

# 2. 把 Jar 复制到容器里
ARG JAR_FILE=target/demo-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 3. 暴露端口
EXPOSE 8080

# 4. 启动命令
ENTRYPOINT ["java","-jar","/app.jar"]
