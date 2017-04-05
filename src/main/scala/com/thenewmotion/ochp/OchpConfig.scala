package com.thenewmotion.ochp

import scala.concurrent.duration._


case class OchpConfig(
  wsUri: String,
  liveWsUri: String = "",
  user: String,
  password: String,
  requestTimeout: Duration = 1.minute
)

