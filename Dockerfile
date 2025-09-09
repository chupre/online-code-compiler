FROM eclipse-temurin:24-jdk-ubi9-minimal AS builder
WORKDIR /app

# Add args for Maven options
ARG MAVEN_OPTS=""
ENV MAVEN_OPTS=${MAVEN_OPTS}

# Copy Maven wrapper files first
COPY mvnw .
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x mvnw

# Copy POM separately to leverage Docker layer caching
COPY pom.xml .

# Download dependencies as a separate layer - will be cached if pom.xml doesn't change
RUN ./mvnw dependency:go-offline -B ${MAVEN_OPTS}

# Copy source code
COPY src src

# Package the application
RUN ./mvnw clean package -DskipTests -B ${MAVEN_OPTS}

FROM sapmachine:24-jre-ubuntu-jammy AS runtime
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
