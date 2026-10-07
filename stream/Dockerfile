FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY api/pom.xml api/pom.xml
COPY stream/pom.xml stream/pom.xml
COPY common/src common/src
COPY stream/src stream/src
RUN mvn -B -pl stream -am -DskipTests package

FROM spark:3.5.8-java17
USER root
RUN mkdir -p /opt/claimpulse /checkpoint && chown -R spark:spark /opt/claimpulse /checkpoint
COPY --from=build --chown=spark:spark /workspace/stream/target/stream-0.1.0.jar /opt/claimpulse/stream.jar
USER spark
ENTRYPOINT ["/opt/spark/bin/spark-submit"]
CMD ["--master", "local[2]", "--class", "dev.jiabei.claimpulse.stream.ClaimPulseStream", "/opt/claimpulse/stream.jar"]
