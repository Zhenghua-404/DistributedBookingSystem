all: compile-runner

compile-runner: compile-util compile-transaction compile-perfcli
	javac -cp ".:../Server:../Client" PerfRunner.java

compile-perfcli: compile-client
	javac -cp ".:../Server:../Client/" PerfClient.java

compile-client:
	make -C ../Client

compile-util:
	javac Logs.java Timer.java

compile-transaction:
	javac -cp ../Client:. Operation.java Transaction.java TransactionFactory.java


clean:
	rm -rf *.class

