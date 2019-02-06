lazy val akkaHttpVersion = "10.1.7"
lazy val akkaVersion    = "2.5.19"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.rediscache",
      scalaVersion    := "2.12.7"
    )),
    scalacOptions := Seq(
      "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
      "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps"),
    testPassUseCucumber := true,
    testTagsToExecute := "ready",
    CucumberPlugin.glue := "steps/",
    unmanagedClasspath in Test += baseDirectory.value / "src/test/resources/features",
    name := "Redis Cache",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "net.debasishg"     %% "redisclient"          % "3.9",
      "com.typesafe.akka" %% "akka-http-caching"    % "10.1.7",
      "io.gatling"        % "gatling-test-framework" % "3.0.3" % Test,
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.3" % Test,
     // "io.cucumber"        %% "cucumberber-scala"      % "4.2.0"         % Test,
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalaj"        %% "scalaj-http"           % "2.4.1"        % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test,
      "org.scalamock"     %%  "scalamock"           % "4.1.0"         % Test,
      "io.cucumber"       % "cucumber-core"         % "2.0.1"         % Test,
      "io.cucumber"       %% "cucumber-scala"       % "2.0.1"         % Test,
      "io.cucumber"       % "cucumber-jvm"          % "2.0.1"         % Test,
      "io.cucumber"       % "cucumber-junit"        % "2.0.1"         % Test
    )
  ).enablePlugins(GatlingPlugin, JavaAppPackaging, CucumberPlugin)
