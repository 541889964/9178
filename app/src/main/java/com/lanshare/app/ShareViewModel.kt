package com.lanshare.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

data class ShareUiState(
    val running: Boolean = false,
    val url: String = "",
    val ip: String = "",
    val port: Int = 8080,
    val files: List<FileItem> = emptyList(),
    val error: String? = null,
)

data class FileItem(
    val name: String,
    val size: Long,
    val mtime: Long,
)

class ShareViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(ShareUiState())
    val ui: StateFlow<ShareUiState> = _ui.asStateFlow()

    private var server: FileShareServer? = null
    private var cm: ConnectivityManager? = null
    private var netCallback: ConnectivityManager.NetworkCallback? = null
    private val prefs = app.getSharedPreferences("lanshare", Context.MODE_PRIVATE)

    private val storageDir: File
        get() = File(getApplication<Application>().getExternalFilesDir(null), "lan-share")
            .apply { mkdirs() }

    init {
        startServer()
        registerNetworkMonitor()
    }

    fun startServer() {
        stopServer()
        val port = _ui.value.port
        val srv = FileShareServer(getApplication(), storageDir, port) { refreshFiles() }
        try {
            srv.start()
            server = srv
            val ip = currentLanIp() ?: "127.0.0.1"
            _ui.value = _ui.value.copy(running = true, ip = ip, url = "http://$ip:$port", error = null)
            refreshFiles()
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(running = false, error = e.message ?: "启动失败")
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
    }

    private fun registerNetworkMonitor() {
        val ctx = getApplication<Application>()
        cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        netCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { restartSoon() }
            override fun onLost(network: Network) { restartSoon() }
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) { restartSoon() }
        }.also { cm?.registerDefaultNetworkCallback(it) }
    }

    private fun restartSoon() {
        viewModelScope.launch { delay(600); startServer() }
    }

    private fun currentLanIp(): String? {
        try {
            for (nif in NetworkInterface.getNetworkInterfaces()) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))
                            return ip
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun refreshFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = storageDir.listFiles()?.filter { !it.name.startsWith(".") }
                ?.map { FileItem(it.name, it.length(), it.lastModified()) }
                ?.sortedByDescending { it.mtime } ?: emptyList()
            _ui.value = _ui.value.copy(files = list)
        }
    }

    fun deleteFile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            File(storageDir, name).takeIf { it.exists() }?.delete()
            refreshFiles()
        }
    }

    fun loadAnnouncement(): Announcement? {
        return try {
            val json = getApplication<Application>().assets.open("announcement.json")
                .bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val id = obj.optString("id", "default")
            if (obj.optBoolean("showOnce", true) && prefs.getBoolean("announce_read_$id", false))
                return null
            Announcement(
                id = id,
                title = obj.optString("title", "公告"),
                subtitle = obj.optString("subtitle", ""),
                content = obj.optString("content", ""),
                showOnce = obj.optBoolean("showOnce", true),
                primaryBtn = obj.optString("primaryBtn", "我知道了"),
                secondaryBtn = obj.optString("secondaryBtn", ""),
                link = obj.optString("link", ""),
            )
        } catch (_: Exception) { null }
    }

    fun markAnnouncementRead(id: String) {
        prefs.edit().putBoolean("announce_read_$id", true).apply()
    }

    override fun onCleared() {
        netCallback?.let { cm?.unregisterNetworkCallback(it) }
        stopServer()
        super.onCleared()
    }
}
