
FROM maven:3.8.4-openjdk-17 AS build
COPY src /app/src/
COPY pom.xml /app/
WORKDIR /app
RUN mvn clean compile assembly:single

FROM openjdk:17
COPY --from=build /app/target/lightchain-sdp-1.0-SNAPSHOT-jar-with-dependencies.jar .
COPY prometheus /app/prometheus/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "lightchain-sdp-1.0-SNAPSHOT-jar-with-dependencies.jar"]