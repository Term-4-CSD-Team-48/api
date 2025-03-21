FROM eclipse-temurin:23

# Set the working directory in the container
WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy the source code
COPY src ./src

# Install Gradle and build the application
RUN chmod +x gradlew && ./gradlew clean build -x test

EXPOSE 8080 
EXPOSE 5000/udp

ENTRYPOINT ["java", "-jar", "build/libs/api-0.0.1-SNAPSHOT.jar"]