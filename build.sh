#!/usr/bin/env bash

# This script is used to build and package Consoles locally

mvn package -Dmaven.test.skip=true | egrep -v "(^\[WARNING\])|(already added\, skipping)"
mvn package -pl bungee -am -Dmaven.test.skip=true | egrep -v "(^\[WARNING\])|(already added\, skipping)"
