#!/bin/bash
function cmd() {
  tmux send-keys -t $1 $2
}

function test1() {
  tmux send-keys -t 0 "start" C-m "queryflight,1,1" C-m
  tmux send-keys -t 1 "start" C-m "queryflight,2,1" C-m
#  cmd 0 '"start C-m "queryflight,1,1" C-m'
##  cmd 1 "start" C-m "queryflight,2,1" C-m
}


if [ $1 -eq 1 ]
  then
    test1
  else
    echo "no"
  fi
