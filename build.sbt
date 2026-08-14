import org.scalajs.linker.interface.ModuleKind

ThisBuild / scalaVersion := "3.3.8" // LTS
ThisBuild / organization := "es.urjc"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val V = new {
  val munit = "1.3.5"
  val munitScalacheck = "1.3.0"
  val laminar = "17.2.1"
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
  .aggregate(engine.jvm, engine.js, web)
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
