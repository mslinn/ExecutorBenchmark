// see https://github.com/sbt/sbt-assembly
import AssemblyKeys._ // put this at the top of the file

organization := "com.micronautics"

name := "executorBenchmark"

version := "0.1"

scalaVersion := "2.9.3"

scalaVersion in update := "2.9.3"

autoCompilerPlugins := true

javaOptions in run += "-Xbatch -server -Xmx1G -Xms1G -XX:PermSize=64m -XX:MaxPermSize=64m"

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8")

resolvers ++= Seq(
  "Akka Snapshots"           at "http://akka.io/snapshots",
  "Typesafe Snapshots"       at "http://repo.typesafe.com/typesafe/snapshots",
  "Typesafe Releases"        at "http://repo.typesafe.com/typesafe/releases",
  "Scala-Tools Snapshots"    at "http://scala-tools.org/repo-snapshots",
  "Scala Tools Releases"     at "http://scala-tools.org/repo-releases",
  "Sonatype Nexus Releases"  at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype Legacy Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype staging"         at "https://oss.sonatype.org/content/repositories/staging",
  "Sonatype Snapshots"       at "https://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"             %  "akka-actor"           % "2.0"       withSources(),
  "com.github.scala-incubator.io" %  "scala-io-core_2.9.1"  % "0.3.0"     withSources(),
  "com.github.scala-incubator.io" %  "scala-io-file_2.9.1"  % "0.3.0"     withSources(),
  "org.apache.httpcomponents"     %  "httpclient"           % "4.1.2"     withSources(),
  "org.scala-stm"                 %% "scala-stm"            % "0.7"       withSources(),
  "org.scala-tools.time"          %  "time_2.9.1"           % "0.5"       ,
  "org.scala-lang"                %  "scala-swing"          % "2.9.1-1"   withSources(),
  "org.clapper"                   %  "grizzled-scala_2.9.2" % "1.0.13"    withSources(),
  "org.jfree"                     %  "jfreechart"           % "1.0.14"    withSources()
)

seq(assemblySettings: _*)

logLevel := Level.Error

// Only show warnings and errors on the screen for compilations.
// This applies to both test:compile and compile and is Info by default
logLevel in compile := Level.Warn
