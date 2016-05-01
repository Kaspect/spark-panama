name := "Simple Project"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.1"
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies += "io.atlassian.aws-scala" %% "aws-scala"  % "4.0.2"

addSbtPlugin("com.chatwork" % "sbt-aws-s3" % "1.0.19")

addSbtPlugin("com.chatwork" % "sbt-aws-cfn" % "1.0.19")

addSbtPlugin("com.chatwork" % "sbt-aws-eb" % "1.0.19")
