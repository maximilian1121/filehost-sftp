FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:25-jre AS run
WORKDIR /app
RUN mkdir -p /app/files
COPY --from=build /app/target/*.jar app.jar

VOLUME ["/app/files"]
EXPOSE 8080 2222
ENTRYPOINT ["java", "-jar", "app.jar"]