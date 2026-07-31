#!/usr/bin/env bash

# change to script directory
cd "${0%/*}"

cd ..
./gradlew clean shadowJar

cd text-ui-test

# data/ is regenerated on every run (facilities.txt/connections.txt copied from the bundled
# defaults, activities.txt/topics.txt created empty) -- removing it first keeps the test
# deterministic and repeatable from a clean checkout instead of depending on leftover state
# from whatever input.txt last ran.
rm -rf data

java  -jar $(find ../build/libs/ -mindepth 1 -print -quit) < input.txt > ACTUAL.TXT

cp EXPECTED.TXT EXPECTED-UNIX.TXT
dos2unix EXPECTED-UNIX.TXT ACTUAL.TXT
diff EXPECTED-UNIX.TXT ACTUAL.TXT
if [ $? -eq 0 ]
then
    echo "Test passed!"
    exit 0
else
    echo "Test failed!"
    exit 1
fi
