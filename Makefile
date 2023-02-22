IMAGE_NAME := "localhost:5001/lightchain:lastest"
CONTAINER_NAME := "NODE"
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
compile-lightchain:
	mvn -B -f pom.xml dependency:go-offline -DskipTests
	mvn compile assembly:single -DskipTests
docker-build-lightchain: compile-lightchain
	docker run -d -p 5001:5001 --name registry registry:2
	docker image rm -f $(IMAGE_NAME)
	docker build -f ./DockerfileTestnet -t  $(IMAGE_NAME) .
docker-clean-registry:
	docker container stop registry || true
	docker container rm -f registry || true
docker-stop-lightchain:
	docker container stop $$(docker ps -aq --filter name="NODE" --format="{{.ID}}") || true
docker-remove-lightchain:
	docker container rm -f $$(docker ps -aq --filter name="NODE" --format="{{.ID}}") || true
docker-remove-lightchain-volume:
	docker volume rm $$(docker volume ls | grep "^NODE_" | awk '{print $2}')
docker-clean-lightchain: docker-stop-lightchain docker-remove-lightchain
docker-clean-build: docker-clean-lightchain docker-clean-registry docker-build-lightchain