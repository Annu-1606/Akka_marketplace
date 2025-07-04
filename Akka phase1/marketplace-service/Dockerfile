# Use Maven as the base image
FROM maven:3.8.5-openjdk-17-slim
 
FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y maven
WORKDIR /app
COPY . ${WORKDIR}
EXPOSE 8080
RUN mvn compile
CMD ["sh", "-c", "mvn exec:java"]
