package com.lanshare.app

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class FileShareServer(
    private val ctx: Context,
    private val rootDir: File,
    port: Int,
    private val onFileChanged: () -> Unit
) : NanoHTTPD(port) {

    private val htmlPage: String by lazy { buildIndexPage() }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        val params = session.parameters
        return when {
            uri == "/" || uri == "/index.html" -> html(htmlPage)
            uri == "/api/files" -> json(listFilesJson())
            uri == "/api/info" -> json("""{"port":$listeningPort,"ips":["${currentIp()}"],"dir":"${rootDir.absolutePath}"}""")
            uri == "/api/announcement" -> ann()
            uri == "/api/wallpapers" -> wpList()
            uri.startsWith("/wallpaper/") -> wpFile(uri.substringAfter("/wallpaper/"))
            uri == "/api/upload" && session.method == Method.POST -> upload(session)
            uri == "/api/delete" && session.method == Method.POST -> del(params)
            uri == "/f" -> file(params)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404")
        }
    }

    private fun currentIp(): String {
        try {
            for (nif in java.net.NetworkInterface.getNetworkInterfaces()) {
                if (!nif.isUp || nif.isLoopback) continue
                for (a in nif.inetAddresses) {
                    if (!a.isLoopbackAddress && a is java.net.Inet4Address) {
                        val ip = a.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))
                            return ip
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun html(s: String) = newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", s)
    private fun json(s: String) = newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", s)

    private fun ann(): Response = try {
        json(ctx.assets.open("announcement.json").bufferedReader().use { it.readText() })
    } catch (_: Exception) { json("null") }

    private fun wpList(): Response = try {
        json(ctx.assets.open("wallpapers.json").bufferedReader().use { it.readText() })
    } catch (_: Exception) { json("""{"count":0,"items":[]}""") }

    private fun wpFile(name: String): Response {
        val safe = name.replace("..", "").replace("/", "")
        return try {
            val input = ctx.assets.open("wallpapers/$safe")
            val mime = when {
                safe.endsWith(".png", true) -> "image/png"
                safe.endsWith(".webp", true) -> "image/webp"
                safe.endsWith(".gif", true) -> "image/gif"
                else -> "image/jpeg"
            }
            newChunkedResponse(Response.Status.OK, mime, input).apply {
                addHeader("Cache-Control", "public, max-age=86400")
            }
        } catch (_: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
        }
    }

    private fun listFilesJson(): String {
        val files = rootDir.listFiles()?.filter { !it.name.startsWith(".") }?.map {
            """{"name":"${it.name.replace("\"", "\\\"")}","size":${it.length()},"mtime":${it.lastModified()}}"""
        } ?: emptyList()
        return "[${files.joinToString(",")}]"
    }

    private fun upload(session: IHTTPSession): Response {
        val raw = session.parameters["name"]?.firstOrNull() ?: return jErr("缺少文件名")
        val safe = safeName(raw) ?: return jErr("文件名非法")
        val tmp = File(rootDir, ".upload-${System.nanoTime()}")
        return try {
            session.inputStream.use { i -> tmp.outputStream().use { i.copyTo(it) } }
            val target = unique(safe)
            tmp.renameTo(target)
            onFileChanged()
            json("""{"ok":true,"name":"${target.name}"}""")
        } catch (e: Exception) {
            tmp.delete()
            jErr("上传失败: ${e.message}")
        }
    }

    private fun del(params: Map<String, List<String>>): Response {
        val name = params["name"]?.firstOrNull() ?: return jErr("缺少文件名")
        val safe = safeName(name) ?: return jErr("文件名非法")
        val f = File(rootDir, safe)
        return if (f.exists() && f.delete()) { onFileChanged(); json("""{"ok":true}""") } else jErr("删除失败")
    }

    private fun file(params: Map<String, List<String>>): Response {
        val name = params["name"]?.firstOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "缺少 name")
        val safe = safeName(name)
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "非法")
        val f = File(rootDir, safe)
        if (!f.exists() || !f.isFile)
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "文件不存在")
        val mime = mime(f.name)
        val dl = params["dl"]?.firstOrNull() == "1"
        val resp = newChunkedResponse(Response.Status.OK, mime, FileInputStream(f))
        if (dl) resp.addHeader("Content-Disposition", "attachment; filename*=UTF-8''${enc(f.name)}")
        return resp
    }

    private fun safeName(raw: String): String? {
        val n = raw.replace("\\", "/").substringAfterLast("/")
            .filter { it >= ' ' && it != '\u007f' }.trim().trim('.')
        if (n.isEmpty() || n == "." || n == "..") return null
        return if (n.toByteArray().size > 200) n.take(120) else n
    }

    private fun unique(name: String): File {
        val base = File(rootDir, name)
        if (!base.exists()) return base
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 1
        while (true) {
            val f = File(rootDir, "$stem($i)$ext")
            if (!f.exists()) return f
            i++
        }
    }

    private fun jErr(msg: String) =
        newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", """{"ok":false,"error":"$msg"}""")

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun mime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "html","htm" -> "text/html; charset=utf-8"
        "css" -> "text/css; charset=utf-8"
        "js" -> "application/javascript; charset=utf-8"
        "json" -> "application/json; charset=utf-8"
        "png" -> "image/png"
        "jpg","jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "txt","md","log" -> "text/plain; charset=utf-8"
        else -> "application/octet-stream"
    }

    private fun buildIndexPage(): String = INDEX_HTML
}
