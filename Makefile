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


