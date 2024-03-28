#!/bin/sh

# Compile the RMIGateway class
javac -d bin src/rmi/RMIGateway.java src/rmi/RMIGatewayRemote.java

# Compile the Downloader class
javac -d bin -cp .:bin:lib/jars/jsoup-1.17.2.jar src/downloader/Downloader.java
