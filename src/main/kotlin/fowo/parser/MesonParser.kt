package fowo.parser

import fowo.model.Dependency

object MesonParser {
    // Handle both single and double quoted strings for dependency name and version
    private val depRegex = Regex("""dependency\s*\(\s*['"]([^'"]+)['"](?:.*?version\s*:\s*['"]([^'"]+)['"])?\s*\)""")

    fun parse(content: String): List<Dependency> {
        val deps = mutableListOf<Dependency>()
        depRegex.findAll(content).forEach { matchResult ->
            val name = matchResult.groups[1]?.value
            // Preserve the full version constraint string (e.g. ">=1.2.3") for the SAT resolver
            val ver = matchResult.groups[2]?.value?.trim()
            if (name != null && name.any { it.isLetter() }) {
                deps.add(Dependency(name, ver))
            }
        }
        return deps.distinctBy { it.name }
    }
}
