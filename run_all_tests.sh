#!/usr/bin/env bash

sbt clean compile scalastyleAll coverage test it/Test/test coverageOff coverageReport dependencyUpdates
