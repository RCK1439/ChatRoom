JAVAC = javac
JVM = java

JCFLAGS = -d
JRFLAGS = -cp

BIN = bin
SRC = src/*.java
EXE_SERVER = Server
EXE_CLIENT = Client

.PHONY: all client server clean

all:
	$(JAVAC) $(JCFLAGS) $(BIN) $(SRC)

client:
	$(JVM) $(JRFLAGS) $(BIN) $(EXE_CLIENT)

server:
	$(JVM) $(JRFLAGS) $(BIN) $(EXE_SERVER)

clean:
	rm -f $(BIN)/*.class
