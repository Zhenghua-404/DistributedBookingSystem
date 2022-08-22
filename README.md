# comp512-project

To run the RMI resource manager:

```shell script
cd Server/
./run_server.sh [<rmi_name>] # starts a single ResourceManager
./run_servers.sh # convenience script for starting multiple resource managers
```

To run the RMI client:

```shell script
cd Client
./run_client.sh [<server_hostname> [<server_rmi_name>]]
```
For local test:

```shell script
cd Server
./run_local_servers.sh
```
This script compiles `Client` and `Server` and starts a new tmux session.