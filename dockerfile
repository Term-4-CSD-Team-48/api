# Use the Eclipse Temurin image for Java 23
FROM eclipse-temurin:23-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy the source code
COPY src ./src

# Copy the .env file
COPY .env ./.env

# Install Gradle and build the application
RUN chmod +x gradlew && ./gradlew clean build -x test

# Copy the built JAR file to the working directory
COPY build/libs/*.jar app.jar

EXPOSE 8080 
EXPOSE 5000/udp

ENTRYPOINT ["java", "-jar", "app.jar"]