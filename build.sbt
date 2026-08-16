import org.scalajs.linker.interface.ModuleKind

ThisBuild / scalaVersion := "3.3.8" // LTS
ThisBuild / organization := "es.urjc"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val V = new {
  val munit = "1.3.5"
  val munitScalacheck = "1.3.0"
  val laminar = "17.2.1"
  val cats = "2.13.0"
}

lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
)

// The engine is pure Scala with no platform dependency: it cross-builds to the
// JVM so the property suites run at full speed, and to JS for the web app.
lazy val engine = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("engine"))
  .settings(commonSettings)
  .settings(
    name := "curry-howard-engine",
    libraryDependencies ++= Seq(
      // D10 amendment (D20): cats is the engine's one runtime dependency.
      "org.typelevel" %%% "cats-core" % V.cats,
      "org.typelevel" %%% "cats-free" % V.cats,
      "org.scalameta" %%% "munit" % V.munit % Test,
      "org.scalameta" %%% "munit-scalacheck" % V.munitScalacheck % Test
    )
  )

lazy val web = project
  .in(file("web"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(engine.js)
  .settings(commonSettings)
  .settings(
    name := "curry-howard-web",
    scalaJSUseMainModuleInitializer := true,
    // NoModule keeps the linker output to a single main.js, which keeps the
    // service worker's cache list short and the page loadable without a bundler.
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.NoModule)),
    libraryDependencies += "com.raquo" %%% "laminar" % V.laminar
  )

// A console client, JVM only. The engine gains no console dependency from it:
// the REPL is a client exactly as the web app is, and playing through it
// exercises the same calls the Play screen will make.
lazy val repl = project
  .in(file("repl"))
  .dependsOn(engine.jvm)
  .settings(commonSettings)
  .settings(
    name := "curry-howard-repl",
    Compile / mainClass := Some("curryhoward.repl.Main"),
    run / fork := true,
    run / connectInput := true,
    run / outputStrategy := Some(StdoutOutput)
  )

// --- Static site assembly ---------------------------------------------------
// No npm, no bundler: the app has no JavaScript dependencies, so a "build" is
// the linker output plus the hand-written files in static/.

lazy val staticDir = settingKey[File]("Hand-written static assets")
lazy val distDir = settingKey[File]("Assembled site output")
lazy val distDev = taskKey[File]("Assemble dist/ with the fast (development) linker")
lazy val distProd = taskKey[File]("Assemble dist/ with the full (production) linker")

def assembleSite(js: File, static: File, out: File, log: Logger): File = {
  IO.delete(out)
  IO.copyDirectory(static, out)
  IO.copyDirectory(js, out / "js")
  log.info(s"site assembled at $out")
  out
}

lazy val root = project
  .in(file("."))
  .aggregate(engine.jvm, engine.js, web, repl)
  .settings(
    name := "curry-howard-game",
    publish / skip := true,
    staticDir := baseDirectory.value / "static",
    distDir := baseDirectory.value / "dist",
    distDev := assembleSite(
      (web / Compile / fastLinkJSOutput).value,
      staticDir.value,
      distDir.value,
      streams.value.log
    ),
    distProd := assembleSite(
      (web / Compile / fullLinkJSOutput).value,
      staticDir.value,
      distDir.value,
      streams.value.log
    )
  )
