#!/bin/bash
#make
if [ "$1" = "help" ]
then
  echo "Usage: ./run_perf.sh [numClients] [load] [middleHost] [iteration] [output_filename]"
else
  rm -f ./output.log
  java -cp .:../Server:../Client PerfRunner $1 $2 $3 $4 $5
fi

