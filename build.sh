#!/usr/bin/env bash
trap 'printf "\nfailed to compile, complain to Jarcode on github\n\n";exit' ERR
mvn compile
mkdir -p consoles-computers/target/natives
cd consoles-computers
rm -f target/natives/libcomputerimpl.so
make all
cd ..
mvn install