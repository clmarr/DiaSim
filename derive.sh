#!/bin/bash

RUN_NAME = $1
RULESET = $2
LEX = $3

java -cp build/classes DerivationSimulation -out $RUN_NAME -rules ./$RULESET -lex ./$LEX

