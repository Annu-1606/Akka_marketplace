# Use an official OpenJDK runtime as a parent image
FROM openjdk:17

# Set the working directory to /userService
WORKDIR /userService

# Copy the current directory contents into the container at /userService
COPY . .

# Install Maven
RUN microdnf install -y maven

# Build the projects with Maven
RUN mvn clean install



ENTRYPOINT ["java" ,"-jar","target/userService-0.0.1.jar"]
