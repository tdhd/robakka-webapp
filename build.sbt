name := """robakka-webapp"""

version := "1.0-SNAPSHOT"

lazy val robakkaProj = ProjectRef(uri("https://github.com/tdhd/robakka.git"), "robakka-id")

lazy val root = (project in file(".")).enablePlugins(PlayScala).dependsOn(robakkaProj)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
