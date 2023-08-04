#!/usr/bin/env bash

sbt clean compile scalastyleAll coverage test IntegrationTest/test coverageOff coverageReport dependencyUpdates
