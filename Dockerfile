FROM openjdk:17-slim

WORKDIR /app

COPY *.java ./

RUN javac *.java

CMD ["java", "Main"]

