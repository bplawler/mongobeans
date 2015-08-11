organization := "default"

name := "mongobeans"

version := "0.3.8"

resolvers ++= Seq(
    "sonatype"  at "https://oss.sonatype.org/content/repositories/scala-tools/"
  , "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  , "releases"  at "https://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq (
    "org.mongodb" %% "casbah" % "2.8.1"
  , "org.specs2" % "specs2_2.10" % "1.14" % "test"
)

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked")
