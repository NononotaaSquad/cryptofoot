FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon && cp build/libs/*.jar application.jar

FROM eclipse-temurin:25-jre AS extract
WORKDIR /builder
COPY --from=build /workspace/application.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
WORKDIR /application
COPY --from=extract /builder/extracted/dependencies/ ./
COPY --from=extract /builder/extracted/spring-boot-loader/ ./
COPY --from=extract /builder/extracted/snapshot-dependencies/ ./
COPY --from=extract /builder/extracted/application/ ./
USER spring
EXPOSE 8080
# the JVM defaults to 25% of the container memory for the heap
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
