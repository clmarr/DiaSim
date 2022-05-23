#!/bin/sh

java -cp build/classes DerivationSimulation -out $1 -rules ./DiaCLEF -lex ./FLLex.txt
