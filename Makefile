IMAGE_NAME := "localhost:5001/lightchain:lastest"

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
	docker image rm -f $(IMAGE_NAME)
	docker build -f ./DockerfileTestnet -t  $(IMAGE_NAME) .
docker-clean-registry:
	docker container stop registry || true
	docker container rm -f registry || true
docker-clean-lightchain:
	(docker rm -f $(docker stop $(docker ps -aq --filter ancestor=$(IMAGE_NAME) --format="{{.ID}}"))) || true
docker-clean-build-lightchain: docker-clean-lightchain docker-clean-registry docker-build-lightchain