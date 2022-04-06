lint:
	@mvn checkstyle:checkstyle
lint-verbose:
	@mvn -e checkstyle:checkstyle
check:
	@mvn spotbugs:check
test:
	@mvn clean install
	@mvn compile
	@mvn test
generate:
	@mvn clean install
	@mvn compile
proto:
	@mvn protobuf:compile-custom
	@mvn protobuf:compile
	@cp target/generated-sources/protobuf/java/network/p2p/* src/main/java/network/p2p
	@cp target/generated-sources/protobuf/grpc-java/network/p2p/MessengerGrpc.java src/main/java/network/p2p
	@mvn clean install
	@mvn compile