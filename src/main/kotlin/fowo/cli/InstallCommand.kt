package fowo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import fowo.core.PackageManager

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.types.enum
import fowo.model.BuildSystem

class InstallCommand : CliktCommand(name = "install") {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Install a package from a git repository URL"
    val url by argument().help("The git repository URL")
    val buildSystem by option("--build-system").enum<BuildSystem>().help("Force a specific build system (CMAKE, MESON, AUTOTOOLS, CARGO, MAKE)")

    override fun run() {
        echo("Starting installation from $url")
        PackageManager.install(url, buildSystemHint = buildSystem)
    }
}
