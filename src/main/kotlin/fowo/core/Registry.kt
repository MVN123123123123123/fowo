package fowo.core

import fowo.model.RegistryEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object Registry {
    private val registryFile = File("/etc/fowo/registry.json")
    private val format = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    private var entries: MutableMap<String, RegistryEntry> = mutableMapOf()

    init {
        load()
    }

    private fun load() {
        if (registryFile.exists()) {
            try {
                entries = format.decodeFromString(registryFile.readText())
            } catch (e: Exception) {
                // Initial or corrupted
            }
        }
    }

    fun save() {
        val tmpFile = File.createTempFile("fowo-registry", ".json")
        tmpFile.writeText(format.encodeToString(entries))
        
        val pb = ProcessBuilder("asroot", "fowo-update-registry", tmpFile.absolutePath)
        pb.inheritIO()
        val proc = pb.start()
        val ret = proc.waitFor()
        if (ret != 0) {
            System.err.println("Failed to save registry to /etc/fowo/registry.json. Are you sure asroot and fowo-update-registry are configured?")
        }
    }

    fun add(name: String, entry: RegistryEntry) {
        entries[name] = entry
        save()
    }

    fun remove(name: String) {
        entries.remove(name)
        save()
    }

    fun lookup(name: String): RegistryEntry? {
        return entries[name]
    }

    fun listAll(): Map<String, RegistryEntry> {
        return entries.toMap()
    }

    fun resolveCanonicalName(name: String): String {
        val entry = lookup(name)
        return if (entry?.alias != null) {
            resolveCanonicalName(entry.alias) // Recursively resolve
        } else {
            name
        }
    }
}
