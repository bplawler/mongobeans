name := "mongobeans"

version := "0.3.7"

scalaVersion := "2.9.1"

crossPaths in ThisBuild := false

resolvers ++= Seq(
    "sonatype"  at "http://oss.sonatype.org/content/repositories/scala-tools/"
  , "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
  , "releases"  at "http://oss.sonatype.org/content/repositories/releases")

libraryDependencies ++= Seq (
    "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
  , "org.specs2" %% "specs2" % "1.12.4" % "test"
)

scalacOptions in ThisBuild += "-deprecation"

scalacOptions in ThisBuild += "-unchecked"
