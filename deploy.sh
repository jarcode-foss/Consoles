#!/usr/bin/env bash

# This script is used to deploy Consoles to my webserver. You can use this if you have access to it.

mvn deploy -Dmaven.test.skip=true | egrep -v "(^\[WARNING\])|(already added\, skipping)"
mvn deploy -pl bungee -am -Dmaven.test.skip=true | egrep -v "(^\[WARNING\])|(already added\, skipping)"