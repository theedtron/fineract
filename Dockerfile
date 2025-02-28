FROM azul/zulu-openjdk-debian:17 AS builder

RUN apt-get update -qq && apt-get install -y wget

COPY . fineract
WORKDIR /fineract

# Add Pentaho dependencies to build.gradle before building
# RUN ./gradlew --info --no-daemon -q -x rat -x compileTestJava -x test -x spotlessJavaCheck -x spotlessJava bootJar
RUN ./gradlew clean bootJar -x licenseMain -x licenseTest  --info

WORKDIR /fineract/target
RUN echo "Contents of /fineract/fineract-provider/build/libs:" && \
    ls -la /fineract/fineract-provider/build/libs && \
    JAR_FILE=$(ls /fineract/fineract-provider/build/libs/fineract-provider-*.jar) && \
    echo "Extracting JAR: $JAR_FILE" && \
    jar -xf $JAR_FILE

# Download JDBC driver
WORKDIR /fineract/target/BOOT-INF/libs
RUN wget -q https://downloads.mariadb.com/Connectors/java/connector-java-2.7.3/mariadb-java-client-2.7.3.jar

# =========================================

FROM azul/zulu-openjdk-alpine:17 AS fineract

# Copy application files
COPY --from=builder /fineract/target/BOOT-INF/lib /app/lib
COPY --from=builder /fineract/target/META-INF /app/META-INF
COPY --from=builder /fineract/target/BOOT-INF/classes /app

# Create Pentaho directories
RUN mkdir -p /app/pentahoReports /app/plugins

# Copy Pentaho files if they exist
COPY --from=builder /fineract/pentaho-plugin-10/pentahoReports/* /app/pentahoReports/
COPY --from=builder /fineract/pentaho-plugin-10/libs/* /app/plugins/

WORKDIR /

COPY entrypoint.sh /entrypoint.sh
RUN chmod 775 /entrypoint.sh

# Set Pentaho environment variables
ENV FINERACT_PENTAHO_REPORTS_PATH=/app/pentahoReports
ENV FINERACT_PENTAHO_PLUGINS_PATH=/app/plugins

EXPOSE 8443

ENTRYPOINT ["/entrypoint.sh"]
