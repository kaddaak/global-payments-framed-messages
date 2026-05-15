# Framed Message Client/Server

## Reviewer Quick Start

| Goal | Command |
| --- | --- |
| Run tests | `mvn test` |
| Build package | `mvn clean package` |
| Start server | `mvn compile exec:java -Dexec.mainClass=com.globalpayments.assignment.ServerMain` |
| Send default sample | `mvn compile exec:java -Dexec.mainClass=com.globalpayments.assignment.ClientMain` |
| Send Croatian sample | `mvn compile exec:java -Dexec.mainClass=com.globalpayments.assignment.ClientMain -Dexec.args="--input docs/assignment/sample-input-hr.bin"` |

## Requirements

| Tool | Version |
| --- | --- |
| Java | 17+ |
| Maven | 3.9+ |

## Manual Run

Server:

```bash
mvn compile exec:java -Dexec.mainClass=com.globalpayments.assignment.ServerMain
```

Client:

```bash
mvn compile exec:java -Dexec.mainClass=com.globalpayments.assignment.ClientMain
```

## Verify Results

| Output | Location |
| --- | --- |
| UTF-8 flat file | `data/messages.txt` |
| SQLite database | `data/framed-messages.db` |
| Database schema | `src/main/resources/schema.sql` |
| Assignment samples | `docs/assignment/*.bin` |

ASCII sample output:

```text
PAYMENT|12345|100.50|HRK
REFUND|98765|42.00|EUR
END
```

Croatian sample output:

```text
UPLATA|ČĆŽŠĐ|100.50|HRK
STORNO|žćčšđ|42.00|EUR
KRAJ
```

## Configuration

| CLI | System property | Default |
| --- | --- | --- |
| `--host` | `app.server.host` | `127.0.0.1` |
| `--port` | `app.server.port` | `5000` |
| `--input` | `app.client.input-file` | `docs/assignment/sample-input-ascii.bin` |
| `--output` | `app.output.file` | `data/messages.txt` |
| `--db` | `app.database.url` | `jdbc:sqlite:data/framed-messages.db` |
| `--schema` | `app.database.schema` | `schema.sql` |

| Priority | Source |
| --- | --- |
| 1 | CLI argument |
| 2 | Java system property |
| 3 | `src/main/resources/application.properties` |

Port override:

```bash
mvn compile exec:java -Dexec.mainClass=com.globalpayments.assignment.ServerMain -Dexec.args="--port 5001"
```

```bash
mvn compile exec:java -Dexec.mainClass=com.globalpayments.assignment.ClientMain -Dexec.args="--port 5001"
```

## Deliverables

| Assignment item | Location |
| --- | --- |
| Client source | `ClientMain.java` |
| Server source | `ServerMain.java` |
| Database definition | `schema.sql` |
| Unit tests | `src/test/java` |
| Integration tests | `src/test/java` |
| Design note | `docs/design-note.md` |
| Assignment files | `docs/assignment` |
