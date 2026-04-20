# ktestify — local test infrastructure

## Quick start

```bash
# 1. Start Kafka + Schema Registry + init topics
cd docker/
docker compose up -d

# 2. Wait for the init container to finish (watch logs)
docker compose logs -f init

# 3. Build the fat JAR (from project root)
cd ../ktestify-core   && mvn install -DskipTests
cd ../ktestify-cucumber && mvn package -DskipTests

# 4. Run the sample tests
cd ..
java -Dconfig.file=docker/local.conf \
     -jar ktestify-cucumber/target/ktestify-cucumber.jar \
     ktestify-cucumber/src/test/resources/features/sample

# 5. Run only @raw or @avro tagged scenarios
java -Dconfig.file=docker/local.conf \
     -jar ktestify-cucumber/target/ktestify-cucumber.jar \
     --tags @raw \
     ktestify-cucumber/src/test/resources/features/sample
```

## What gets created

| Topic | Type | Direction |
|---|---|---|
| `ktestify.raw-orders` | String | INPUT |
| `ktestify.raw-orders-processed` | String | OUTPUT |
| `ktestify.avro-orders` | Avro | INPUT |
| `ktestify.avro-orders-processed` | Avro | OUTPUT |

Avro schemas registered in Schema Registry:
- `ktestify.avro-orders-value` → `Order.avsc`
- `ktestify.avro-orders-processed-value` → `OrderProcessed.avsc`

## Tear down

```bash
docker compose down -v   # removes containers + volumes
```

## Ports

| Service | Port |
|---|---|
| Kafka broker | `localhost:9092` |
| Schema Registry | `http://localhost:8081` |

## Config override

The `local.conf` file points at the compose stack. Pass it at runtime:

```bash
# env var (Docker-friendly)
KTESTIFY_CONFIG_FILE=docker/local.conf java -jar ktestify-cucumber/target/ktestify-cucumber.jar

# JVM property
java -Dconfig.file=docker/local.conf -jar ktestify-cucumber/target/ktestify-cucumber.jar
```

