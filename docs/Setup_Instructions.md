# Setup Instructions

Environment used for this project: **Windows 11**, **JDK 21 (LTS)**, **IntelliJ IDEA**.

> **Note on screenshots:** the placeholders below (`> [Screenshot: ...]`) mark exactly where a screenshot
> belongs. Take each one on your own machine while following the step and drop the image in
> `docs/screenshots/` (create that folder), then replace the placeholder line with
> `![description](screenshots/your-file.png)`.

## 1. Install the JDK

1. Download **JDK 21** (LTS) from [Adoptium/Eclipse Temurin](https://adoptium.net/) or
   [Oracle's JDK downloads](https://www.oracle.com/java/technologies/downloads/#java21) for Windows (x64 installer, `.msi`).
2. Run the installer. Accept the default install location
   (typically `C:\Program Files\Java\jdk-21` or `C:\Program Files\Eclipse Adoptium\jdk-21...`).
   Leave "Set JAVA_HOME variable" and "Add to PATH" checked if the installer offers them.

   > [Screenshot: JDK installer with the install-location / PATH options visible]

3. If the installer didn't set `JAVA_HOME` for you, set it manually:
   - Open **Settings → System → About → Advanced system settings → Environment Variables**.
   - Under "System variables", add `JAVA_HOME` pointing at your JDK install directory.
   - Edit the `Path` variable and add `%JAVA_HOME%\bin`.

   > [Screenshot: Environment Variables dialog showing JAVA_HOME and PATH]

## 2. Verify the installation

Open PowerShell and run:

```powershell
java -version
javac -version
```

Expected output (versions will match whatever JDK you installed — this project targets JDK 21):

```
java version "21.0.8" 2025-07-15 LTS
Java(TM) SE Runtime Environment (build 21.0.8+12-LTS-250)
Java HotSpot(TM) 64-Bit Server VM (build 21.0.8+12-LTS-250, mixed mode, sharing)

javac 21.0.8
```

> [Screenshot: terminal output of `java -version` and `javac -version`]

If `java`/`javac` aren't recognized, the PATH update from step 1.3 hasn't taken effect yet —
close and reopen the terminal, or restart the machine.

## 3. Install an IDE (IntelliJ IDEA)

1. Download **IntelliJ IDEA Community Edition** from
   [jetbrains.com/idea/download](https://www.jetbrains.com/idea/download/).
2. Run the installer and accept the defaults.

   > [Screenshot: IntelliJ IDEA installer]

## 4. Open the project

1. Launch IntelliJ IDEA → **Open** → select the `MediTrack` project folder (this repository's root,
   the one containing `MediTrack.iml`).
2. IntelliJ will detect the existing project files (`.idea/`, `MediTrack.iml`) and load the module automatically.

   > [Screenshot: IntelliJ's Open dialog pointed at the MediTrack folder]

3. Confirm the Project SDK matches the JDK you installed:
   **File → Project Structure → Project → SDK**. This project is configured for
   **JDK 21** (language level `JDK_21`, see `.idea/misc.xml`). If no SDK is listed, click
   **Add SDK → JDK...** and point it at your JDK 21 install directory.

   > [Screenshot: Project Structure dialog showing the Project SDK set to 21]

## 5. Run the application

**From IntelliJ:** open `src/com/airtribe/meditrack/Main.java`, right-click in the editor, and choose
**Run 'Main.main()'**. A console panel opens at the bottom with the `=== MediTrack ===` menu.

> [Screenshot: IntelliJ run output showing the MediTrack menu]

**From the command line**, from the project root:

```powershell
# Compile every source file into ./out
Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName } > files.txt
javac -d out -encoding UTF-8 "@files.txt"

# Run the console app
java -cp out com.airtribe.meditrack.Main
```

## 6. Run the manual test suite

`TestRunner` (`src/com/airtribe/meditrack/test/TestRunner.java`) is a plain-Java manual test harness —
no JUnit dependency required. After compiling (step 5), run:

```powershell
java -cp out com.airtribe.meditrack.test.TestRunner
```

Expected output: a `[PASS]`/`[FAIL]` line per test, ending with a summary like `9 passed, 0 failed.`
(exit code `1` if anything fails).

> [Screenshot: terminal output of a full TestRunner pass]