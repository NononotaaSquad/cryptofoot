FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon && cp build/libs/*.jar application.jar
# Download Playwright Chromium browser for the runtime image
RUN java -cp application.jar -Dloader.main=com.microsoft.playwright.CLI org.springframework.boot.loader.launch.JarLauncher install chromium --with-deps 2>/dev/null || \
    ./gradlew -q playwrightInstall 2>/dev/null || true

FROM eclipse-temurin:25-jre AS extract
WORKDIR /builder
COPY --from=build /workspace/application.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
# Playwright Chromium system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 libcups2 libdrm2 \
    libxkbcommon0 libxcomposite1 libxdamage1 libxfixes3 libxrandr2 \
    libgbm1 libasound2 libpango-1.0-0 libcairo2 libatspi2.0-0 \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
WORKDIR /application
COPY --from=extract /builder/extracted/dependencies/ ./
COPY --from=extract /builder/extracted/spring-boot-loader/ ./
COPY --from=extract /builder/extracted/snapshot-dependencies/ ./
COPY --from=extract /builder/extracted/application/ ./
# Playwright browsers downloaded during build (if any)
COPY --from=build /root/.cache/ms-playwright /home/spring/.cache/ms-playwright
USER spring
EXPOSE 8080
# the JVM defaults to 25% of the container memory for the heap
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
