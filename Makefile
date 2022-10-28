generate: proto
	@mvn clean install
	@mvn compile
proto:
	@mvn protobuf:compile-custom
	@mvn protobuf:compile
	@cp target/generated-sources/protobuf/java/network/p2p/proto/* src/main/java/network/p2p/proto/
	@cp target/generated-sources/protobuf/grpc-java/network/p2p/proto/MessengerGrpc.java src/main/java/network/p2p/proto/
lint:
	@mvn checkstyle:checkstyle
lint-verbose:
	@mvn -e checkstyle:checkstyle
spotbug-check:
	@mvn spotbugs:check
test: proto generate
	@mvn test
check:
	@ mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version -Dlog4j.debug
docker-build-lightchain:
	mvn -B -f pom.xml dependency:go-offline -DskipTests
	mvn compile assembly:single -DskipTests
	docker run -d -p 5001:5001 --name registry registry:2
	docker build -f ./DockerfileTestnet -t localhost:5001/lightchain:lastest .

docker-clean-lightchain:
	docker container stop registry && docker container rm -v registry
docker-stop-all:
	docker rm -f $(docker ps -aq) && docker rmi -f $(docker images -q)
docker-clean-build-lightchain: docker-clean-lightchain docker-build-lightchain