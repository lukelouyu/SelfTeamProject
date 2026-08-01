# UniEnable

UniEnable is a single-user, offline, CLI-based Java 17 application that helps tertiary students
with ASD or ADHD, and tertiary students who use wheelchairs, prepare for unfamiliar university,
internship, or entry-level work routines. It combines fixed/flexible activity planning with
energy-demand and sensory-load ratings, category and topic organisation, a deterministic "next
relevant activity" lookup, and read-only local facility/accessible-route reference information.

Useful links:
* [User Guide](docs/UserGuide.md)
* [Developer Guide](docs/DeveloperGuide.md)
* [About Us](docs/AboutUs.md)

## Setting up in Intellij

Prerequisites: JDK 17 (use the exact version), update Intellij to the most recent version.

1. **Ensure Intellij JDK 17 is defined as an SDK**, as described [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk) -- this step is not needed if you have used JDK 17 in a previous Intellij project.
1. **Import the project _as a Gradle project_**, as described [here](https://se-education.org/guides/tutorials/intellijImportGradleProject.html).
1. **Verify the setup**: After the importing is complete, locate the `src/main/java/seedu/unienable/UniEnable.java` file, right-click it, and choose `Run UniEnable.main()`. If the setup is correct, you should see something like the below:
   ```
   > Task :compileJava
   > Task :processResources
   > Task :classes

   > Task :UniEnable.main()
   ____________________________________________________________
   Hello! Welcome to UniEnable.
   Your Uni Friend for planning accessible university routines.
   ```
   Type `bye` and press enter to let the execution proceed to the end.

**Warning:** Keep the `src\main\java` folder as the root folder for Java files (i.e., don't rename those folders or move Java files to another folder outside of this folder path), as this is the default location some tools (e.g., Gradle) expect to find Java files.

## Build automation using Gradle

* This project uses Gradle for build automation and dependency management (`build.gradle`).
* Run `./gradlew shadowJar` to build the standalone executable JAR at `build/libs/unienable.jar`.
* If you are new to Gradle, refer to the [Gradle Tutorial at se-education.org/guides](https://se-education.org/guides/tutorials/gradle.html).

## Distribution (release ZIP)

`recur` needs an external, human-editable `data/academic-calendar.txt` that must live outside the
JAR so a future academic year can be added by editing a text file, not by recompiling. Because of
that, **the distributable is a ZIP, not a bare JAR.** Run:

```bash
./gradlew releaseZip
```

This produces `build/distributions/unienable.zip`, containing:

```text
unienable.zip
├── unienable.jar
└── data/
    └── academic-calendar.txt
```

To run it: unzip anywhere, `cd` into the extracted folder, and run `java -jar unienable.jar` from
there (so the app's own `data/` directory resolves next to the calendar file already provided).
`academic-calendar.txt` is never read, created, or modified by the JAR itself except by `recur`
loading it — see the [User Guide](docs/UserGuide.md#11-data-storage) for the file format and how
to extend it to a new academic year.

## Testing

### I/O redirection tests

* To run _I/O redirection_ tests (aka _Text UI tests_), navigate to `text-ui-test` and run the `runtest(.bat/.sh)` script. It scripts most v1.0 and v2.0-so-far commands, boundary cases, and error paths end-to-end against a freshly built JAR, including `recur`, the three-option `reset all`, `facility validate`, and `connection validate`. The script copies a small synthetic `academic-calendar-test.txt` fixture into `data/academic-calendar.txt` before each run, so `recur` results stay deterministic instead of depending on the real, date-bound calendar. It deliberately does **not** cover `list today`/`list tomorrow`/`list this week`, since their result depends on the current date; those are covered by JUnit tests with an injected fixed `now` instead (see `ActivityCommandParserTest`).

### JUnit tests

* Run `./gradlew test` to run the full JUnit test suite (`src/test/java/seedu/unienable/`).
* If you are new to JUnit, refer to the [JUnit Tutorial at se-education.org/guides](https://se-education.org/guides/tutorials/junit.html).

## Checkstyle

* Run `./gradlew checkstyleMain checkstyleTest` to check code style against `config/checkstyle/checkstyle.xml`.
* If you are new to Checkstyle, refer to the [Checkstyle Tutorial at se-education.org/guides](https://se-education.org/guides/tutorials/checkstyle.html).

## CI using GitHub Actions

The project uses [GitHub Actions](https://github.com/features/actions) for CI (`.github/workflows/gradle.yml`). When a commit is pushed to this repo or a PR is opened against it, GitHub Actions runs automatically to build and verify the code.

## Documentation

The `/docs` folder contains the User Guide, Developer Guide, and About Us pages, published via
GitHub Pages from the `master`/`main` branch's `/docs` folder.
