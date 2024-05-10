#!/bin/bash

# THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

id=$(date +%N) # default id for the downloader
input_path="config/downloaders.properties" # default input path relative to the root directory
domain="com.ds.googol" # default domain for the packages
jsoup_path="backend/lib/jars/jsoup-1.17.2.jar" # default path to the jsoup jar

# check if the user provides an id for the downloaders there mustn't be any other equal id
if [ $# -gt 0 ]; then
    if [[ $1 == *[^0-9]* ]]; then
        echo "The id must be a number."
        exit 1
    else
        id=$1
    fi
fi

output_path="logs/downloader_$id.txt" # default output path relative to the root directory

# if the user provides an input path, use it
if [ $# -gt 1 ]; then
        
    # check if the file exists and if it is a properties file otherwise warn the user
    if [ -f $2 ]; then
        if [[ $2 == *.properties ]]; then
            input_path=$2
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
if [ $# -gt 2 ]; then
    output_path=$3
fi

cd bin

java -cp .:../$jsoup_path $domain.downloader.Downloader $id ../$input_path > ../$output_path

cd ..