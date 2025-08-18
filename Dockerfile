FROM openjdk:21-jdk-slim

COPY target/booking-0.0.1-SNAPSHOT.jar backend-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "backend-service.jar"]
