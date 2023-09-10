FROM azul/zulu-openjdk-alpine:17-latest AS BUILDER
WORKDIR /build/mailserver
COPY . .
RUN ./gradlew runner:installDist --stacktrace --no-daemon

FROM azul/zulu-openjdk-alpine:17-jre-headless-latest
WORKDIR /mailserver
COPY --from=BUILDER /build/mailserver/runner/build/install/runner .

# IMAP
EXPOSE 143
# POP3
EXPOSE 110
# SUBMISSION
EXPOSE 587
# SMTP
EXPOSE 25

ENTRYPOINT [ "/mailserver/bin/runner" ]