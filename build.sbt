name := "coap_kafka_spark"

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.3.11"

libraryDependencies ++= Seq(
  "org.eclipse.californium" % "californium-core" % "1.0.0",
  "io.netty" % "netty-all" % "4.0.29.Final",
  "log4j" % "log4j" % "1.2.17"
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

mainClass := Some("netty.coap.sample.SampleReverseClient")