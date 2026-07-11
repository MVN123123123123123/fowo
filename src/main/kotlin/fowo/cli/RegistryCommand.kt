package fowo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import fowo.core.Registry
import fowo.model.RegistryEntry

class RegistryCommand : CliktCommand(name = "registry") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Manage the package registry"
    override fun run() = Unit
}

class RegistryAddCommand : CliktCommand(name = "add") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Add a package to the registry"
    val name by argument().help("Package name")
    val url by argument().help("Git repository URL")

    override fun run() {
        Registry.add(name, RegistryEntry(repoUrl = url))
        echo("Added $name to registry.")
    }
}

class RegistryListCommand : CliktCommand(name = "list") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "List packages in the registry"
    override fun run() {
        val all = Registry.listAll()
        if (all.isEmpty()) {
            echo("Registry is empty.")
            return
        }
        for ((name, entry) in all) {
            echo("$name -> ${entry.repoUrl}")
        }
    }
}

class RegistryRemoveCommand : CliktCommand(name = "remove") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Remove a package from the registry"
    val name by argument().help("Package name")

    override fun run() {
        Registry.remove(name)
        echo("Removed $name from registry.")
    }
}
