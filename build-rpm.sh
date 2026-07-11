#!/bin/bash
set -e

# Setup rpmbuild directory structure
RPM_ROOT=$HOME/rpmbuild
mkdir -p $RPM_ROOT/{BUILD,RPMS,SOURCES,SPECS,SRPMS}

# Copy the pre-built jar to SOURCES
cp build/libs/fowo-all.jar $RPM_ROOT/SOURCES/

# Copy the hardened asroot script to SOURCES
cp asroot $RPM_ROOT/SOURCES/

# Copy the spec file to SPECS
cp fowo.spec $RPM_ROOT/SPECS/

# Build the RPM
rpmbuild -ba $RPM_ROOT/SPECS/fowo.spec

echo "RPM built successfully. Check $RPM_ROOT/RPMS/noarch/fowo-1.0.0-1*.rpm"
