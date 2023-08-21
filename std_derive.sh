#!/bin/sh

java -cp bin DiachronicSimulator -out $1 -rules ./DiaCLEF -lex ./FLLex.txt
