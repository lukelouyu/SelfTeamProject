@echo off
setlocal enableextensions
pushd %~dp0

cd ..
call gradlew clean shadowJar

cd build\libs
for /f "tokens=*" %%a in (
    'dir /b *.jar'
) do (
    set jarloc=%%a
)

cd ..\..\text-ui-test

rem data\ is regenerated on every run -- remove it first so the test is deterministic and
rem repeatable from a clean checkout instead of depending on leftover state. academic-calendar.txt
rem is never auto-created by the app, so a deterministic synthetic fixture is copied in here
rem instead of depending on the real, date-bound data\academic-calendar.txt.
if exist data rmdir /s /q data
mkdir data
copy /y academic-calendar-test.txt data\academic-calendar.txt >NUL

java -jar ..\build\libs\%jarloc% < input.txt > ACTUAL.TXT

FC ACTUAL.TXT EXPECTED.TXT >NUL && ECHO Test passed! || Echo Test failed!
