#!/usr/bin/env bash
set -e
mvn compile
mkdir consoles-computers/target/natives
cd consoles-computers
make all
cd ..
mvn install