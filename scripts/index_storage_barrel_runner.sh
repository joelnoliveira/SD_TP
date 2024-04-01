#!/bin/bash

id=$(date +%N) # default id for the index storage barrel
input_path="config/index_storage_barrels.properties" # default input path relative to the root directory

# check if the user provides an id for the downloaders there mustn't be any other equal id
if [ $# -gt 0 ]; then
    if [[ $1 == *[^0-9]* ]]; then
        echo "The id must be a number."
        exit 1
    else
        id=$1
    fi
fi

output_path="logs/index_storage_barrel_$id.txt" # default output path relative to the root directory

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
if [ $# -gt 1 ]; then
    output_path=$3
fi

cd bin

java barrels.IndexStorageBarrel $id ../$input_path > ../$output_path

cd ..
