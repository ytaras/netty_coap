name := "coap_kafka_spark"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.1" % "test",
  "org.scalatest"  %% "scalatest"  % "2.2.4"    % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.1",
  "io.kamon" % "sigar-loader" % "1.6.6-rev002"
)
