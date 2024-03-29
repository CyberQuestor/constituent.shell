package com.hs.haystack.tachyon.constituent.shell

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.apache.predictionio.controller.Engine

class EngineTest
  extends FlatSpec with Matchers {

  "apply" should "return a new engine instance" in {
    val engine = VanillaEngine.apply()
    engine shouldBe an [Engine[_,_,_,_,_,_]]
  }
}