import sbt._
import Keys._

// some usefull libraries
// they are pulled only if used
object Dependencies {
  val specs = "org.scala-tools.testing" % "specs_2.9.0-1" % "1.6.8" % "test"
  val scalatest = "org.scalatest" % "scalatest_2.9.0" % "1.6.1" % "test"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.8.4" % "test"
  val unfiltered_filter = "net.databinder" %% "unfiltered-filter" % "0.4.1"
  val unfiltered_jetty = "net.databinder" %% "unfiltered-jetty" % "0.4.1"
  val unfiltered_spec = "net.databinder" %% "unfiltered-spec" % "0.4.1" % "test"
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.5.8"
  val antiXML = "com.codecommit" %% "anti-xml" % "0.3-SNAPSHOT" % "test"
  val jena = "com.hp.hpl.jena" % "jena" % "2.6.4"
  val arq = "com.hp.hpl.jena" % "arq" % "2.8.8"
//  val jenaIri = "com.hp.hpl.jena" % "iri" % "0.8" from "http://openjena.org/repo/com/hp/hpl/jena/iri/0.8/iri-0.8.jar"


}

// some usefull repositories
object Resolvers {
  val novus = "repo.novus snaps" at "http://repo.novus.com/snapshots/"
}

// general build settings
object BuildSettings {

  val buildOrganization = "org.w3"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.9.0-1"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion,
    parallelExecution in Test := false,
    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )

}

object YourProjectBuild extends Build {

  import Dependencies._
  import Resolvers._
  import BuildSettings._
  import ProguardPlugin._

  def keepUnder(pakage:String):String = "-keep class %s.**" format pakage
  
  val proguardSettings:Seq[Setting[_]] =
    ProguardPlugin.proguardSettings ++ Seq[Setting[_]](
      minJarPath := new File("readwriteweb.jar"),
      proguardOptions += keepMain("org.w3.readwriteweb.ReadWriteWebMain"),
      proguardOptions += keepUnder("org.w3.readwriteweb"),
      proguardOptions += keepUnder("unfiltered"),
      proguardOptions += keepUnder("org.apache.log4j"),
      proguardOptions += keepUnder("com.hp.hpl.jena"),
      proguardOptions += "-keep class com.hp.hpl.jena.rdf.model.impl.ModelCom"
    )

  val yourProjectSettings =
    Seq(
      resolvers += ScalaToolsReleases,
      resolvers += ScalaToolsSnapshots,
      libraryDependencies += specs,
      libraryDependencies += unfiltered_spec,
      libraryDependencies += dispatch,
      libraryDependencies += unfiltered_filter,
      libraryDependencies += unfiltered_jetty,
//      libraryDependencies += slf4jSimple,
      libraryDependencies += jena,
      libraryDependencies += arq,
      libraryDependencies += antiXML
    )

  lazy val yourProject = Project(
    id = "read-write-web",
    base = file("."),
    settings = buildSettings ++ yourProjectSettings ++ sbtassembly.Plugin.assemblySettings ++ proguardSettings
  )
  


}

