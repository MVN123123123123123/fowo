package fowo.core.resolver

import fowo.core.Registry
import fowo.core.SystemDepChecker
import io.github.z4kn4fein.semver.constraints.toConstraintOrNull
import org.sat4j.core.VecInt
import org.sat4j.minisat.SolverFactory
import org.sat4j.specs.ContradictionException
import org.sat4j.specs.ISolver

class SatResolver {
    private val solver: ISolver = SolverFactory.newDefault()
    private var nextVarId = 1
    
    // Map of (PackageName, Version/Commit) to SAT variable ID
    private val varMap = mutableMapOf<Pair<String, String>, Int>()
    private val reverseVarMap = mutableMapOf<Int, Pair<String, String>>()
    
    // The universe of versions we have discovered so far
    private val packageVersions = mutableMapOf<String, List<GitVersion>>()

    // Dependency edges: (pkg, commitHash) -> list of source dep names
    // This is carried forward so PackageBuilder doesn't need to re-extract
    private val dependencyEdges = mutableMapOf<Pair<String, String>, List<String>>()
    
    // Visited packages to avoid infinite loops during exploration
    private val exploredPackages = mutableSetOf<String>()

    // Track whether a contradiction was detected during exploration
    private var contradicted = false

    init {
        solver.newVar(10000)
    }

    private fun getVarId(pkg: String, ver: String): Int {
        val key = pkg to ver
        return varMap.getOrPut(key) {
            val id = nextVarId++
            reverseVarMap[id] = key
            id
        }
    }

    data class ResolutionResult(
        val versions: Map<String, GitVersion>,
        val edges: Map<String, List<String>>  // pkg -> list of source dep names
    )

    fun resolve(rootPkg: String, rootRepoUrl: String, rootBranchOrTag: String? = null, rootBuildSystemHint: fowo.model.BuildSystem? = null): ResolutionResult? {
        explorePackage(rootPkg, rootRepoUrl, rootBuildSystemHint)
        
        if (contradicted) {
            System.err.println("Unsatisfiable dependency constraints detected during exploration.")
            return null
        }

        try {
            // 1. Uniqueness constraint: For each package, AT MOST ONE version can be installed.
            for ((pkgName, versions) in packageVersions) {
                for (i in 0 until versions.size) {
                    for (j in i + 1 until versions.size) {
                        val v1 = getVarId(pkgName, versions[i].commitHash)
                        val v2 = getVarId(pkgName, versions[j].commitHash)
                        solver.addClause(VecInt(intArrayOf(-v1, -v2)))
                    }
                }
            }

            // 2. We MUST install the root package
            val rootVersions = packageVersions[rootPkg] ?: return null
            if (rootBranchOrTag != null) {
                val specificVersion = rootVersions.find { it.tag == rootBranchOrTag || it.commitHash == rootBranchOrTag }
                if (specificVersion != null) {
                    solver.addClause(VecInt(intArrayOf(getVarId(rootPkg, specificVersion.commitHash))))
                } else {
                    return null // Specific branch/tag not found
                }
            } else {
                // Must pick at least one root version
                val rootVec = VecInt(rootVersions.size)
                for (v in rootVersions) {
                    rootVec.push(getVarId(rootPkg, v.commitHash))
                }
                solver.addClause(rootVec)
            }
        } catch (e: ContradictionException) {
            System.err.println("Unsatisfiable dependency constraints detected (Contradiction).")
            return null
        }

        // Solve
        if (solver.isSatisfiable()) {
            val model = solver.model()
            val versionResult = mutableMapOf<String, GitVersion>()
            for (variable in model) {
                if (variable > 0) {
                    // Only look up variables we actually allocated — solver.model() returns
                    // all variables up to newVar(10000), most of which are not in our map
                    val entry = reverseVarMap[variable] ?: continue
                    val (pkg, hash) = entry
                    val gitVer = packageVersions[pkg]?.find { it.commitHash == hash }
                    if (gitVer != null) {
                        versionResult[pkg] = gitVer
                    }
                }
            }

            // Build the edges map for the selected versions
            val edgeResult = mutableMapOf<String, List<String>>()
            for ((pkg, gitVer) in versionResult) {
                val key = pkg to gitVer.commitHash
                edgeResult[pkg] = dependencyEdges[key] ?: emptyList()
            }

            return ResolutionResult(versionResult, edgeResult)
        }

        return null
    }

    private fun explorePackage(pkgName: String, repoUrl: String, buildSystemHint: fowo.model.BuildSystem? = null) {
        if (contradicted) return
        if (!exploredPackages.add(pkgName)) return
        
        println("==> Discovering versions for $pkgName...")
        val versions = VersionScanner.scanVersions(pkgName, repoUrl)
        packageVersions[pkgName] = versions
        
        for (gitVer in versions) {
            if (contradicted) return

            val deps = DependencyExtractor.extractDependenciesAt(pkgName, gitVer.commitHash, buildSystemHint)
            
            // Build dependency constraints
            // pkg_ver => (dep1_verX OR dep1_verY) AND (dep2_verA OR ...)
            val pkgVarId = getVarId(pkgName, gitVer.commitHash)

            // Track source dependency edges for this version
            val sourceDepsForVersion = mutableListOf<String>()
            
            for (dep in deps) {
                if (SystemDepChecker.isSystemDep(dep.name)) continue // system deps are handled via dnf5, not SAT solver
                
                // We need to find the repo URL for this dependency
                var regEntry = Registry.lookup(dep.name)
                if (regEntry == null) {
                    System.err.println("Dependency ${dep.name} for $pkgName is not in registry!")
                    System.err.print("Please provide a Git repository URL for ${dep.name} (or leave empty to skip): ")
                    val providedUrl = readlnOrNull()?.trim()
                    if (!providedUrl.isNullOrEmpty()) {
                        regEntry = fowo.model.RegistryEntry(repoUrl = providedUrl)
                        Registry.add(dep.name, regEntry)
                    }
                }

                if (regEntry != null) {
                    explorePackage(dep.name, regEntry.repoUrl, regEntry.buildSystemHint)
                    if (contradicted) return
                    
                    val depVersions = packageVersions[dep.name] ?: emptyList()
                    val constraint = dep.version?.toConstraintOrNull()
                    
                    val validDepVersions = depVersions.filter {
                        if (constraint != null && it.semver != null) {
                            constraint.isSatisfiedBy(it.semver)
                        } else {
                            true // If no constraint or dep version has no semver, assume valid
                        }
                    }
                    
                    try {
                        if (validDepVersions.isEmpty()) {
                            // This version of the package has unsatisfiable dependency, so we forbid it.
                            solver.addClause(VecInt(intArrayOf(-pkgVarId)))
                        } else {
                            val clause = VecInt()
                            clause.push(-pkgVarId) // NOT pkg_ver
                            for (v in validDepVersions) {
                                clause.push(getVarId(dep.name, v.commitHash)) // OR dep_ver
                            }
                            solver.addClause(clause)
                            sourceDepsForVersion.add(dep.name)
                        }
                    } catch (e: ContradictionException) {
                        // The solver is now in a contradicted state — no solution possible
                        contradicted = true
                        return
                    }
                } else {
                    try {
                        solver.addClause(VecInt(intArrayOf(-pkgVarId)))
                    } catch (e: ContradictionException) {
                        contradicted = true
                        return
                    }
                }
            }

            dependencyEdges[pkgName to gitVer.commitHash] = sourceDepsForVersion
        }
    }
}
