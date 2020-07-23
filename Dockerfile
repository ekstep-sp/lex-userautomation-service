FROM openjdk:8-jre-alpine
RUN adduser -u 1001 -h /home/sunbird/ -D sunbird \
    && mkdir -p /home/sunbird \
    && chown -R sunbird:sunbird /home/sunbird
USER sunbird
# This assume that the content-service dist is unzipped.
COPY --chown=sunbird ./target/UserAutomation-0.0.7-SNAPSHOT.jar /home/sunbird/UserAutomation-0.0.7-SNAPSHOT.jar
WORKDIR /home/sunbird/
CMD java -jar UserAutomation-0.0.7-SNAPSHOT.jar