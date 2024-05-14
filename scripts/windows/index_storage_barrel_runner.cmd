@echo off
REM THIS SCRIPT IS INTENDED TO BE RUN FROM THE ROOT DIRECTORY OF THE PROJECT

setlocal enabledelayedexpansion

REM Generate a default id for the index storage barrel based on current time in milliseconds
for /f "delims=" %%a in ('wmic os get LocalDateTime ^| find "."') do set id=%%a

REM Default input path relative to the root directory
set input_path=config\index_storage_barrels.properties

REM Check if the user provides an id for the index storage barrel; there mustn't be any other equal id
if "%~1" neq "" (
    echo %1 | findstr /r /c:"^[0-9][0-9]*$" >nul
    if errorlevel 1 (
        echo The id must be a number.
        exit /b 1
    ) else (
        set id=%1
    )
)

REM Default output path relative to the root directory
set output_path=logs\index_storage_barrel_!id!.txt

REM If the user provides an input path, use it
if not "%~2"=="" (
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

REM If the user provides an output path, use it
if not "%~3"=="" (
    set output_path=%~3
)

cd bin

java meta1.IndexStorageBarrel !id! ..\!input_path! >> ..\!output_path!

cd ..