@echo off
REM THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

setlocal enabledelayedexpansion

REM Generate a default id for the RMI Gateway based on current time
for /f "delims=" %%a in ('wmic os get LocalDateTime ^| find "."') do set id=%%a

REM Default input path relative to the root directory
set input_path=config\rmi_gateway.properties

REM Default output path relative to the root directory
set output_path=logs\rmi_gateway_!id!.txt

REM If the user provides an input path, use it
if not "%~1"=="" (
    if exist "%~1" (
        echo %~1 | findstr /r /c:".*\.properties$" >nul
        if errorlevel 1 (
            echo The input file must be a properties file.
            exit /b 1
        ) else (
            set input_path=%~1
        )
    ) else (
        echo The input file does not exist.
        exit /b 1
    )
)

REM If the user provides an output path, use it
if not "%~2"=="" (
    set output_path=%~2
)

cd bin

java -cp . meta1.RMIGateway ..\!input_path! > ..\!output_path!

cd ..