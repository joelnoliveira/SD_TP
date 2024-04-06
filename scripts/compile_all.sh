#!/bin/bash

# In case there is a flag -delete_bin_before_compile or -clean_all, delete everything in the bin folder
if [ "$1" = "-delete_bin_before_compile" ] || [ "$1" = "-clean_all" ]; then
    rm -rf bin/*
fi
# Compile the RMIGateway
javac -d bin src/rmi/RMIGateway.java src/rmi/RMIGatewayDownloadersInterface.java src/rmi/RMIGatewayBarrelsInterface.java src/barrels/RMIBarrelsInterface.java src/rmi/RMIGatewayClientsInterface.java src/rmi/RMIClientsInterface.java

# Compile the Downloader
javac -d bin -cp .:bin:lib/jars/jsoup-1.17.2.jar src/downloader/Downloader.java src/downloader/URLData.java src/rmi/RMIGatewayDownloadersInterface.java

# Compile the IndexStorageBarrel
javac -d bin src/barrels/IndexStorageBarrel.java src/downloader/URLData.java src/barrels/RMIBarrelsInterface.java src/rmi/RMIGatewayBarrelsInterface.java

# Compile the RMIClient
javac -d bin src/rmi/RMIClient.java src/rmi/RMIClientsInterface.java src/rmi/RMIGatewayClientsInterface.java

# In case there is a flag -delete_logs or -clean_all, delete everything in the logs folder
if [ "$1" = "-delete_logs" ] || [ "$1" = "-clean_all" ]; then
    rm -rf logs/*
fi

