IMAGE_NAME := "localhost:5001/lightchain:lastest"
NODE_VOLUME_NAME := "lightchain_node_volume"
PROMETHEUS_CONTAINER_NAME := "lightchain_prometheus"
GRAFANA_CONTAINER_NAME := "lightchain_grafana"
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
	docker volume ls -q --filter name=$(NODE_VOLUME_NAME) | xargs -r docker volume rm
docker-stop-prometheus:
	docker container stop $$(docker ps -aq --filter name=$(PROMETHEUS_CONTAINER_NAME) --format="{{.ID}}") || true
docker-stop-grafana:
	docker container stop $$(docker ps -aq --filter name=$(GRAFANA_CONTAINER_NAME) --format="{{.ID}}") || true
docker-stop-metrics: docker-stop-prometheus docker-stop-grafana
docker-clean-lightchain: docker-stop-lightchain docker-remove-lightchain docker-remove-lightchain-volume
docker-clean-build: docker-clean-lightchain docker-clean-registry docker-build-lightchain