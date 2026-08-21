# door-codes-manager

Web app for managing door codes on a Zigbee2MQTT-connected smart lock. Add,
remove, and automatically rotate codes; changes are pushed to the lock over
MQTT and only saved once the lock confirms them.

## Requirements

- A running Zigbee2MQTT instance with an MQTT broker
- A container runtime such as Docker or Podman (for the container path), or JDK 21 + the included Gradle wrapper for local runs

## Configuration

Copy the example config and fill in your values:

```sh
cp config/config.yaml.example config/config.yaml
```

```yaml
zigbee2Mqtt:
  mqtt:
    address: tcp://127.0.0.1   # your MQTT broker
    port: 1883
    topic: zigbee2mqtt         # Zigbee2MQTT base topic
    timeout: 15
    username: someuser
    password: somepassword
automation:
  mqtt:
    address: tcp://127.0.0.1
    port: 1883
    topic: automated_door_codes
    username: someuser
    password: somepassword
security:
  rememberMeKey: change-me     # any random string
  users:
    - name: alice
      password: change-me      # plaintext here; hashed at startup
    - name: bob
      password: change-me
code-start-and-expiration-check-timer: 5m # scheduled timer to check start date and expiration date of door codes
```

`config.yaml` is gitignored — never commit it.

## Running in a container

Pull the image from GHCR:

```sh
docker pull ghcr.io/allen-archer/door-codes-manager
```

Run it, mounting config, data, and logs directories:

```sh
docker run --rm -p 8080:8080 \
  -v "$(pwd)/data":/data \
  -v "$(pwd)/config":/config \
  -v "$(pwd)/logs":/logs \
  ghcr.io/allen-archer/door-codes-manager
```

The container reads config from `/config/config.yaml`, stores its SQLite
database in `/data`, and writes logs to `/logs`. Substitute `podman` (or
your runtime of choice) for `docker` if needed.

`scripts/build_image.sh` and `scripts/run_container.sh` show equivalent
build/run commands for local development.

## Running locally

```sh
./gradlew bootRun
```

Uses `./config/config.yaml` and `./data/door_codes_manager.db` relative to
the working directory by default (override with `CONFIG_PATH` / `DB_PATH`).

## Tests

```sh
scripts/run_tests.sh
```

## Tech stack

- Kotlin 2.3 / JDK 21
- Spring Boot 4.1, Spring Security
- Vaadin Flow 25.2
- Hibernate/JPA on SQLite
- Eclipse Paho MQTT client
- Gradle