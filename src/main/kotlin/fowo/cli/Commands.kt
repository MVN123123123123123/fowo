package fowo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help

class ListCommand : CliktCommand(name = "list") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "List installed packages"
    override fun run() {
        echo("List feature is not fully implemented yet.")
    }
}

class RemoveCommand : CliktCommand(name = "remove") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Remove an installed package"
    val name by argument().help("Package name")

    override fun run() {
        echo("Remove feature is not fully implemented yet.")
    }
}

class InfoCommand : CliktCommand(name = "info") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Show information about a package"
    val name by argument().help("Package name")

    override fun run() {
        echo("Info feature is not fully implemented yet.")
    }
}

class UpdateCommand : CliktCommand(name = "update") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Update an installed package"
    val name by argument().help("Package name")

    override fun run() {
        echo("Update feature is not fully implemented yet.")
    }
}
