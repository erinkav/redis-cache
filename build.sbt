lazy val akkaHttpVersion = "10.1.7"
lazy val akkaVersion    = "2.5.19"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.rediscache",
      scalaVersion    := "2.12.7"
    )),

    name := "Redis Cache",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "net.debasishg"     %% "redisclient"          % "3.9",
      "com.typesafe.akka" %% "akka-http-caching"    % "10.1.7",
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    )
  )
