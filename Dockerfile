FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/women-day-app-1.0.0.jar app.jar
RUN mkdir -p uploads/images
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
