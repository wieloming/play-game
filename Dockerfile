FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/scala-2.13/phishing-filter.jar /app/phishing-filter.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/phishing-filter.jar"]