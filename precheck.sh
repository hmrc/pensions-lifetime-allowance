#!/bin/bash

sbt clean scalafmt test:scalafmt it/Test/scalafmt coverage test it/test coverageReport
