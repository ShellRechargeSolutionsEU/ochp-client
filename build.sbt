import sbt._

val cxfVersion = "3.1.10"

def cxfRt(lib: String) =
  "org.apache.cxf" % s"cxf-rt-$lib" % cxfVersion

def specs(lib: String) =
  "org.specs2" %% s"specs2-$lib" % "3.8.9"


val ochp = (project in file("."))
  .enablePlugins(OssLibPlugin)
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    cxf.settings,
    soapui.settings,

    organization := "com.newmotion",
    name := "ochp-client",
    moduleName := name.value,

    scalaVersion := tnm.ScalaVersion.curr,
    crossScalaVersions := Seq(tnm.ScalaVersion.curr, tnm.ScalaVersion.prev),

    libraryDependencies ++= Seq(
      cxfRt("frontend-jaxws"),
      cxfRt("transports-http"),
      cxfRt("ws-security"),
      "com.sun.xml.messaging.saaj" % "saaj-impl" % "1.3.28",
      "com.github.nscala-time" %% "nscala-time" % "2.16.0",
      "org.slf4j" % "slf4j-api" % "1.7.25",

      "com.typesafe" % "config" % "1.3.1" % "it,test",
      specs("junit") % "it,test",
      specs("mock") % "it,test"
    ),

    cxf.cxfVersion := cxfVersion,
    cxf.wsdls := Seq(
      cxf.Wsdl(
        (resourceDirectory in Compile).value / "wsdl" / "ochp-1.3.wsdl",
        Seq("-validate", "-xjc-verbose"), "ochp")
    ),
    fork in IntegrationTest := true,
    soapui.mockServices := Seq(
      soapui.MockService(
        (resourceDirectory in IntegrationTest).value / "soapui" / "OCHP-1-3-soapui-project.xml",
        "8088")
    ),
    mappings in (Compile, packageSrc) ++= Path.allSubpaths(target.value / "cxf" / "ochp").toSeq
)

val ochpCommandLine = (project in file("cmdline"))
  .enablePlugins(AppPlugin)
  .dependsOn(ochp)
  .settings(
    organization := "com.newmotion",
    name := "ochp-client-cmdline"
  )