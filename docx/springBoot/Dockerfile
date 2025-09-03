FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy the compiled Spring Boot jar
COPY target/payment.jar app.jar

EXPOSE 8080

# Set Timezone to Asia/Kolkata
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Run the Spring Boot jar
ENTRYPOINT ["java", "-jar", "app.jar"]
