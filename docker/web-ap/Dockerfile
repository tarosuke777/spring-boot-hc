# ========================
# ステージ 1: ビルドステージ (JARの作成のみ)
# ========================
FROM gradle:jdk21 AS builder 

# RUN apk update && apk add --no-cache bash build-base

# Jenkinsのshコマンド内容をDockerに移す
COPY . /app
WORKDIR /app
RUN chmod +x gradlew
RUN ./gradlew clean build

# ========================
# ステージ 2: 実行ステージ (JARの実行のみ)
# ========================
# FROM openjdk:21-jdk
FROM eclipse-temurin:21-jre-ubi10-minimal
# タイムゾーンを日本時間に変更（コンテナ内の実行環境をJSTに）
ENV TZ Asia/Tokyo 

# ビルドステージから作成されたJARをコピー
COPY --from=builder /app/build/libs/spring-boot-hc-0.0.1-SNAPSHOT.jar app.jar

# 実行
ENTRYPOINT ["java", "-jar", "app.jar"]