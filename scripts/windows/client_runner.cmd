@echo off
REM THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

set id=%random% REM default id for the downloader
set input_path=config\clients.properties REM default input path relative to the root directory

REM check if the user provides an id for the user there mustn't be any other equal id
if "%~1" neq "" (
    echo %1 | findstr /r /c:"^[0-9][0-9]*$" >nul
    if errorlevel 1 (
        echo The id must be a number.
        exit /b 1
    ) else (
        set id=%1
    )
)

REM if the user provides an input path, use it
if not "%~2"=="" (
    REM check if the file exists and if it is a properties file otherwise warn the user
    if exist "%~2" (
        echo %~2 | findstr /r /c:".*\.properties$" >nul
        if errorlevel 1 (
            echo The input file must be a properties file.
            exit /b 1
        ) else (
            set input_path=%~2
        )
    ) else (
        echo The input file does not exist.
        exit /b 1
    )
)

cd bin

java meta1.RMIClient %id% ..\%input_path%

cd ..