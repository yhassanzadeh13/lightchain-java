
FROM maven:3.6.3-jdk-11
WORKDIR /app
COPY . /app
#RUN mvn -f pom.xml clean package
RUN mkdir "d"
RUN mvn -B -f pom.xml dependency:go-offline
RUN mvn -B install
RUN javac DemoServer.java -d d
CMD ["java", "-cp", "d", "integration.localnet.DemoServer"]