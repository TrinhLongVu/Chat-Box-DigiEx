# Use an official OpenJDK 21 runtime as a parent image
FROM amazoncorretto:22-alpine

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the build stage
COPY /target/Server-1.0.0.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]