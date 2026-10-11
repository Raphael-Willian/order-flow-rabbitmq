FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build
COPY pom.xml .
COPY common common
COPY order-api order-api
COPY payment-worker payment-worker
COPY notification-worker notification-worker
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /build/${MODULE}/target/${MODULE}.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]