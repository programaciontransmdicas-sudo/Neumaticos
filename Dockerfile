FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-jammy
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends libreoffice-calc fonts-liberation fonts-crosextra-carlito && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/neumaticos-0.0.1-SNAPSHOT.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=render LIBREOFFICE_PATH=/usr/bin/libreoffice
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
