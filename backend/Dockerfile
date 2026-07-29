# Build and runtime are separate stages so the shipped image carries a JRE and a
# jar, not Maven and the whole dependency cache.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Copy the wrapper and POM first: dependency resolution then re-runs only when
# the POM actually changes, not on every source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user; nothing in this service needs to write to its own image.
RUN addgroup -S app && adduser -S -G app app
COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8080

# Container-aware heap sizing: the platform's memory cap is the source of truth,
# not a hardcoded -Xmx that breaks when the instance size changes.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
