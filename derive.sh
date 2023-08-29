#!/bin/sh

export ARGS=$*
java -cp bin DiachronicSimulator $ARGS
