name := "mongobeans"

version := "0.1"

scalaVersion := "2.8.1"

resolvers += "sonatype" at "https://oss.sonatype.org/content/repositories/scala-tools/"

libraryDependencies ++= Seq (
  "com.mongodb.casbah" %% "casbah" % "2.1.5.0"
)
