name := "coap_kafka_spark"

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.3.11"

libraryDependencies ++= Seq(
  "org.eclipse.californium" % "californium-core" % "1.0.0",
  "org.apache.spark" %% "spark-core" % "1.5.2")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"