all: java.policy compile-server-rmi

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: Server/Interface/*.java Server/RMI/InvalidTransactionException.java Server/RMI/TransactionAbortedException.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/*.java Server/RMI/InvalidTransactionException.java Server/RMI/TransactionAbortedException.java
	jar cvf RMIInterface.jar Server/Interface/*.class Server/RMI/InvalidTransactionException.class Server/RMI/TransactionAbortedException.class

compile-server-rmi: Server/RMI/*.java Server/Interface/*.java Server/Common/*.java Server/LockManager/*.java
	javac Server/RMI/*.java Server/Interface/*.java Server/Common/*.java Server/LockManager/*.java

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class Server/LockManager/*.class
	rm -f RMIInterface.jar
	rm -f java.policy 
