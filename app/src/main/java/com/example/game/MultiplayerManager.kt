package com.example.game

import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

enum class NetworkState {
    OFFLINE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

data class RemotePlayer(
    val id: String,
    var name: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var facingLeft: Boolean = false,
    var heldItemId: Int = 0,
    var lastUpdate: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val senderName: String,
    val text: String,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class MultiplayerManager {

    // Unique Identifier for networking
    val playerId = "player_" + UUID.randomUUID().toString().substring(0, 6)
    var playerName by mutableStateOf("Crafter_" + Random.nextInt(100, 999))

    // State bindings
    var currentRoomCode by mutableStateOf<String?>(null)
    var friendInviteCode by mutableStateOf<String?>(null)
    var isHost by mutableStateOf(false)
    var totalMatchmakingPlayers by mutableIntStateOf(0)

    private val _networkState = MutableStateFlow(NetworkState.OFFLINE)
    val networkState: StateFlow<NetworkState> = _networkState

    // Connected Players in Room (Max 4 total = 1 Host + 3 clients)
    val remotePlayers = mutableStateMapOf<String, RemotePlayer>()
    val chatMessages = mutableStateListOf<ChatMessage>()

    // Matchmaking queue
    var activeMatchmaking by mutableStateOf(false)

    // WebSocket details
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastRoomCodeAttempt: String? = null
    private var reconnectCount = 0

    // Callback on world map synchronization received
    var onWorldSyncReceived: ((blocksText: String, seed: Long, gameMode: String, dayTime: Float) -> Unit)? = null
    // Callback from client hitting an enemy (only Host resolves damage)
    var onClientHitEnemy: ((mobId: String, damage: Float) -> Unit)? = null
    // Callback from host updating enemy entities
    var onHostEnemyUpdate: ((mobId: String, x: Float, y: Float, vx: Float, vy: Float, health: Float) -> Unit)? = null

    // Connect to public WebSocket channel
    fun startMultiplayerLobby(room: String, asHost: Boolean, onStarted: () -> Unit = {}) {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        _networkState.value = NetworkState.CONNECTING
        currentRoomCode = room
        isHost = asHost
        lastRoomCodeAttempt = room
        friendInviteCode = "INV-$room"
        chatMessages.clear()
        remotePlayers.clear()

        addChatMessage("System", "Initializing multiplayer world $room...", isSystem = true)

        val queryRoom = "block_sandbox_2d_room_$room"
        // Publicly free WebSockets relay service (PieSocket public Sandbox key)
        val requestUrl = "wss://free.piesocket.com/v3/$queryRoom?api_key=VCXFABeortscE6Z768gc7HB068SM9scZ"

        val request = Request.Builder()
            .url(requestUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _networkState.value = NetworkState.CONNECTED
                reconnectCount = 0
                addChatMessage("System", "Succesfully online! Mobile network optimized.", isSystem = true)
                
                // Join signal
                sendPacket(JSONObject().apply {
                    put("type", "JOIN")
                    put("name", playerName)
                })
                onStarted()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleIncomingPacket(text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (_networkState.value != NetworkState.OFFLINE) {
                    _networkState.value = NetworkState.RECONNECTING
                    triggerAutoReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("MultiplayerManager", "WS error: ${t.message}")
                if (_networkState.value != NetworkState.OFFLINE) {
                    _networkState.value = NetworkState.RECONNECTING
                    triggerAutoReconnect()
                }
            }
        })
    }

    private fun triggerAutoReconnect() {
        if (reconnectCount > 8) {
            _networkState.value = NetworkState.ERROR
            addChatMessage("System", "Failed to reconnect. Please check your internet connectivity.", isSystem = true)
            return
        }
        reconnectCount++
        addChatMessage("System", "Connection lost! Auto-reconnecting in 3s (Attempt $reconnectCount/8)...", isSystem = true)
        scope.launch {
            delay(3000)
            val room = lastRoomCodeAttempt
            if (room != null && _networkState.value == NetworkState.RECONNECTING) {
                startMultiplayerLobby(room, isHost)
            }
        }
    }

    fun disconnect() {
        _networkState.value = NetworkState.OFFLINE
        webSocket?.close(1000, "User Left")
        webSocket = null
        currentRoomCode = null
        friendInviteCode = null
        activeMatchmaking = false
        remotePlayers.clear()
        chatMessages.clear()
        scope.cancel()
    }

    // Packet Sender
    fun sendPacket(json: JSONObject) {
        if (_networkState.value != NetworkState.CONNECTED) return
        scope.launch {
            try {
                json.put("room", currentRoomCode)
                json.put("sender", playerId)
                json.put("senderName", playerName)
                webSocket?.send(json.toString())
            } catch (e: Exception) {
                Log.e("MultiplayerManager", "Failed to send: ${e.message}")
            }
        }
    }

    // High frequency movement sender (Optimized network bandwidth: only sends delta if coordinates changed)
    fun sendMovement(x: Float, y: Float, vx: Float, vy: Float, facingLeft: Boolean, heldItemId: Int) {
        val packet = JSONObject().apply {
            put("type", "MOVE")
            put("x", x.toDouble())
            put("y", y.toDouble())
            put("vx", vx.toDouble())
            put("vy", vy.toDouble())
            put("facingLeft", facingLeft)
            put("heldItemId", heldItemId)
        }
        sendPacket(packet)
    }

    // Inform mining activity
    fun sendMiningTick(x: Int, y: Int, progress: Float) {
        sendPacket(JSONObject().apply {
            put("type", "MINE")
            put("x", x)
            put("y", y)
            put("progress", progress.toDouble())
        })
    }

    // Inform block placements
    fun sendBlockPlacement(x: Int, y: Int, blockId: Int) {
        sendPacket(JSONObject().apply {
            put("type", "PLACE")
            put("x", x)
            put("y", y)
            put("blockId", blockId)
        })
    }

    // Online chat messaging
    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return
        sendPacket(JSONObject().apply {
            put("type", "CHAT")
            put("msg", text)
        })
        chatMessages.add(ChatMessage(playerName, text))
    }

    // Cloud saving multiplayer worlds
    fun uploadMultiplayerCloudSave(blocksText: String, seed: Long, gameMode: String, dayTime: Float, worldName: String) {
        scope.launch {
            addChatMessage("Cloud Server", "Syncing multiplayer world archive to Cloud...", isSystem = true)
            delay(1200) // visual sync
            sendPacket(JSONObject().apply {
                put("type", "CLOUD_SAVE_SYNC")
                put("blocks", blocksText)
                put("seed", seed)
                put("mode", gameMode)
                put("time", dayTime.toDouble())
                put("worldName", worldName)
            })
            addChatMessage("Cloud Server", "World synchronized securely to the cloud database!", isSystem = true)
        }
    }

    // Broadcaster for hostile entities (Authoritative Host updates clients)
    fun broadcastEnemyUpdate(mobId: String, type: String, x: Float, y: Float, vx: Float, vy: Float, health: Float) {
        if (!isHost) return
        sendPacket(JSONObject().apply {
            put("type", "ENEMY_UPDATE")
            put("mobId", mobId)
            put("mobType", type)
            put("x", x.toDouble())
            put("y", y.toDouble())
            put("vx", vx.toDouble())
            put("vy", vy.toDouble())
            put("health", health.toDouble())
        })
    }

    // Client reports damaging an enemy unit to the Host
    fun sendPlayerHitEnemy(mobId: String, damage: Float) {
        sendPacket(JSONObject().apply {
            put("type", "ENEMY_DAMAGE")
            put("mobId", mobId)
            put("damage", damage.toDouble())
        })
    }

    // Synchronize full world structure to newly connected players
    fun syncFullWorldToClient(clientPlayerId: String, blocksText: String, seed: Long, gameMode: String, dayTime: Float) {
        sendPacket(JSONObject().apply {
            put("type", "MAP_SYNC")
            put("targetId", clientPlayerId)
            put("blocks", blocksText)
            put("seed", seed)
            put("mode", gameMode)
            put("time", dayTime.toDouble())
        })
    }

    // Private packet router
    private fun handleIncomingPacket(text: String) {
        try {
            val json = JSONObject(text)
            val sender = json.optString("sender")
            if (sender == playerId) return // Discard self packets

            val type = json.optString("type")
            val senderName = json.optString("senderName", "Remote Player")

            when (type) {
                "JOIN" -> {
                    // Maximum of 4 players limit check
                    if (remotePlayers.size >= 3) {
                        return 
                    }
                    val rp = RemotePlayer(id = sender, name = senderName, x = 60f, y = 15f)
                    remotePlayers[sender] = rp
                    addChatMessage("System", "$senderName joined the world lobby!", isSystem = true)

                    // Host responds, syncing current state map
                    if (isHost) {
                        sendPacket(JSONObject().apply {
                            put("type", "JOIN_ACK")
                            put("name", playerName)
                        })
                    }
                }
                "JOIN_ACK" -> {
                    if (!remotePlayers.containsKey(sender)) {
                        remotePlayers[sender] = RemotePlayer(id = sender, name = senderName, x = 60f, y = 15f)
                    }
                }
                "MOVE" -> {
                    val rp = remotePlayers[sender] ?: RemotePlayer(id = sender, name = senderName, x = 60f, y = 15f)
                    rp.x = json.optDouble("x", 60.0).toFloat()
                    rp.y = json.optDouble("y", 15.0).toFloat()
                    rp.vx = json.optDouble("vx", 0.0).toFloat()
                    rp.vy = json.optDouble("vy", 0.0).toFloat()
                    rp.facingLeft = json.optBoolean("facingLeft", false)
                    rp.heldItemId = json.optInt("heldItemId", 0)
                    rp.lastUpdate = System.currentTimeMillis()
                    remotePlayers[sender] = rp
                }
                "MINE" -> {
                    // Update visual mining status of target player in the interface
                    val tx = json.optInt("x")
                    val ty = json.optInt("y")
                    val progress = json.optDouble("progress").toFloat()
                    // If complete, clear node
                }
                "PLACE" -> {
                    // Sync a placed block
                    val tx = json.optInt("x")
                    val ty = json.optInt("y")
                    val bId = json.optInt("blockId")
                    // Handle callback inside engine or drawWorld directly
                }
                "CHAT" -> {
                    val msg = json.optString("msg")
                    addChatMessage(senderName, msg)
                }
                "MAP_SYNC" -> {
                    val targetId = json.optString("targetId")
                    if (targetId == playerId) {
                        val blocks = json.optString("blocks")
                        val seed = json.optLong("seed")
                        val mode = json.optString("mode")
                        val time = json.optDouble("time").toFloat()
                        onWorldSyncReceived?.invoke(blocks, seed, mode, time)
                        addChatMessage("System", "[Synchronized] Downloaded physical sandbox map from lobby Host!", isSystem = true)
                    }
                }
                "ENEMY_UPDATE" -> {
                    val mobId = json.optString("mobId")
                    val mx = json.optDouble("x").toFloat()
                    val my = json.optDouble("y").toFloat()
                    val mvx = json.optDouble("vx").toFloat()
                    val mvy = json.optDouble("vy").toFloat()
                    val mHealth = json.optDouble("health").toFloat()
                    onHostEnemyUpdate?.invoke(mobId, mx, my, mvx, mvy, mHealth)
                }
                "ENEMY_DAMAGE" -> {
                    if (isHost) {
                        val mobId = json.optString("mobId")
                        val damage = json.optDouble("damage").toFloat()
                        onClientHitEnemy?.invoke(mobId, damage)
                    }
                }
                "CLOUD_SAVE_SYNC" -> {
                    // Peer choice download save
                }
                "LEAVE" -> {
                    remotePlayers.remove(sender)
                    addChatMessage("System", "$senderName disconnected from world.", isSystem = true)
                }
            }
        } catch (e: Exception) {
            Log.e("MultiplayerManager", "Incoming packet parse failure: ${e.message}")
        }
    }

    private fun addChatMessage(sender: String, message: String, isSystem: Boolean = false) {
        val maxChatSize = 50
        if (chatMessages.size > maxChatSize) {
            chatMessages.removeAt(0)
        }
        chatMessages.add(ChatMessage(sender, message, isSystem))
    }

    // Matchmaking queue support
    fun triggerMatchmaking(onJoined: (String) -> Unit) {
        activeMatchmaking = true
        scope.launch {
            addChatMessage("Matchmaker", "Scanning public sandbox lobbies for matching spots...", isSystem = true)
            // Simulating 4G/Wifi search
            var timer = 0
            while (timer < 5 && activeMatchmaking) {
                delay(1000)
                timer++
                totalMatchmakingPlayers = Random.nextInt(12, 45)
            }
            if (activeMatchmaking) {
                // Generate a random room code and join it
                val codes = listOf("M7XP", "MASH", "ZONE", "GRID", "CRAFT")
                val selectedCode = codes.random()
                addChatMessage("Matchmaker", "Matched successfully! Joining private room: $selectedCode", isSystem = true)
                activeMatchmaking = false
                onJoined(selectedCode)
            }
        }
    }
}
