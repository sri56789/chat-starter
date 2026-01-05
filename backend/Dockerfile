# Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create pdfs directory
RUN mkdir -p /app/pdfs

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set memory limits
ENV JAVA_OPTS="-Xmx1g -Xms512m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]



