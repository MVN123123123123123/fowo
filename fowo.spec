Name:           fowo
Version:        1.0.0
Release:        1%{?dist}
Summary:        Source-Based Package Manager for Fedora-Based Distros
License:        GPL-3.0-or-later
URL:            https://github.com/MVN123123123123123/fowo
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

# Copy the pre-built Fat JAR from the SOURCES directory
cp %{_sourcedir}/fowo-all.jar $RPM_BUILD_ROOT/usr/share/fowo/

# Copy the hardened asroot script from the SOURCES directory
cp %{_sourcedir}/asroot $RPM_BUILD_ROOT/usr/bin/asroot
chmod 755 $RPM_BUILD_ROOT/usr/bin/asroot

# Copy the registry update script from the SOURCES directory
cp %{_sourcedir}/fowo-update-registry $RPM_BUILD_ROOT/usr/bin/fowo-update-registry
chmod 755 $RPM_BUILD_ROOT/usr/bin/fowo-update-registry

# Copy the installed package update script from the SOURCES directory
cp %{_sourcedir}/fowo-update-installed $RPM_BUILD_ROOT/usr/bin/fowo-update-installed
chmod 755 $RPM_BUILD_ROOT/usr/bin/fowo-update-installed

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
/usr/bin/fowo-update-registry
/usr/bin/fowo-update-installed

%changelog
%autochangelog