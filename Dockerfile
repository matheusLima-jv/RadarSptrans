# Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 radar
USER radar
COPY --from=build /app/target/RadarSPT-*.jar app.jar
EXPOSE 8080
# O token vem do ambiente: docker run -e SPTRANS_API_TOKEN=... -p 8080:8080 radarsptrans
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
