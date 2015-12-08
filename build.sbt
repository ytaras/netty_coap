name := "coap_kafka_spark"

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.3.11"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.1" % "test",
  "org.scalatest"  %% "scalatest"  % "2.2.4"    % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
//  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "io.kamon" % "sigar-loader" % "1.6.6-rev002",
  "org.eclipse.californium" % "californium-core" % "1.0.0",
  "org.apache.spark" %% "spark-core" % "1.5.2",
  "org.apache.spark" %% "spark-streaming" % "1.5.2"

)
