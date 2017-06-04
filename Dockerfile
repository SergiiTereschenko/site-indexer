FROM java:8

RUN apt-get update -q && apt-get install -y maven

WORKDIR /code
ADD pom.xml /code/pom.xml
ADD src /code/src
RUN mvn package

EXPOSE 8080
ENTRYPOINT ["/usr/lib/jvm/java-8-openjdk-amd64/bin/java", "-jar", "target/indexer-1.0.0.jar"]