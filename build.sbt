// see https://github.com/sbt/sbt-assembly
//import AssemblyKeys._ // put this at the top of the file

organization := "com.micronautics"

name := "executorBenchmark"

version := "0.1"

scalaVersion := "2.10.1"

scalaVersion in update := "2.10"

autoCompilerPlugins := true

javaOptions in run += "-Xbatch -server -Xmx1G -Xms1G -XX:PermSize=64m -XX:MaxPermSize=64m"

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8")

resolvers ++= Seq(
  "Akka Snapshots"           at "http://akka.io/snapshots",
  "Typesafe Snapshots"       at "http://repo.typesafe.com/typesafe/snapshots",
  "Typesafe Releases"        at "http://repo.typesafe.com/typesafe/releases",
  "Scala-Tools Snapshots"    at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Scala Tools Releases"     at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype Nexus Releases"  at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype Legacy Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype staging"         at "https://oss.sonatype.org/content/repositories/staging",
  "Sonatype Snapshots"       at "https://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"             %%  "akka-actor"           % "2.1.2"     withSources(),
  "com.github.scala-incubator.io" %% "scala-io-core"         % "0.4.2"     withSources(),
  "com.github.scala-incubator.io" %% "scala-io-file"         % "0.4.2"     withSources(),
  "org.apache.httpcomponents"     %  "httpclient"            % "4.1.3"     withSources(),
  "org.scala-stm"                 %% "scala-stm"             % "0.7"       withSources(),
  "org.scalaj"                    %  "scalaj-time_2.10.0-M7" % "0.6" withSources(),
  "org.scala-lang"                %  "scala-swing"           % "2.10.1"   withSources(),
  "org.clapper"                   %%  "grizzled-scala"       % "1.1.3"    withSources(),
  "org.jfree"                     %  "jfreechart"            % "1.0.14"    withSources()
)

//seq(assemblySettings: _*)

logLevel := Level.Error

// Only show warnings and errors on the screen for compilations.
// This applies to both test:compile and compile and is Info by default
logLevel in compile := Level.Warn
