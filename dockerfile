# First stage, build the custom JRE
FROM eclipse-temurin:23-jdk-alpine AS jre-builder

# Install binutils, required by jlink
RUN apk update &&  \
    apk add binutils

# Build small JRE image
RUN $JAVA_HOME/bin/jlink \
--verbose \
--add-modules ALL-MODULE-PATH \
--strip-debug \
--no-man-pages \
--no-header-files \
--compress=2 \
--output /optimized-jdk-23

# Second stage, Use the custom JRE and build the app image
FROM alpine:latest
ENV JAVA_HOME=/opt/jdk/jdk-23
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# copy JRE from the base image
COPY --from=jre-builder /optimized-jdk-23 $JAVA_HOME

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