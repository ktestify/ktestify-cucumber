<p align="center">
  <img src="https://raw.githubusercontent.com/ktestify/.github/refs/heads/main/profile/assets/png/ktestify-banner-2x.png" alt="ktestify-cucumber" width="100%"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/build-passing-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="build passing"/>
  <img src="https://img.shields.io/badge/license-Apache%202.0-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="license"/>
  <img src="https://img.shields.io/badge/java-25-2DD4BF?style=flat-square&labelColor=0C1018&color=2DD4BF" alt="java 25"/>
  <img src="https://img.shields.io/badge/version-1.0.5-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="version"/>
  <img src="https://img.shields.io/badge/docker-ghcr.io%2Fktestify%2Fktestify--cucumber-2DD4BF?style=flat-square&labelColor=0C1018&color=2DD4BF" alt="docker"/>
</p>

<br/>

**ktestify-cucumber** is the standalone BDD test runner of the [ktestify](https://github.com/ktestify) ecosystem. It wraps [ktestify-core](https://github.com/ktestify/ktestify-core) a Cucumber layer & Gherkin step definitions, PicoContainer dependency injection, and a ready-to-run Docker image, so you can write Kafka integration tests in plain English without a single line of Java.

---

## Quick Start

### Run with Docker (recommended)

```bash
docker run --rm \
  -v $(pwd)/features:/workspace/features \
  -v $(pwd)/assets:/workspace/assets \
  -v $(pwd)/config:/workspace/config \
  -v $(pwd)/reports:/workspace/reports \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e SCHEMA_REGISTRY_URL=http://schema-registry:8081 \
  -e config.file=/workspace/config/application.conf \
  ghcr.io/ktestify/ktestify-cucumber:latest \
  /workspace/features
```

### Run with the fat JAR

```bash
java -Dconfig.file=src/test/resources/local.conf \
     -jar target/ktestify-cucumber.jar \
     src/test/resources/features
```



---

## Writing Tests

Tests are written in standard Gherkin inside `.feature` files. A complete roundtrip example:

```gherkin
Feature: Order processing — raw roundtrip

  Background:
    Given namespace
      | namespace |
      | my-org    |
    Given input topic
      | topicName | topicAlias |
      | orders    | orders-in  |
    Given output topic
      | topicName         | topicAlias  |
      | orders-processed  | orders-out  |
    Given assets directory
      | absolutePath                    |
      | src/test/resources/data/orders  |

  Scenario: Processed order matches expected file
    When record from file is sent
      | topicName | file         | recordKey |
      | orders    | order.json   | ORD-001   |
    Then expected record from file
      | topicAlias | file                  | expectedRecordKey | consumerReadTimeout |
      | orders-out | expected-order.json   | ORD-001           | 30                  |
```

---

## Step Catalogue

### Background (`@Given`)

| Step | Purpose |
|---|---|
| `Given namespace` | Register a single topic namespace |
| `Given namespaces` | Register multiple namespaces with aliases |
| `Given input topic` | Register a producer topic |
| `Given output topic` | Register a consumer topic |
| `Given assets directory` | Set the base path for payload/expected files |
| `Given schema` | Register an Avro schema (name + alias + version) |
| `Given input queue` | Register an IBM MQ input queue |
| `Given output queue` | Register an IBM MQ output queue |
| `Given CFT hosts` | Register a CFT file-transfer host |

### Actions (`@When`)

| Step | Purpose |
|---|---|
| `When record from file is sent` | Produce a raw (String) record |
| `When record from file based on schema is sent` | Produce an Avro record |
| `When queue record from file is sent` | Send a message to an IBM MQ queue |
| `When file is sent using CFT` | Transfer a file via CFT |
| `And wait for {int} seconds` | Sleep between steps |
| `When script is executed` | Run a shell script |

### Validations (`@Then` / `@And`)

| Step | Purpose |
|---|---|
| `Then expected record from file` | Raw file match |
| `Then expected record should have fields matching from file` | Positional field match (line/from/to) |
| `Then expected record from file based on XML` | XML structural match with optional exclusions |
| `Then expected record based on XML should have fields matching from file` | XPath expression match |
| `Then expected records from files` | Batch raw records vs. multiple files |
| `Then expected record from file based on schema` | Avro file match with optional excluded keys |
| `Then expected record based on schema should have fields matching from given value` | Avro field/value match |
| `Then expected records from files based on schema` | Batch Avro records |
| `And record should not appear in topic` | Negative assertion (no record within timeout) |
| `And record should appear in topic` | Positive presence assertion |

---

## Configuration

ktestify-cucumber uses [Typesafe Config](https://github.com/lightbend/config) (HOCON). Override defaults via `-Dconfig.file`, environment variables, or an `application.conf` on the classpath.

```hocon
ktestify {
  kafka {
    bootstrap-servers = "localhost:9092"
    bootstrap-servers = ${?KAFKA_BOOTSTRAP_SERVERS}
    topic-namespace   = ${?KTESTIFY_TOPIC_NAMESPACE}
    consumer.group-id = "my-test-group"
  }

  schema-registry {
    url = "http://localhost:8081"
    url = ${?SCHEMA_REGISTRY_URL}
  }

  framework {
    timeouts {
      default-read-timeout = 30s   # cucumber default (longer than core's 10s)
      consumer-delta-time  = 60s
    }
    directories {
      assets = ""   # set via 'Given assets directory' or KTESTIFY_ASSETS_DIR
    }
  }
}
```

---

## Docker Image

The image is published to [GitHub Container Registry](https://github.com/ktestify/ktestify-cucumber/pkgs/container/ktestify-cucumber) on every release.

```
ghcr.io/ktestify/ktestify-cucumber:<version>
ghcr.io/ktestify/ktestify-cucumber:latest
```

| Mount | Default path inside container | Purpose |
|---|---|---|
| Feature files | `/workspace/features` | Gherkin `.feature` files |
| Assets | `/workspace/assets` | Payload and expected files |
| Config | `/workspace/config` | `application.conf` (set `-Dconfig.file`) |
| Reports | `/workspace/reports` | Cucumber HTML / JSON reports |
| Plugins | `/workspace/plugins` | Drop-in plugin JARs |

The image supports **linux/amd64** and **linux/arm64** (Apple Silicon / cloud ARM).

---

## Dynamic Variables

Payload and expected files are processed transparently by `DynamicVariableProcessor`. Use `{{VARIABLE}}` syntax inside any file read by `FileUtils.getFileContent()`.

| Syntax | Output example |
|---|---|
| `{{date}}` | `2026-05-31` |
| `{{date:yyyy/MM/dd}}` | `2026/05/31` |
| `{{timestamp}}` | `2026-05-31T14:22:00` |
| `{{random}}` | UUID v4 |
| `{{random:str:12}}` | `aB3xZ9qWmK1p` |
| `{{env:MY_VAR}}` | value of `$MY_VAR` |

---

## Related

- **[ktestify-core](https://github.com/ktestify/ktestify-core)** — the transport and assertion engine
- **[docs.ktestify.xyz](https://docs.ktestify.xyz)** — full documentation and configuration reference

---

## Contributing

Contributions are welcome. Please read the contributing guide before opening a pull request.

1. Fork the repository
2. Create a feature branch — `git checkout -b feat/my-feature`
3. Commit with [Conventional Commits](https://www.conventionalcommits.org/) — `git commit -m "feat: add my feature"`
4. Push and open a Pull Request against `main`

---

## License

ktestify-cucumber is licensed under the [Apache License 2.0](LICENSE).

---

<p align="center">
  <img src="https://raw.githubusercontent.com/ktestify/.github/refs/heads/main/profile/assets/png/ktestify-logo-128.png" width="32" height="32" alt="KTestify"/>
  <br/>
  <sub>Assert the stream. Own the pipeline.</sub>
</p>

