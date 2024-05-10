#!/bin/bash

# THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

input_path="config/rmi_gateway.properties" # default input path relative to the root directory
output_path="logs/rmi_gateway_$(date +%Y%m%d%H%M%S%N).txt" # default output path relative to the root directory
domain="com.ds.googol" # default domain for the packages

# if the user provides an input path, use it
if [ $# -gt 0 ]; then
    
    # check if the file exists and if it is a properties file otherwise warn the user
    if [ -f $1 ]; then
        if [[ $1 == *.properties ]]; then
            input_path=$1
        else
            echo "The input file must be a properties file."
            exit 1
        fi
    else
        echo "The input file does not exist."
        exit 1
    fi
fi

# if the user provides an output path, use it
if [ $# -gt 1 ]; then
    output_path=$2
fi

cd bin

java -cp . $domain.rmi.RMIGateway ../$input_path > ../$output_path

cd ..