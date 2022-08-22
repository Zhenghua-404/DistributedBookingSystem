SERVER=(Flights Cars Rooms)
PORTS=(30101 30102 30103)
CLIENT_PATH="$(pwd)/../Client"
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

tmux new-session \; \
  split-window -h \; \
	split-window -v \; \
	split-window -v \; \
  split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 1 \; \
	send-keys "cd $(pwd); ./run_server.sh ${SERVER[0]} ${PORTS[0]}" C-m \; \
	select-pane -t 2 \; \
	send-keys "cd $(pwd); ./run_server.sh ${SERVER[1]} ${PORTS[1]}" C-m \; \
	select-pane -t 3 \; \
	send-keys "cd $(pwd); ./run_server.sh ${SERVER[2]} ${PORTS[2]}" C-m \; \
	select-pane -t 4 \; \
	send-keys "cd $(pwd); ./run_middleware.sh localhost localhost localhost" C-m \; \
  # select-pane -t 0 \; \
  # send-keys "cd ${CLIENT_PATH}; ./run_client.sh localhost Server" C-m \; \
  # split-pane -v \; \
  # send-keys "cd ${CLIENT_PATH}; ./run_client.sh localhost Server" C-m \; \
  # split-pane -v \; \
