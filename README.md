# OCHP client [![Build Status](https://secure.travis-ci.org/NewMotion/ochp-client.png)](http://travis-ci.org/NewMotion/ochp-client)

Client for [OCHP](http://ochp.eu) written in Scala, for Scala 2.12/2.13

## Includes

* Open Clearing House Protocol v1.3 generated client and bean classes with help of [cxf](http://cxf.apache.org)

* Higher-level Scala API to use the OCHP protocol
* Command-line tool to download roaming authorization information and print it as a CSV

## Usage

### Higher-level Scala API

Example:

```scala
val service = new OchpService {
  val conf = OchpConfig(
    wsUri = "http://localhost:8088/mockeCHS-OCHP_1.3",
    liveWsUri = "http://localhost:8088/mockeCHS-OCHP_1.3-live",
    user = "me",
    password = "mypass"
  )

  val client = OchpClient.createCxfClient(conf)
}
```

### Command-line tool

Call it through sbt like:

`sbt 'ochpCommandLine/run https://echs-q.e-clearing.net/service/ochp/v1.3 <your username> <password>'`

The URI here is for the staging version of the eCHS system; strip the "-q" for the production URI.

## Integration tests

Integration tests can be run as follows:

```
sbt it:test
```

In order for these tests to work, valid credentials need to be provided (see `src/it/resources/reference.conf` for reference).
