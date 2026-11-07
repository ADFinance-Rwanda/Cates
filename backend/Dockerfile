FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S app -G app
WORKDIR /app
COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY --from=build /src/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN chmod 755 /docker-entrypoint.sh
USER app
EXPOSE 8082
ENTRYPOINT ["/docker-entrypoint.sh"]
