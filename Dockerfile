FROM eclipse-temurin:24-jdk-ubi9-minimal

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw package -DskipTests

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/online-code-compiler-0.0.1-SNAPSHOT.jar"]