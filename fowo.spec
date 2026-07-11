Name:           fowo
Version:        1.0.0
Release:        1%{?dist}
Summary:        Source-Based Package Manager for Fedora-Based Distros
License:        MIT
URL:            https://github.com/example/fowo
BuildArch:      noarch

Requires:       java-headless >= 21
Requires:       bubblewrap
Requires:       git
Requires:       dnf5
Requires:       keyutils

%description
Fowo is a source-based package manager designed specifically for Fedora-based distributions.
It features semantic version resolution using SAT solvers, isolated sandboxed builds using
bubblewrap, and parallel compilation using Kotlin Coroutines.

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr/share/fowo
mkdir -p $RPM_BUILD_ROOT/usr/bin

# Copy the Fat JAR
cp %{_sourcedir}/fowo-all.jar $RPM_BUILD_ROOT/usr/share/fowo/

# Copy the hardened asroot script
cp %{_sourcedir}/asroot $RPM_BUILD_ROOT/usr/bin/asroot
chmod 755 $RPM_BUILD_ROOT/usr/bin/asroot

# Create the fowo wrapper script
cat << 'EOF' > $RPM_BUILD_ROOT/usr/bin/fowo
#!/bin/bash
# Silence JNA native access warnings on modern JDKs
exec java --enable-native-access=ALL-UNNAMED -jar /usr/share/fowo/fowo-all.jar "$@"
EOF

chmod 755 $RPM_BUILD_ROOT/usr/bin/fowo

%files
/usr/share/fowo/fowo-all.jar
/usr/bin/fowo
/usr/bin/asroot

%changelog
* Fri Jul 11 2026 Developer <dev@example.com> - 1.0.0-1
- Initial release of fowo package manager
- Includes hardened asroot with kernel keyring credential storage
