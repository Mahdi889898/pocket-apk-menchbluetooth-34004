package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class DiscoveredBtDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean
)

enum class BluetoothRole {
    NONE,
    HOST,
    CLIENT
}

data class BluetoothUiState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val role: BluetoothRole = BluetoothRole.NONE,
    val isHosting: Boolean = false,
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnectedToHost: Boolean = false,
    val statusMessage: String = "آماده اتصال بلوتوثی",
    val pairedDevices: List<DiscoveredBtDevice> = emptyList(),
    val discoveredDevices: List<DiscoveredBtDevice> = emptyList(),
    val connectedPeerAddresses: Set<String> = emptySet()
)

sealed interface BluetoothIncomingEvent {
    data class PeerConnected(val address: String, val deviceName: String) : BluetoothIncomingEvent
    data class PeerDisconnected(val address: String) : BluetoothIncomingEvent
    data class PacketReceived(val fromAddress: String, val packet: BluetoothPacket) : BluetoothIncomingEvent
    data class ConnectionFailed(val reason: String) : BluetoothIncomingEvent
}

class BluetoothMenchManager(private val context: Context) {

    companion object {
        private const val SERVICE_NAME = "MenchBluetoothTemple"
        val APP_UUID: UUID = UUID.fromString("e4a8f102-7c93-4b51-9a6e-5d1234567890")
        const val MAX_CLIENTS = 3
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(
        BluetoothUiState(
            isSupported = adapter != null,
            isEnabled = adapter?.isEnabled == true
        )
    )
    val state: StateFlow<BluetoothUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BluetoothIncomingEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BluetoothIncomingEvent> = _events.asSharedFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var acceptJob: Job? = null
    private var clientSocket: BluetoothSocket? = null
    private var clientWriter: PrintWriter? = null

    private data class PeerSession(
        val address: String,
        val socket: BluetoothSocket,
        val writer: PrintWriter,
        val readerJob: Job
    )

    private val hostPeers = ConcurrentHashMap<String, PeerSession>()
    private var receiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    if (device != null && hasConnectPermission()) {
                        val address = device.address ?: return
                        val name = device.name?.takeIf { it.isNotBlank() } ?: "دستگاه ناشناس ($address)"
                        val entry = DiscoveredBtDevice(name = name, address = address, isPaired = false)
                        _state.update { current ->
                            if (current.discoveredDevices.any { it.address == address } ||
                                current.pairedDevices.any { it.address == address }
                            ) {
                                current
                            } else {
                                current.copy(discoveredDevices = current.discoveredDevices + entry)
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _state.update { it.copy(isScanning = false, statusMessage = "جستجو به پایان رسید") }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    refreshAdapterState()
                }
            }
        }
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun hasAllPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshAdapterState() {
        val supported = adapter != null
        val enabled = adapter?.isEnabled == true
        _state.update {
            it.copy(
                isSupported = supported,
                isEnabled = enabled,
                statusMessage = when {
                    !supported -> "این دستگاه فاقد سخت‌افزار بلوتوث است"
                    !enabled -> "بلوتوث خاموش است؛ لطفاً روشن کنید"
                    else -> it.statusMessage
                }
            )
        }
        if (supported && enabled && hasConnectPermission()) {
            loadPairedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        val btAdapter = adapter ?: return
        if (!hasConnectPermission()) return
        val paired = runCatching {
            btAdapter.bondedDevices.orEmpty().map { device ->
                DiscoveredBtDevice(
                    name = device.name?.takeIf { it.isNotBlank() } ?: "دستگاه جفت‌شده",
                    address = device.address,
                    isPaired = true
                )
            }.sortedBy { it.name }
        }.getOrDefault(emptyList())
        _state.update { it.copy(pairedDevices = paired) }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val btAdapter = adapter
        if (btAdapter == null || !btAdapter.isEnabled) {
            _state.update { it.copy(statusMessage = "ابتدا بلوتوث را روشن کنید") }
            return
        }
        if (!hasScanPermission() || !hasConnectPermission()) {
            _state.update { it.copy(statusMessage = "دسترسی بلوتوث تایید نشده است") }
            return
        }
        registerReceiverIfNeeded()
        loadPairedDevices()
        runCatching {
            if (btAdapter.isDiscovering) btAdapter.cancelDiscovery()
            _state.update {
                it.copy(
                    isScanning = true,
                    discoveredDevices = emptyList(),
                    statusMessage = "در حال جستجوی گوشی‌های اطراف..."
                )
            }
            val started = btAdapter.startDiscovery()
            if (!started) {
                _state.update {
                    it.copy(
                        isScanning = false,
                        statusMessage = "امکان جستجو نبود؛ از لیست جفت‌شده انتخاب کنید"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val btAdapter = adapter
        if (btAdapter != null && hasScanPermission()) {
            runCatching {
                if (btAdapter.isDiscovering) btAdapter.cancelDiscovery()
            }
        }
        _state.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    fun startHosting() {
        val btAdapter = adapter
        if (btAdapter == null || !btAdapter.isEnabled) {
            _state.update { it.copy(statusMessage = "ابتدا بلوتوث را روشن کنید") }
            return
        }
        if (!hasConnectPermission()) {
            _state.update { it.copy(statusMessage = "دسترسی بلوتوث تایید نشده است") }
            return
        }
        stopAllConnections()
        _state.update {
            it.copy(
                role = BluetoothRole.HOST,
                isHosting = true,
                statusMessage = "اتاق بلوتوثی ساخته شد! منتظر اتصال دوستان..."
            )
        }

        acceptJob = scope.launch {
            try {
                serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, APP_UUID)
                while (isActive && hostPeers.size < MAX_CLIENTS) {
                    val socket = serverSocket?.accept() ?: break
                    val remote = socket.remoteDevice
                    val address = remote.address ?: UUID.randomUUID().toString()
                    val name = runCatching { remote.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "دوست بلوتوثی"
                    val writer = PrintWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8), true)
                    val readerJob = launchPeerReader(address, socket, isHostSide = true)
                    hostPeers[address] = PeerSession(address, socket, writer, readerJob)

                    _state.update {
                        it.copy(
                            connectedPeerAddresses = hostPeers.keys.toSet(),
                            statusMessage = "${hostPeers.size} بازیکن بلوتوثی متصل شد"
                        )
                    }
                    _events.tryEmit(BluetoothIncomingEvent.PeerConnected(address, name))
                }
            } catch (e: Exception) {
                if (isActive && _state.value.isHosting) {
                    _state.update { it.copy(statusMessage = "پذیرش اتصال متوقف شد") }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToHost(address: String) {
        val btAdapter = adapter
        if (btAdapter == null || !btAdapter.isEnabled) {
            _events.tryEmit(BluetoothIncomingEvent.ConnectionFailed("بلوتوث خاموش است"))
            return
        }
        if (!hasConnectPermission()) {
            _events.tryEmit(BluetoothIncomingEvent.ConnectionFailed("دسترسی بلوتوث تایید نشده است"))
            return
        }
        stopDiscovery()
        stopAllConnections()

        _state.update {
            it.copy(
                role = BluetoothRole.CLIENT,
                isConnecting = true,
                statusMessage = "در حال اتصال به میزبان..."
            )
        }

        scope.launch {
            try {
                val device = btAdapter.getRemoteDevice(address)
                val socket = device.createRfcommSocketToServiceRecord(APP_UUID)
                socket.connect()
                clientSocket = socket
                clientWriter = PrintWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8), true)

                val hostName = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "میزبان"
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnectedToHost = true,
                        connectedPeerAddresses = setOf(address),
                        statusMessage = "متصل به $hostName"
                    )
                }
                _events.tryEmit(BluetoothIncomingEvent.PeerConnected(address, hostName))
                launchPeerReader(address, socket, isHostSide = false)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        role = BluetoothRole.NONE,
                        isConnecting = false,
                        isConnectedToHost = false,
                        statusMessage = "اتصال ناموفق بود؛ مطمئن شوید میزبان اتاق ساخته است"
                    )
                }
                _events.tryEmit(
                    BluetoothIncomingEvent.ConnectionFailed("اتصال به دستگاه برقرار نشد")
                )
            }
        }
    }

    private fun launchPeerReader(address: String, socket: BluetoothSocket, isHostSide: Boolean): Job {
        return scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    val packet = BluetoothPacket.parse(line)
                    if (packet != null) {
                        _events.emit(BluetoothIncomingEvent.PacketReceived(address, packet))
                    }
                }
            } catch (_: Exception) {
                // Socket closed or dropped
            } finally {
                if (isHostSide) {
                    hostPeers.remove(address)?.let {
                        runCatching { it.socket.close() }
                    }
                    _state.update {
                        it.copy(
                            connectedPeerAddresses = hostPeers.keys.toSet(),
                            statusMessage = "یکی از بازیکنان قطع شد (${hostPeers.size} متصل)"
                        )
                    }
                } else {
                    runCatching { clientSocket?.close() }
                    clientSocket = null
                    clientWriter = null
                    _state.update {
                        it.copy(
                            isConnectedToHost = false,
                            connectedPeerAddresses = emptySet(),
                            statusMessage = "ارتباط با میزبان قطع شد"
                        )
                    }
                }
                _events.emit(BluetoothIncomingEvent.PeerDisconnected(address))
            }
        }
    }

    fun sendToPeer(address: String, packet: BluetoothPacket) {
        val wire = packet.toWireString()
        scope.launch {
            val peer = hostPeers[address] ?: return@launch
            runCatching {
                peer.writer.println(wire)
                peer.writer.flush()
            }
        }
    }

    fun broadcastFromHost(packet: BluetoothPacket) {
        val wire = packet.toWireString()
        scope.launch {
            hostPeers.values.forEach { peer ->
                runCatching {
                    peer.writer.println(wire)
                    peer.writer.flush()
                }
            }
        }
    }

    fun sendToHost(packet: BluetoothPacket) {
        val wire = packet.toWireString()
        scope.launch {
            runCatching {
                clientWriter?.println(wire)
                clientWriter?.flush()
            }
        }
    }

    fun stopAllConnections() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null

        hostPeers.values.forEach { session ->
            session.readerJob.cancel()
            runCatching { session.socket.close() }
        }
        hostPeers.clear()

        runCatching { clientSocket?.close() }
        clientSocket = null
        clientWriter = null

        _state.update {
            it.copy(
                role = BluetoothRole.NONE,
                isHosting = false,
                isConnecting = false,
                isConnectedToHost = false,
                connectedPeerAddresses = emptySet(),
                statusMessage = "آماده اتصال بلوتوثی"
            )
        }
    }

    private fun registerReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            discoveryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    fun release() {
        stopDiscovery()
        stopAllConnections()
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(discoveryReceiver) }
            receiverRegistered = false
        }
        scope.cancel()
    }
}
