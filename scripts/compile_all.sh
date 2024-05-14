#!/bin/bash

# THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

general_path="not_web_server/src/" # default path to the packages 
jsoup_path="not_web_server/lib/jars/jsoup-1.17.2.jar" # default path to the jsoup jar
meta1_jar_path="web_server/lib/jars/meta1.jar" # default path to the gateway client dependencies jar

# default paths to the packages
meta1_path="${general_path}meta1/"

# Create the bin and logs in case they don't exist 
mkdir -p bin
mkdir -p logs

# In case there is a flag -delete_bin_before_compile or -clean_all, delete everything in the bin folder
if [ "$1" = "-delete_bin_before_compile" ] || [ "$1" = "-clean_all" ]; then
    echo "Deleting everything in the bin folder"
    rm -rf bin/*
fi

echo "Compiling meta1 package"
# Compile the meta1 package
javac -d bin ${meta1_path}* -cp ${jsoup_path}

# If there happened no errors in the last compilation create a jar files for the gateway_client_dependencies package to be used by the web server
if [ $? -eq 0 ]; then
    echo "Creating jar files for the client"
    jar -cvf $meta1_jar_path -C bin meta1
fi 

# In case there is a flag -delete_logs or -clean_all, delete everything in the logs folder
if [ "$1" = "-delete_logs" ] || [ "$1" = "-clean_all" ]; then
    echo "Deleting everything in the logs folder"
    rm -rf logs/*
fi

