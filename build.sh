#!/usr/bin/env bash
set -e
mvn compile
mkdir -p consoles-computers/target/natives
cd consoles-computers
rm -f target/natives/libcomputerimpl.so
make all
cd ..
mvn install