@echo off
REM THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

set general_path=not_web_server\src\ REM default path to the packages
set jsoup_path=not_web_server\lib\jars\jsoup-1.17.2.jar REM default path to the jsoup jar
set meta1_jar_path=web_server\lib\jars\meta1.jar REM default path to the gateway client dependencies jar

REM default paths to the packages
set meta1_path=%general_path%meta1\

REM Create the bin and logs in case they don't exist
if not exist bin mkdir bin
if not exist logs mkdir logs

REM In case there is a flag -delete_bin_before_compile or -clean_all, delete everything in the bin folder
if "%1"=="-delete_bin_before_compile" or "%1"=="-clean_all" (
    echo Deleting everything in the bin folder
    rmdir /s /q bin
    mkdir bin
)

echo Compiling meta1 package
REM Compile the meta1 package
javac -d bin %meta1_path%* -cp %jsoup_path%

REM If there happened no errors in the last compilation create a jar files for the gateway_client_dependencies package to be used by the web server
if %ERRORLEVEL% EQU 0 (
    echo Creating jar files for the client
    jar -cvf %meta1_jar_path% -C bin meta1
)

REM In case there is a flag -delete_logs or -clean_all, delete everything in the logs folder
if "%1"=="-delete_logs" or "%1"=="-clean_all" (
    echo Deleting everything in the logs folder
    rmdir /s /q logs
    mkdir logs
)