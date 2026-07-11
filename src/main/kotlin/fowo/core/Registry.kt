package fowo.core

import fowo.model.RegistryEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object Registry {
    private val registryFile = File(System.getProperty("user.home"), ".config/fowo/registry.json")
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
        registryFile.parentFile.mkdirs()
        registryFile.writeText(format.encodeToString(entries))
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
}
