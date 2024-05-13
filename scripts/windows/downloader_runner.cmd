@echo off
REM THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

set id=%random% REM default id for the downloader
set input_path=config\downloaders.properties REM default input path relative to the root directory
set jsoup_path=lib\jars\jsoup-1.17.2.jar REM default path to the jsoup jar

REM check if the user provides an id for the downloaders there mustn't be any other equal id
if not "%~1"=="" (
    echo.%~1| findstr /R /C:"^[0-9]*$">nul
    if errorlevel 1 (
        echo The id must be a number.
        exit /b 1
    ) else (
        set id=%~1
    )
)

set output_path=logs\downloader_%id%.txt REM default output path relative to the root directory

REM if the user provides an input path, use it
if not "%~2"=="" (
    REM check if the file exists and if it is a properties file otherwise warn the user
    if exist %~2 (
        if "%~x2"==".properties" (
            set input_path=%~2
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
if not "%~3"=="" (
    set output_path=%~3
)

cd bin

java -cp .;..\"%jsoup_path%" downloader.Downloader %id% ..\"%input_path%" > ..\"%output_path%"

cd ..