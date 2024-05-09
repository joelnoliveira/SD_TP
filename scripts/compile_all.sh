#!/bin/bash

# THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

general_path="backend/src/main/java/com/ds/googol/" # default path to the packages 
jsoup_path="${general_path}lib/jars/jsoup-1.17.2.jar" # default path to the jsoup jar

# default paths to the packages
rmi_path="${general_path}rmi/"
downloader_path="${general_path}downloader/"
barrel_path="${general_path}barrels/"

# Create the bin and logs in case they don't exist 
mkdir -p bin
mkdir -p logs

# In case there is a flag -delete_bin_before_compile or -clean_all, delete everything in the bin folder
if [ "$1" = "-delete_bin_before_compile" ] || [ "$1" = "-clean_all" ]; then
    rm -rf bin/*
fi

# Compile the RMIGateway
javac -d bin ${rmi_path}RMIGateway.java ${rmi_path}RMIGatewayDownloadersInterface.java ${rmi_path}RMIGatewayBarrelsInterface.java ${barrel_path}RMIBarrelsInterface.java ${rmi_path}RMIGatewayClientsInterface.java ${rmi_path}RMIClientsInterface.java

# Compile the Downloader
javac -d bin -cp .:bin:${jsoup_path} ${downloader_path}Downloader.java ${downloader_path}URLData.java ${rmi_path}RMIGatewayDownloadersInterface.java

# Compile the IndexStorageBarrel
javac -d bin ${barrel_path}IndexStorageBarrel.java ${downloader_path}URLData.java ${barrel_path}RMIBarrelsInterface.java ${rmi_path}RMIGatewayBarrelsInterface.java

# Compile the RMIClient
javac -d bin ${rmi_path}RMIClient.java ${rmi_path}RMIClientsInterface.java ${rmi_path}RMIGatewayClientsInterface.java

# In case there is a flag -delete_logs or -clean_all, delete everything in the logs folder
if [ "$1" = "-delete_logs" ] || [ "$1" = "-clean_all" ]; then
    rm -rf logs/*
fi

