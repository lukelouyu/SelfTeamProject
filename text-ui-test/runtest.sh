#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_dir="$(cd -- "$script_dir/.." && pwd)"

"$repository_dir/gradlew" --no-daemon -p "$repository_dir" clean shadowJar

mapfile -t jar_paths < <(find "$repository_dir/build/libs" -maxdepth 1 -type f -name '*.jar' -print)
if [[ ${#jar_paths[@]} -ne 1 ]]; then
    echo "Expected exactly one JAR in build/libs, found ${#jar_paths[@]}."
    exit 1
fi

cd "$script_dir"

# data/ is regenerated on every run (facilities.txt/connections.txt copied from the bundled
# defaults, activities.txt/topics.txt created empty) -- removing it first keeps the test
# deterministic and repeatable from a clean checkout instead of depending on leftover state
# from whatever input.txt last ran. academic-calendar.txt is never auto-created by the app (see
# recur's external-calendar requirement), so a deterministic synthetic fixture is copied in here
# instead of depending on the real, date-bound data/academic-calendar.txt.
rm -rf -- "$script_dir/data"
mkdir -- "$script_dir/data"
cp -- "$script_dir/academic-calendar-test.txt" "$script_dir/data/academic-calendar.txt"

java -jar "${jar_paths[0]}" < "$script_dir/input.txt" > "$script_dir/ACTUAL.TXT"

cp -- "$script_dir/EXPECTED.TXT" "$script_dir/EXPECTED-UNIX.TXT"
dos2unix "$script_dir/EXPECTED-UNIX.TXT" "$script_dir/ACTUAL.TXT"
if diff "$script_dir/EXPECTED-UNIX.TXT" "$script_dir/ACTUAL.TXT"; then
    echo "Test passed!"
else
    echo "Test failed!"
    exit 1
fi
