resolvers += "TNM" at "https://nexus.thenewmotion.com/content/groups/public"

// https://github.com/thenewmotion/sbt-build-seed
addSbtPlugin("com.thenewmotion" % "sbt-build-seed" % "2.1.0" )

// https://github.com/ebiznext/sbt-cxf-wsdl2java
addSbtPlugin("com.ebiznext.sbt.plugins" % "sbt-cxf-wsdl2java" % "0.1.4")

addSbtPlugin("com.thenewmotion" % "sbt-soapui-mockservice" % "0.1.4")
