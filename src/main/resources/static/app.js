// --- 1. GAME STATE VARIABLES ---
// We need to remember who we are and what room we are in.
let myPlayerId = null;
let currentRoomId = null;
let stompClient = null;
let playerNumber = null; // 1 or 2
let isMyTurn = false;

// --- 2. HTML ELEMENTS ---
// These are the buttons and screens we want to show/hide or click.
const lobbyScreen = document.getElementById('lobby-screen');
const gameScreen = document.getElementById('game-screen');
const roomDisplay = document.getElementById('room-display');
const statusDisplay = document.getElementById('status-display');

// --- 2.5 CHECK FOR SAVED SESSION ON PAGE LOAD ---
window.addEventListener('load', () => {
    const savedSession = localStorage.getItem('mancalaSession');
    const wasInGame = sessionStorage.getItem('wasInGame');

    // Only auto-reconnect if:
    // 1. We have a saved session AND
    // 2. We were previously in a game in THIS tab (page refresh, not new tab)
    if (savedSession && wasInGame) {
        const session = JSON.parse(savedSession);
        reconnectToGame(session.roomId, session.playerId, session.playerNumber);
    } else if (savedSession && !wasInGame) {
        // New tab opened - ask user if they want to continue existing game
        const session = JSON.parse(savedSession);
        const shouldReconnect = confirm(`You have an active game in Room ${session.roomId}. Rejoin?`);
        if (shouldReconnect) {
            reconnectToGame(session.roomId, session.playerId, session.playerNumber);
        } else {
            // User wants to start fresh - clear the session
            localStorage.removeItem('mancalaSession');
        }
    }
});

async function reconnectToGame(roomId, playerId, pNum) {
    try {
        // First check if the room still exists via REST
        const response = await fetch('/api/rooms/' + roomId);
        if (!response.ok) {
            // Room doesn't exist anymore, clear session
            localStorage.removeItem('mancalaSession');
            return;
        }

        const room = await response.json();
        // Reconnect via WebSocket with saved credentials
        joinWebSocket(roomId, playerId, pNum, room.game);
    } catch (e) {
        console.error('Reconnection failed:', e);
        localStorage.removeItem('mancalaSession');
    }
}

// --- 3. REST API CALLS (Lobby Phase) ---

document.getElementById('btn-create').addEventListener('click', async () => {
    const playerName = document.getElementById('player-name').value;
    if (!playerName) return alert("Please enter a name!");

    const response = await fetch('/api/rooms/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ playerName: playerName })
    });

    const room = await response.json();
    // Pass the initial room.game state!
    joinWebSocket(room.roomId, room.game.player1.id, 1, room.game);
});

document.getElementById('btn-join').addEventListener('click', async () => {
    const playerName = document.getElementById('player-name').value;
    const roomId = document.getElementById('room-id-input').value;
    if (!playerName || !roomId) return alert("Please enter a name and room ID!");

    const response = await fetch('/api/rooms/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ roomId: roomId, playerName: playerName })
    });

    if (!response.ok) return alert("Room full or not found!");
    const room = await response.json();
    // Pass the initial room.game state!
    joinWebSocket(room.roomId, room.game.player2.id, 2, room.game);
});

// --- 4. WEBSOCKETS (Gameplay Phase) ---

// Add initialGameState as the 4th parameter
function joinWebSocket(roomId, playerId, pNum, initialGameState) {
    myPlayerId = playerId;
    currentRoomId = roomId;
    playerNumber = pNum;

    // Save session for reconnection after page refresh
    localStorage.setItem('mancalaSession', JSON.stringify({
        roomId: roomId,
        playerId: playerId,
        playerNumber: pNum
    }));

    // Mark this tab as being in a game (survives refresh, but not new tab)
    sessionStorage.setItem('wasInGame', 'true');

    const socket = new SockJS('/mancala-ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, () => {
        lobbyScreen.classList.remove('active');
        gameScreen.classList.add('active');
        roomDisplay.innerText = "Room: " + roomId;
        highlightMyPits();

        // SUBSCRIBE FIRST, then update board
        stompClient.subscribe('/topic/room/' + roomId, (message) => {
            const gameData = JSON.parse(message.body);
            updateBoard(gameData);
        });

        // --- ADD THIS LINE ---
        // Shout to the GameController that we are back!
        // (If it's our first time joining, the server just ignores this, which is perfect)
        stompClient.send('/app/game.reconnect', {}, JSON.stringify({
            roomId: currentRoomId,
            playerId: myPlayerId
        }));

        // THEN draw the board with initial state
        updateBoard(initialGameState);
    });
}
// --- 5. MAKING A MOVE ---

// Add a click listener to every single pit on the board
document.querySelectorAll('.pit').forEach(pitElement => {
    pitElement.addEventListener('click', (event) => {
        // Prevent clicking if not your turn
        if (!isMyTurn) return;

        // Get the number from the pit's HTML id (e.g., "pit-4" -> 4)
        const pitIndex = parseInt(event.target.id.split('-')[1]);

        // Validate: Player 1 can only click pits 0-5, Player 2 can only click pits 7-12
        if (playerNumber === 1 && (pitIndex < 0 || pitIndex > 5)) return;
        if (playerNumber === 2 && (pitIndex < 7 || pitIndex > 12)) return;

        // Send the PlayPitCommand to your Spring Boot GameController
        const command = {
            roomId: currentRoomId,
            playerId: myPlayerId,
            pitIndex: pitIndex
        };
        stompClient.send('/app/game.move', {}, JSON.stringify(command));
    });
});

// --- 6. HIGHLIGHT MY PITS ---
function highlightMyPits() {
    // Add CSS class to player's own pits
    if (playerNumber === 1) {
        document.querySelectorAll('.player1-row .pit').forEach(p => p.classList.add('my-pit'));
        document.querySelectorAll('.player2-row .pit').forEach(p => p.classList.add('opponent-pit'));
    } else {
        document.querySelectorAll('.player2-row .pit').forEach(p => p.classList.add('my-pit'));
        document.querySelectorAll('.player1-row .pit').forEach(p => p.classList.add('opponent-pit'));
    }
}

// --- 7. UPDATING THE SCREEN ---

function updateBoard(gameData) {
    // 1. Update the stones in all 14 pits
    const pits = gameData.board.pits;
    for (let i = 0; i < 14; i++) {
        document.getElementById('pit-' + i).innerText = pits[i].stones;
    }

    // 2. Determine if it's my turn
    isMyTurn = (gameData.gameStatus === 'PLAYER_1_TURN' && playerNumber === 1) ||
               (gameData.gameStatus === 'PLAYER_2_TURN' && playerNumber === 2);

    // 3. Update pit clickability based on turn
    updatePitStates();

    // 4. Update the status text
    if (gameData.gameStatus === 'WAITING_FOR_PLAYER_2') {
        statusDisplay.innerText = "Waiting for an opponent to join...";
    } else if (gameData.gameStatus === 'PLAYER_1_TURN') {
        statusDisplay.innerText = playerNumber === 1 ? "Your Turn! 🎯" : "Opponent's Turn...";
    } else if (gameData.gameStatus === 'PLAYER_2_TURN') {
        statusDisplay.innerText = playerNumber === 2 ? "Your Turn! 🎯" : "Opponent's Turn...";
    } else if (gameData.gameStatus === 'PAUSED_FOR_RECONNECT') {
        statusDisplay.innerText = "Opponent lost connection! Pausing for 30 seconds...";
        disableAllPits(); // Freeze the board!
    } else if (gameData.gameStatus === 'GAME_OVER' || gameData.gameStatus === 'FORFEIT' || gameData.gameStatus === 'PLAYER_DISCONNECTED') {
        if (gameData.winner === 'DRAW') {
            statusDisplay.innerText = "Game Over - It's a Draw!";
        } else if (gameData.winner === myPlayerId) {
            statusDisplay.innerText = "Game Over - You WIN! 🎉";
        } else {
            statusDisplay.innerText = "Game Over - You Lose 😢";
        }
        disableAllPits();
        // Clear saved session - game is over
        localStorage.removeItem('mancalaSession');
        sessionStorage.removeItem('wasInGame');
    }
}

function updatePitStates() {
    document.querySelectorAll('.my-pit').forEach(pit => {
        if (isMyTurn) {
            pit.classList.add('active');
            pit.classList.remove('disabled');
        } else {
            pit.classList.remove('active');
            pit.classList.add('disabled');
        }
    });
}

function disableAllPits() {
    document.querySelectorAll('.pit').forEach(pit => {
        pit.classList.add('disabled');
        pit.classList.remove('active');
    });
}
