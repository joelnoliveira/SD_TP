@echo off
REM THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

set input_path=config\rmi_gateway.properties REM default input path relative to the root directory
set output_path=logs\rmi_gateway_%date:~-4,4%%date:~-10,2%%date:~-7,2%%time:~-11,2%%time:~-8,2%%time:~-5,2%%time:~-2,2%.txt REM default output path relative to the root directory

REM if the user provides an input path, use it
if not "%~1"=="" (
    REM check if the file exists and if it is a properties file otherwise warn the user
    if exist %~1 (
        if "%~x1"==".properties" (
            set input_path=%~1
        ) else (
            echo The input file must be a properties file.
            exit /b 1
        )
    ) else (
        echo The input file does not exist.
        exit /b 1
    )
)

REM if the user provides an output path, use it
if not "%~2"=="" (
    set output_path=%~2
)

cd bin

java -cp . gateway.RMIGateway ..\"%input_path%" > ..\"%output_path%"

cd ..