package fowo.core

object SystemDepChecker {
    private val knownSystemDeps = setOf(
        "wayland", "wayland-server", "wayland-client", "wayland-protocols", "libdrm", "mesa",
        "libinput", "systemd", "glibc", "cairo", "pango", "pangocairo",
        "pixman-1", "xcursor", "gbm", "gio-2.0", "xkbcommon", "uuid",
        "openssl", "gnutls", "dbus", "glib2", "xcb", "xcb-render",
        "xcb-xfixes", "xcb-icccm", "xcb-composite", "xcb-res", "xcb-errors",
        "re2", "muparser", "lcms2", "gl", "egl", "glesv2", "threads"
    )

    // Windows-only libraries that should be silently ignored on a Fedora-based system
    private val windowsOnlyDeps = setOf(
        "gdi32", "user32", "kernel32", "advapi32", "shell32", "ole32", "oleaut32",
        "ws2_32", "mswsock", "comdlg32", "comctl32", "imm32", "winmm", "shlwapi",
        "uuid", "dwmapi", "uxtheme", "d2d1", "d3d11", "d3d12", "dxgi", "dwrite",
        "dcomp", "windowscodecs", "setupapi", "cfgmgr32", "crypt32", "secur32",
        "bcrypt", "ncrypt", "userenv", "psapi", "iphlpapi", "mfplat", "mf",
        "appleframeworks"
    )

    fun isSystemDep(name: String): Boolean {
        val lower = name.lowercase()
        // Skip known system deps and Windows-only deps
        if (knownSystemDeps.contains(lower)) return true
        if (windowsOnlyDeps.contains(lower)) return true
        
        return isInstalledViaPkgConfig(name)
    }

    fun isInstalledViaPkgConfig(name: String): Boolean {
        return try {
            val proc = ProcessBuilder("pkg-config", "--exists", name).start()
            proc.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun isInstalledViaDnf5(name: String): Boolean {
        return try {
            val proc = ProcessBuilder("dnf5", "list", "installed", name)
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor() == 0 && output.contains("Installed")
        } catch (e: Exception) {
            false
        }
    }

    fun installSystemDep(name: String): Boolean {
        println("Installing system dependency: $name via dnf5")
        // Mapping typical pkg-config names to fedora package names
        val pkgName = when(name.lowercase()) {
            "xkbcommon" -> "libxkbcommon-devel"
            "wayland-server", "wayland-client" -> "wayland-devel"
            "cairo" -> "cairo-devel"
            "pango", "pangocairo" -> "pango-devel"
            "libdrm" -> "libdrm-devel"
            "libinput" -> "libinput-devel"
            "gbm" -> "mesa-libgbm-devel"
            "gio-2.0", "glib-2.0", "glib2" -> "glib2-devel"
            "xcb" -> "libxcb-devel"
            "pixman-1" -> "pixman-devel"
            else -> "$name-devel" // Fallback guess
        }
        return try {
            val proc = ProcessBuilder("asroot", "dnf5", "install", "-y", pkgName).inheritIO().start()
            proc.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}

