FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY api/pom.xml api/pom.xml
COPY stream/pom.xml stream/pom.xml
COPY common/src common/src
COPY api/src api/src
RUN mvn -B -pl api -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/api/target/api-0.1.0.jar /app/api.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/api.jar"]
