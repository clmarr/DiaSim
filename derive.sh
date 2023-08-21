#!/bin/bash

export RUN_NAME=$1
export RULESET=$2
export LEX=$3

if [ -z "$4" ]; then 
	if [[ $LEX == *"FLLAP"* ]]; then export ARGS="-h"
	else export ARGS="-i"
	fi
else
	export MANFLAGS=$4
	if [[ $MANFLAGS == "-"* ]]; then export MANFLAGS=${$MANFLAGS:1}
	fi
	if [[ $MANFLAGS == *_"i"_* ]] || [[ $MANFLAGS == *_"h"_* ]]; then
		export ARGS="-"$MANFLAGS
	else
		if [[ $LEX == *"FLLAP"* ]]; then export ARGS="-h"${$MANFLAGS:1}
		else export ARGS="-i"${$MANFLAGS:1}
		fi
	fi
fi

echo $ARGS

java -cp bin DiachronicSimulator -out $RUN_NAME -rules ./$RULESET -lex ./$LEX $ARGS

