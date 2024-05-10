#!/bin/bash

# THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

general_path="not_web_server/src/" # default path to the packages 
jsoup_path="lib/jars/jsoup-1.17.2.jar" # default path to the jsoup jar

# default paths to the packages
gateway_path="${general_path}gateway/"
downloader_path="${general_path}downloader/"
barrel_path="${general_path}barrels/"
client_path="${general_path}client/"
g_c_d_path="${general_path}gateway_client_dependencies/" # path to the gateway client dependencies

# Create the bin and logs in case they don't exist 
mkdir -p bin
mkdir -p logs

# In case there is a flag -delete_bin_before_compile or -clean_all, delete everything in the bin folder
if [ "$1" = "-delete_bin_before_compile" ] || [ "$1" = "-clean_all" ]; then
    echo "Deleting everything in the bin folder"
    rm -rf bin/*
fi

echo "Compiling RMIGateway"
# Compile the RMIGateway
javac -d bin ${gateway_path}RMIGateway.java ${gateway_path}RMIGatewayDownloadersInterface.java ${gateway_path}RMIGatewayBarrelsInterface.java ${barrel_path}RMIBarrelsInterface.java ${g_c_d_path}RMIGatewayClientsInterface.java ${g_c_d_path}RMIClientsInterface.java

echo "Compiling Downloader"
# Compile the Downloader
javac -d bin -cp .:bin:${jsoup_path} ${downloader_path}Downloader.java ${downloader_path}URLData.java ${gateway_path}RMIGatewayDownloadersInterface.java

echo "Compiling IndexStorageBarrel"
# Compile the IndexStorageBarrel
javac -d bin ${barrel_path}IndexStorageBarrel.java ${downloader_path}URLData.java ${barrel_path}RMIBarrelsInterface.java ${gateway_path}RMIGatewayBarrelsInterface.java

echo "Compiling RMIClient"
# Compile the RMIClient
javac -d bin ${client_path}RMIClient.java ${g_c_d_path}RMIClientsInterface.java ${g_c_d_path}RMIGatewayClientsInterface.java

# If there happened no errors in the last compilation create a jar files for the gateway_client_dependencies package to be used by the web server
if [ $? -eq 0 ]; then
    echo "Creating jar files for the client"
    jar -cvf lib/jars/gateway_client_dependencies.jar -C bin gateway_client_dependencies
fi 

# In case there is a flag -delete_logs or -clean_all, delete everything in the logs folder
if [ "$1" = "-delete_logs" ] || [ "$1" = "-clean_all" ]; then
    echo "Deleting everything in the logs folder"
    rm -rf logs/*
fi

