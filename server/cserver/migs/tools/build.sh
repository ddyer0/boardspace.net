#!/bin/sh
# command line build of migs.jar without Eclipse (javac 16 or later)
cd "$(dirname "$0")/.." && rm -rf bin && mkdir -p bin && \
javac -Xlint:all --release 16 -d bin $(find src -name '*.java') && \
jar --create --file migs.jar --main-class net.boardspace.migs.Migs -C bin . && echo built migs.jar
