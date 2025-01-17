
FROM azul/zulu-openjdk-debian:17 AS builder

RUN apt-get update -qq && apt-get install -y wget

COPY . fineract
WORKDIR /fineract

RUN ./gradlew --no-daemon -q -x rat -x compileTestJava -x test -x spotlessJavaCheck -x spotlessJava bootJar

WORKDIR /fineract/target
RUN jar -xf /fineract/fineract-provider/build/libs/fineract-provider-1.8.3.jar

# We download separately a JDBC driver (which not allowed to be included in Apache binary distribution)
WORKDIR /fineract/target/BOOT-INF/libs
RUN wget -q https://downloads.mariadb.com/Connectors/java/connector-java-2.7.3/mariadb-java-client-2.7.3.jar

# =========================================

FROM azul/zulu-openjdk-alpine:17 AS fineract

COPY --from=builder /fineract/target/BOOT-INF/lib /app/lib
COPY --from=builder /fineract/target/META-INF /app/META-INF
COPY --from=builder /fineract/target/BOOT-INF/classes /app
#COPY ./pentahoReports /app/.mifosx/pentahoReports
#COPY ./pentaho-lib/libs/ /app/lib

WORKDIR /

COPY entrypoint.sh /entrypoint.sh

RUN chmod 775 /entrypoint.sh

# Set the environment variable
#ENV FINERACT_PENTAHO_REPORTS_PATH="$PWD/app/.mifosx/pentahoReports"

EXPOSE 8443

ENTRYPOINT ["/entrypoint.sh"]
