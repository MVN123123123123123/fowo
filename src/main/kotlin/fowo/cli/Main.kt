package fowo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import fowo.core.PackageManager

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.help

class Fowo : CliktCommand(name = "fowo") {
    override fun help(context: com.github.ajalt.clikt.core.Context): String = "Source-Based Package Manager for Fedora-Based Distro"
    override fun run() = Unit
}

fun main(args: Array<String>) {
    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            ProcessBuilder("asroot", "--clear").start().waitFor()
        } catch (e: Exception) {
            // Ignore errors if asroot is not installed or fails
        }
    })

    Fowo().subcommands(
        InstallCommand(),
        RegistryCommand().subcommands(
            RegistryAddCommand(),
            RegistryListCommand(),
            RegistryRemoveCommand()
        ),
        ListCommand(),
        RemoveCommand(),
        InfoCommand(),
        UpdateCommand()
    ).main(args)
}
