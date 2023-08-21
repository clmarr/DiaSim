#!/bin/sh

java -cp build/classes DiachronicSimulator -out $1 -rules ./DiaCLEF -lex ./FLLex.txt
