#!/bin/sh

java -cp bin/classes DiachronicSimulator -out $1 -rules ./DiaCLEF -lex ./FLLex.txt
