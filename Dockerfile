
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
#COPY . /app
COPY pom.xml /app/
RUN mvn -B -f pom.xml dependency:go-offline
COPY src /app/src/
#RUN mvn clean compile assembly:single
RUN mvn compile assembly:single

FROM openjdk:17
COPY --from=build /app/target/lightchain-sdp-1.0-SNAPSHOT-jar-with-dependencies.jar .
COPY prometheus /app/prometheus/
EXPOSE 8081
ENTRYPOINT ["java", "-cp", "lightchain-sdp-1.0-SNAPSHOT-jar-with-dependencies.jar", "integration.localnet.DemoServer"]