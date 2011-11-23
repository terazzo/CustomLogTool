import sbt._
import Keys._


object CustomLogToolBuild extends Build {

   val name = "custom-log-tool"
   val version = "0.1"

   val buildSettings = Defaults.defaultSettings ++ Seq(
   
      unmanagedBase <<= baseDirectory { base => base / "lib" },

      javacOptions ++= Seq("-J-Dfile.encoding=UTF8", "-encoding", "UTF-8"),
   
      libraryDependencies ++= Seq(
         "junit" % "junit" % "4.8.1" % "test",
         "org.mortbay.jetty" % "jetty" % "6.1.22",
         "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22",
         "commons-lang" % "commons-lang" % "2.6",
         "commons-cli" % "commons-cli" % "1.2",
         "org.apache.httpcomponents" % "httpclient" % "4.0.2",
         "commons-logging" % "commons-logging" % "1.1.1"
      ),
      artifactName := { (config: String, module: ModuleID, artifact: Artifact) =>
         artifact.name + "-" + module.revision + "." + artifact.extension
      }
   )



   lazy val root = Project("custom-log-tool", file("."), settings = buildSettings)

}

