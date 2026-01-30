# ---- build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# cache deps
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# build
COPY src ./src
RUN mvn -q -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Persist H2 file DB here
VOLUME ["/app/data"]

# Copy jar (assumes single jar in target/)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Ensure H2 file path uses /app/data inside container
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/urlshortener;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE

ENTRYPOINT ["java","-jar","/app/app.jar"]
