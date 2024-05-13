@echo off
REM THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

set general_path=not_web_server\src\ REM default path to the packages 
set jsoup_path=not_web_server\lib\jars\jsoup-1.17.2.jar REM default path to the jsoup jar
set g_c_d_jar_path=web_server\lib\jars\gateway_client_dependencies.jar REM default path to the gateway client dependencies jar
set mvn_path=web_server\backend\ REM default path to the web server

REM default paths to the packages
set gateway_path=%general_path%gateway\
set downloader_path=%general_path%downloader\
set barrel_path=%general_path%barrels\
set client_path=%general_path%client\
set g_c_d_path=%general_path%gateway_client_dependencies\ REM path to the gateway client dependencies

REM Create the bin and logs in case they don't exist 
if not exist bin mkdir bin
if not exist logs mkdir logs

REM In case there is a flag -delete_bin_before_compile or -clean_all, delete everything in the bin folder
if "%~1"=="-delete_bin_before_compile" (
    echo Deleting everything in the bin folder
    del /Q /S bin\*
)

echo Compiling RMIGateway
REM Compile the RMIGateway
javac -d bin %gateway_path%RMIGateway.java %gateway_path%RMIGatewayDownloadersInterface.java %gateway_path%RMIGatewayBarrelsInterface.java %barrel_path%RMIBarrelsInterface.java %g_c_d_path%RMIGatewayClientsInterface.java %g_c_d_path%RMIClientsInterface.java

echo Compiling Downloader
REM Compile the Downloader
javac -d bin -cp .;bin;%jsoup_path% %downloader_path%Downloader.java %downloader_path%URLData.java %gateway_path%RMIGatewayDownloadersInterface.java

echo Compiling IndexStorageBarrel
REM Compile the IndexStorageBarrel
javac -d bin %barrel_path%IndexStorageBarrel.java %downloader_path%URLData.java %barrel_path%RMIBarrelsInterface.java %gateway_path%RMIGatewayBarrelsInterface.java

echo Compiling RMIClient
REM Compile the RMIClient
javac -d bin %client_path%RMIClient.java %g_c_d_path%RMIClientsInterface.java %g_c_d_path%RMIGatewayClientsInterface.java

REM If there happened no errors in the last compilation create a jar files for the gateway_client_dependencies package to be used by the web server
if %errorlevel% equ 0 (
    echo Creating jar files for the client
    jar -cvf %g_c_d_jar_path% -C bin gateway_client_dependencies
)

REM In case there is a flag -delete_logs or -clean_all, delete everything in the logs folder
if "%~1"=="-delete_logs" (
    echo Deleting everything in the logs folder
    del /Q /S logs\*
)