
FROM java:8
WORKDIR /app
COPY . /app
RUN mkdir "d"
RUN javac DemoServer.java -d d
CMD ["java", "-cp", "d", "integration.localnet.DemoServer"]