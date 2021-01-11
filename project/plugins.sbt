resolvers ++= Seq(
  "TNM" at "https://nexus.thenewmotion.com/content/groups/public",
  Resolver.bintrayIvyRepo("borisnaguet", "ivy")
)

// https://github.com/thenewmotion/sbt-build-seed
addSbtPlugin("com.newmotion" % "sbt-build-seed" % "5.1.1" )

// https://github.com/BorisNaguet/sbt-cxf-wsdl2java
addSbtPlugin("io.github.borisnaguet" % "sbt-cxf-wsdl2java" % "0.2.8")

addSbtPlugin("com.newmotion" % "sbt-soapui-mockservice" % "0.2.0")
