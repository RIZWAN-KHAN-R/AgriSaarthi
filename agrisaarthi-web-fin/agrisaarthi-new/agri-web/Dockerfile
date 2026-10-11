FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN sh ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build --chown=appuser:appuser /workspace/target/agri-web-0.0.1-SNAPSHOT.jar /app/app.jar
USER appuser
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
