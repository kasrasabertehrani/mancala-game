# Mancala Game (Spring Boot + WebSockets)

Turn-based Mancala implemented with Spring Boot backend and a simple HTML/JS front-end served from `src/main/resources/static`. Real-time updates use STOMP over WebSockets.

## Prerequisites
- JDK 17+
- Maven Wrapper (`mvnw`/`mvnw.cmd`) included; no global Maven required

## Run (dev)
```bash
./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run
```
App starts on `http://localhost:8080/`.

## Build
```bash
./mvnw clean package   # builds jar in target/
```

## Gameplay Flow
- Create a room from the lobby UI; share the room ID.
- Second player joins by entering the same room ID.
- Moves are turn-based; state is broadcast via WebSocket topics.
- Disconnect handling: brief grace period to reconnect; prolonged disconnect or inactivity forfeits the player (see `GameService`).

## Testing the UI
- Open two browser windows/tabs at `http://localhost:8080/`.
- Join the same room ID from both to simulate two players.
- To test reconnect/timeout: drop network on one tab (DevTools \> Network offline), wait for grace/timeout, then reconnect.

## Project Layout (high level)
- `src/main/java/com/mancalagame`: Spring Boot application code
  - `controller`: REST/WebSocket controllers
  - `service`: game + room services, timers
  - `model`: game domain classes (Game, Board, Pit, etc.)
- `src/main/resources/static`: front-end (`index.html`, `app.js`, `style.css`)

## Notes
- WebSocket endpoint: `/ws` (STOMP). Topics/queues defined in controllers.
- Default inactivity/reconnect thresholds are set in `GameService`; adjust as needed.
- No external database; state is in-memory. Restart clears rooms/games.
