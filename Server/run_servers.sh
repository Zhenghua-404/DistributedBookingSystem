#!/bin/bash
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
echo "${GREEN}Compiling Client${NC}"
make -C ../Client
if [ $? -ne 0 ]
then
  echo "${RED}Unable to compile Client${NC}"
  exit 1
fi
echo "${GREEN}Compiling Server${NC}"
make
if [ $? -ne 0 ]
then
  echo "${RED}Unable to compile Server${NC}"
  exit 1
fi

MACHINES=(lab2-21 lab2-22 lab2-23 lab2-19)

tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 1 \; \
	send-keys "ssh -t ${MACHINES[0]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Flights\"" C-m \; \
	select-pane -t 2 \; \
	send-keys "ssh -t ${MACHINES[1]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Cars\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t ${MACHINES[2]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Rooms\"" C-m \; \
	select-pane -t 0 \; \
	send-keys "ssh -t ${MACHINES[3]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; sleep .5s; ./run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]}\"" C-m \
