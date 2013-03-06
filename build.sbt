name := "mongobeans"

version := "0.3.6"

scalaVersion := "2.8.1"

crossPaths in ThisBuild := false

resolvers ++= Seq(
    "sonatype"  at "http://oss.sonatype.org/content/repositories/scala-tools/"
  , "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
  , "releases"  at "http://oss.sonatype.org/content/repositories/releases")

libraryDependencies ++= Seq (
    "com.mongodb.casbah" %% "casbah" % "2.1.5.0"
  , "org.specs2" %% "specs2" % "1.5" % "test"
  , "org.specs2" %% "specs2-scalaz-core" % "5.1-SNAPSHOT" % "test"
)

scalacOptions in ThisBuild += "-deprecation"

scalacOptions in ThisBuild += "-unchecked"
