FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
COPY webapp ./webapp
RUN mvn -B -q package

FROM tomcat:9.0-jdk17-temurin
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/dompetku.war /usr/local/tomcat/webapps/dompetku.war

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8080/dompetku/health || exit 1
CMD ["catalina.sh", "run"]
