# syntax=docker/dockerfile:1.7
# API da NANDESK (seção 16.3): build com JDK 21, runtime com JRE 21, usuário não-root e JAR em camadas.

FROM eclipse-temurin:21.0.12_8-jdk-noble AS build
WORKDIR /workspace
# Dependências primeiro: mudam pouco e ficam em cache entre builds.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q -ntp dependency:go-offline -DskipTests
COPY src src
RUN ./mvnw -B -q -ntp -DskipTests package \
    && java -Djarmode=tools -jar target/nandesk-api-*.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:21.0.12_8-jre-noble
LABEL org.opencontainers.image.title="nandesk-api" \
      org.opencontainers.image.description="API da NANDESK: feiras de Belém" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.source="https://github.com/oadrianops/semana_ubiqua_2026.2"
RUN groupadd --system --gid 10001 nandesk \
    && useradd --system --uid 10001 --gid nandesk --home-dir /app --shell /usr/sbin/nologin nandesk \
    && mkdir -p /data/uploads \
    && chown -R nandesk:nandesk /data
WORKDIR /app
COPY --from=build --chown=nandesk:nandesk /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=nandesk:nandesk /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=nandesk:nandesk /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=nandesk:nandesk /workspace/target/extracted/application/ ./
USER nandesk
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=prod \
    APP_STORAGE_LOCAL_PATH=/data/uploads
VOLUME ["/data/uploads"]
EXPOSE 8080 8091
# Readiness pela porta de gestão, sem curl na imagem (bash + /dev/tcp).
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8091 && printf "GET /actuator/health/readiness HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
