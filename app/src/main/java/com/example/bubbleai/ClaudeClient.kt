package com.example.bubbleai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ClaudeClient {
    private const val SYSTEM = "Kamu NotMe AI, asisten layar. Lihat tangkapan layar, lalu jawab atau jelaskan isinya " +
        "dengan ringkas dalam bahasa pengguna. Jika layar terlihat seperti ujian, tes seleksi, atau tes berpengawas, " +
        "jangan beri jawaban; katakan kamu tidak bisa membantu untuk itu."

    fun ask(apiKey: String, jpegBase64: String): String {
        val body = JSONObject()
            .put("model", "claude-sonnet-5-5")
            .put("max_tokens", 800)
            .put("system", SYSTEM)
            .put("messages", JSONArray().put(JSONObject()
                .put("role", "user")
                .put("content", JSONArray()
                    .put(JSONObject().put("type", "image").put("source", JSONObject()
                        .put("type", "base64").put("media_type", "image/jpeg").put("data", jpegBase64)))
                    .put(JSONObject().put("type", "text").put("text", "Jelaskan/jawab isi layar ini.")))))
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 20000; c.readTimeout = 60000
        c.setRequestProperty("content-type", "application/json")
        c.setRequestProperty("x-api-key", apiKey)
        c.setRequestProperty("anthropic-version", "2023-06-01")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        val txt = stream.bufferedReader().readText()
        if (c.responseCode !in 200..299) return "Error ${c.responseCode}: $txt"
        val arr = JSONObject(txt).getJSONArray("content")
        return (0 until arr.length()).map { arr.getJSONObject(it) }
            .filter { it.optString("type") == "text" }.joinToString("\n") { it.getString("text") }
    }
}
