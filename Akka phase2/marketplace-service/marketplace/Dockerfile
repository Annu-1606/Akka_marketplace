
    # Stage 1: Build
    FROM eclipse-temurin:21 AS build
    
    WORKDIR /app
    
    RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
    
    COPY pom.xml ./
    RUN mvn dependency:go-offline
    
    COPY src ./src
    RUN mvn clean package -DskipTests
    
    # Stage 2: Run
    FROM eclipse-temurin:21
    
    WORKDIR /app
    
    RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
    
    COPY --from=build /app /app
    
    EXPOSE 8080
    
    # Default entrypoint: run mvn exec:java
    ENTRYPOINT ["mvn", "exec:java", "-Dexec.mainClass=marketplace.marketplace.MarketplaceServiceApplication"]