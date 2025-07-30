# Changelog

This fork is a mavenized version of JetUML.

## Project structure changes

* The project structure has been slightly modified, following the standard maven convention.
* In maven there is a strict separation between sources (everything java) and resource (configurations).
    * The general layout for a maven project is:

```text
JetUML/
├── pom.xml             <- declaration of dependencies + plugins
└── src
    ├── main
    │   └── java        <- source code
    │   └── resources   <- binaries, jsons, etc...
    └── test (currently removed, tests not yet configured)
        └── java
```

* Examples, for moved resources:
    * src/main/java/org.jetUml/JetUML.css => src/main/resources/org/jetuml/JetUML.css
    * src/main/java/org.jetUml/gui/DarkMode.css => src/main/resources/org/jetuml/gui/DarkMode.css

### pom

The Project Object Model (`pom.xml`) is like a recipe:

* Ingredients needed to build, i.e. the dependencies.
    * JavaFX
    * Junit
* How to prepare the dish, i.e. the plugins.
    * Instructions for JavaFX
    * Instructions for building the JAR (see further down)
    * Instructions for building a native OS application (see further down)

(Technically each plugin is also a dependency, only that is only required for the build process - a bit like a test
dependency)

## Loading resources at runtime

* Loading images was a bit tricky with maven, because (for a reason I do not understand), the existing code for loading
  images always uses the `BootLoader` to load resources, i.e. refuses to pick up any resource placed in
  the `src/main/resources/` location.  
  Existing code sample: `pStage.getIcons().add(new Image(RESOURCES.getString("application.icon")));`
* The `RESOURCES.getString` is not problematic, it just resolves resource locations from the `JetUML.properties` key
  value map.  
  Examples:
    * `application.name=JetUML`
    * `application.icon=jet.png`
* `newImage` part is problematic, because it does not pick up anything placed in `src/main/resources`

### Fix

Had to modify how JetUML.java loads resources… in the code.

Illustration:
* Implicit search usign BootLoader (does not work, will not find anything from maven's resource dir):
`pStage.getIcons().add(new Image(RESOURCES.getString("application.icon")));`
* Explicit search in resources:
`pStage.getIcons().add(new Image(getClass().getResource(RESOURCES.getString("application.icon")).toExternalForm()));`

This had to be changed in various places (not that many though):

* Main application image / icon.
* ToolTips / `TipDialogue.prepareStage`
* Image loading for specific diagram images.

### Debugging

Debugging JavaFX is a bit of a hassle, because JavaFX uses background threads and the breakpoints you set in your IDE do
not trigger at application startup.

* I configured two JavaFX executions (see https://stackoverflow.com/a/61341407/13805480 )
    * This means when running `javafx:run` you have to specify what you actually want:
    * Run, as usual: `mvn clean javafx:run@run`
    * Run, without debugging: `mvn clean javafx:run@debug`
* If you chose the debug option, you still
  must [add a remote debugger from your ide on port 8000](https://stackoverflow.com/questions/61340702/intellij-idea-how-to-debug-a-javafx-maven-project/61341407#61341407)

## Builds

The maven `package` phase (`mvn clean package`) is responsible for creating something that can be delivered to the end user (in contrast to the earlier `compile` phase which just provides the compiled individual files.)

### Fat jar build

* By default, maven, will NOT include dependencies in the created application JAR during the package
  phase (`mvn clean package`)
* I added the shade plugin to override the default behaviour:
    * Essentially the shade plugin builds the JAR as usually, but then opens it up again, places add dependencies
      inside, then zips it again.
    * It also requires a new proxy launcher: [ShadeJetUML](src/main/java/org/jetuml/ShadeJetUML.java)
        * This is needed because a classic method call is required as entry point, and the proxy launcher provides a
          main method that internally delegates to the existing launcher's JavaFX logic.

> If you're running `mvn clean package` and you're not on a Mac, the build may fail, because it implicitly tries to build a native OS app, and I only tested this on MacOS. Quick fis is to remove the javapackager plugin, see comments in `pom.xml`.

### Mac native App build

* The fat / self-contained jar (however you want to call it), still requires a JRE to function.
* For a fair share of students this is a severe obstacle (until they failed a milestone), so for convenience it may make
  sense to create a version that does not require manual JRE setup.
    * Implicitly this means parts of the JRE are packaged in the executable, so the generated artefact gets faaaaaaat (
      100 MB)
* There's [a maven plugin](https://github.com/javapackager/JavaPackager
  ) for all operating systems (but you can only build for the host you're on - only tested this on MacOS so far)
* To generate the app: `mvn clean package`

Minor detail: 
* All Mac icons [are supposed to be rounded](https://developer.apple.com/design/Human-Interface-Guidelines/app-icons), and are required in `icon` format.
* Placed a patched icon in `assets/mac/JetUML.icon`

## What's next 

* Enforcing tests pass as build requirement
* Enforcing checkstyle as build requirement
* Profiles for activating / deactivating OS builds
* github CI config to reject commits with code that does not pass checkstyle / tests

> Max, July 30th 2025
