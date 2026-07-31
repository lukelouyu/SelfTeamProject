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
rem repeatable from a clean checkout instead of depending on leftover state.
if exist data rmdir /s /q data

java -jar ..\build\libs\%jarloc% < input.txt > ACTUAL.TXT

FC ACTUAL.TXT EXPECTED.TXT >NUL && ECHO Test passed! || Echo Test failed!
