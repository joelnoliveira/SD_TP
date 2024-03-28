#!/bin/sh

javac -d bin -cp .:lib/jars/jsoup-1.17.2.jar src/downloader/Downloader.java # Compile the Downloader class

# if there are no errors in the compilation, run the Downloader class
if [ $? -eq 0 ]; then
    cd bin
    java -cp .:../lib/jars/jsoup-1.17.2.jar Downloader > ../data/output.txt
fi