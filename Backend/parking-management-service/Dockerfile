# Use a base image with Java
FROM openjdk:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the Maven build file and the source code
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src

# Package the application
RUN ./mvnw package

# Copy the jar file to the container
COPY target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8081

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]