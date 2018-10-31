FROM openjdk:8-alpine

COPY ./target/bpm-engine-akka-1.0-SNAPSHOT-allinone.jar /usr/src/

CMD ["java", "-cp", "/usr/src/bpm-engine-akka-1.0-SNAPSHOT-allinone.jar", "com.pintailai.RxBpmExample"]