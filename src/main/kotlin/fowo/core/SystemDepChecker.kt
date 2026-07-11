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

    fun isSystemDep(name: String): Boolean {
        // Simple heuristic: if it's in our known list, or if pkg-config says it exists on the host system.
        if (knownSystemDeps.contains(name.lowercase())) return true
        
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
