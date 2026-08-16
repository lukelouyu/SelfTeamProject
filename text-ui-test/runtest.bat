@echo off
setlocal enableextensions
pushd "%~dp0" || exit /b 1

call ..\gradlew.bat --no-daemon -p .. clean shadowJar
if errorlevel 1 (
    echo Gradle build failed!
    goto :fail
)

set "jarloc="
set "jarcount=0"
for /f "delims=" %%a in ('dir /b /a-d "..\build\libs\*.jar" 2^>NUL') do (
    set "jarloc=%%a"
    set /a jarcount+=1
)
if not "%jarcount%"=="1" (
    echo Expected exactly one JAR in build\libs, found %jarcount%.
    goto :fail
)

rem data\ is regenerated on every run -- remove it first so the test is deterministic and
rem repeatable from a clean checkout instead of depending on leftover state. academic-calendar.txt
rem is never auto-created by the app, so a deterministic synthetic fixture is copied in here
rem instead of depending on the real, date-bound data\academic-calendar.txt.
if exist data rmdir /s /q data
if exist data (
    echo Could not remove the previous test data directory.
    goto :fail
)
mkdir data
if errorlevel 1 (
    echo Could not create the test data directory.
    goto :fail
)
copy /y academic-calendar-test.txt data\academic-calendar.txt >NUL
if errorlevel 1 (
    echo Could not copy the academic calendar fixture.
    goto :fail
)

rem A fixed clock keeps the fixture's activity dates from ever becoming "in the past" relative to
rem the real calendar date the test happens to run on.
java -Dunienable.fixedNow=2026-08-10T12:00 -jar "..\build\libs\%jarloc%" < input.txt > ACTUAL.TXT
if errorlevel 1 (
    echo Application execution failed!
    goto :fail
)

FC ACTUAL.TXT EXPECTED.TXT >NUL
if errorlevel 1 (
    echo Test failed!
    goto :fail
)

echo Test passed!
popd
exit /b 0

:fail
popd
exit /b 1
