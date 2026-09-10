FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/*.jar app.jar

# Install curl (used by HEALTHCHECK) before dropping to the unprivileged user.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --shell /usr/sbin/nologin bicap \
    && chown -R bicap:bicap /app
USER bicap

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
