# Use the Eclipse Temurin image for Java 23
FROM eclipse-temurin:23-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy the source code
COPY src ./src

# Install Gradle and build the application
RUN chmod +x gradlew && ./gradlew clean build -x test

# Verify the build output
RUN ls -l build/libs

# Copy the built JAR file to the working directory
COPY build/libs/api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080 
EXPOSE 5000/udp

ENTRYPOINT ["java", "-jar", "app.jar"]