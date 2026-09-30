package com.efremandrei.matzpen

import android.content.Context
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class ContentStore(private val context: Context) {
    private val baseUrl = "https://efremandrei.github.io/matzpen/feed/"
    private val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEhCLTBqk7FckXLKJ+8f3F7Tzm5kDqcVwV61xsoie+1XrGNHkQ3QfnNuG/vSogPNoXIMSmUt49B4t0JGepnvHbCg=="
    private val cacheDir = File(context.filesDir, "signed-feed")

    fun bundled(): ElectionData = ElectionData.parse(context.assets.open("content.json").bufferedReader().use { it.readText() })

    fun load(): ElectionData {
        val fallback = bundled()
        return try {
            val manifest = File(cacheDir, "manifest.json").readBytes()
            val signature = File(cacheDir, "manifest.sig").readBytes()
            val content = File(cacheDir, "content.json").readBytes()
            verify(manifest, signature, content, fallback.revision)
        } catch (_: Exception) { fallback }
    }

    fun refresh(currentRevision: Int): ElectionData? {
        val manifest = download("manifest.json", 16_384)
        val signature = download("manifest.sig", 1_024)
        verifySignature(manifest, signature)
        val meta = JSONObject(manifest.toString(Charsets.UTF_8))
        if (meta.getInt("revision") <= currentRevision) return null
        val content = download("content.json", 1_000_000)
        val data = verify(manifest, signature, content, currentRevision)
        cacheDir.mkdirs()
        writeAtomically("manifest.json", manifest)
        writeAtomically("manifest.sig", signature)
        writeAtomically("content.json", content)
        return data
    }

    private fun verify(manifest: ByteArray, signatureBytes: ByteArray, content: ByteArray, minimumRevision: Int): ElectionData {
        verifySignature(manifest, signatureBytes)
        val meta = JSONObject(manifest.toString(Charsets.UTF_8))
        require(meta.getInt("schemaVersion") == 1 && meta.getString("electionId") == "il-knesset-26")
        val hash = MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
        require(hash == meta.getString("sha256")) { "Content hash mismatch" }
        val data = ElectionData.parse(content.toString(Charsets.UTF_8))
        require(data.revision == meta.getInt("revision") && data.revision >= minimumRevision)
        return data
    }

    private fun verifySignature(manifest: ByteArray, signatureBytes: ByteArray) {
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(key)
        verifier.update(manifest)
        require(verifier.verify(signatureBytes)) { "Invalid feed signature" }
    }

    private fun download(name: String, limit: Int): ByteArray {
        val connection = URL(baseUrl + name).openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/octet-stream")
        try {
            require(connection.responseCode == 200)
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    require(output.size() <= limit)
                }
                output.toByteArray()
            }
            require(bytes.size <= limit)
            return bytes
        } finally { connection.disconnect() }
    }

    private fun writeAtomically(name: String, bytes: ByteArray) {
        val temp = File(cacheDir, "$name.tmp")
        temp.writeBytes(bytes)
        require(temp.renameTo(File(cacheDir, name)))
    }
}
