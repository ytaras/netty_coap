name := "coap_kafka_spark"

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.3.11"

libraryDependencies ++= Seq(
  "org.apache.kafka" %% "kafka" % "0.9.0.0",
  "org.scalacheck" %% "scalacheck" % "1.12.1" % "test",
  "org.scalatest"  %% "scalatest"  % "2.2.4"    % "test",
  "org.eclipse.californium" % "californium-core" % "1.0.0",
  "org.apache.spark" %% "spark-core" % "1.5.2",
  "org.apache.spark" %% "spark-streaming" % "1.5.2",
  "org.apache.spark" %% "spark-streaming-kafka" % "1.5.2",
  "com.squants"  %% "squants"  % "0.6.1-SNAPSHOT",
  "co.nstant.in" % "cbor" % "0.5")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"