package fowo.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class InstalledEntry(val version: String, val timestamp: Long)

object InstalledDatabase {
    private val dbFile = File("/etc/fowo/installed.json")
    private val format = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    private var entries: MutableMap<String, InstalledEntry> = mutableMapOf()

    init {
        load()
    }

    private fun load() {
        if (dbFile.exists()) {
            try {
                entries = format.decodeFromString(dbFile.readText())
            } catch (e: Exception) {
                // Initial or corrupted, just start fresh
                entries = mutableMapOf()
            }
        }
    }

    fun isInstalled(name: String): Boolean {
        return entries.containsKey(name)
    }

    fun addInstalled(name: String, version: String) {
        entries[name] = InstalledEntry(version, System.currentTimeMillis())
        save()
    }

    fun save() {
        val tmpFile = File.createTempFile("fowo-installed", ".json")
        tmpFile.writeText(format.encodeToString(entries))
        
        try {
            val pb = ProcessBuilder("asroot", "fowo-update-installed", tmpFile.absolutePath)
            pb.inheritIO()
            val p = pb.start()
            if (p.waitFor() != 0) {
                System.err.println("Warning: failed to update installed.json. Please run manually or check permissions.")
            }
        } catch (e: Exception) {
            System.err.println("Warning: asroot failed to run: ${e.message}")
        } finally {
            tmpFile.delete()
        }
    }
}
