# fowo
an source-based package manager. fowo is the initialism for fedora (for normal people) or FedOwOra for degenerates like me >w<

## The Concept

Instead of using traditional recipes, fowo discovers versions and builds directly from the source code repositories.

- **Discovery**: When you want to install a package, fowo queries a central registry to find the canonical repository URL and branch/tag.
- **Scanning**: It clones the repository and scans for available tags (versions) and the `BUILD` file or `meson.build` file to identify dependencies.
- **Resolution**: It uses a SAT solver to figure out the exact versions of all direct and indirect dependencies required to build the target package.
- **Building**: It builds all packages in the correct order (handling parallel subgraphs) using `asroot`.

**j4f project. don't take this seriously.**
*still,if you do use it on ur code, gpl buddy,no closed source.*