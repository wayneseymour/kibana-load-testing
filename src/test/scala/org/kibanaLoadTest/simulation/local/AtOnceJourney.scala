package org.kibanaLoadTest.simulation.local

import org.kibanaLoadTest.simulation.BaseSimulation
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import org.kibanaLoadTest.scenario.{Canvas, Dashboard, Discover}

class AtOnceJourney extends BaseSimulation {
  // overriding default users count for build testing
  defaultNbUsers = 250

  val scnDiscover: ScenarioBuilder = scenario(scenarioName("discover"))
    .exec(loginStep.pause(props.loginPause))
    .exec(Discover.doQuery(appConfig.baseUrl, defaultHeaders))

  val scnDashboard: ScenarioBuilder = scenario(scenarioName("dashboard"))
    .exec(loginStep.pause(props.loginPause))
    .exec(Dashboard.load(appConfig.baseUrl, defaultHeaders))

  val scnCanvas: ScenarioBuilder = scenario(scenarioName("canvas"))
    .exec(loginStep.pause(props.loginPause))
    .exec(Canvas.loadWorkpad(appConfig.baseUrl, defaultHeaders))

  setUp(
    scnDiscover
      .inject(atOnceUsers(props.nbUsers), nothingFor(props.scnPause))
      .andThen(
        scnDashboard
          .inject(atOnceUsers(props.nbUsers), nothingFor(props.scnPause))
          .andThen(scnCanvas.inject(atOnceUsers(props.nbUsers)))
      )
  ).protocols(httpProtocol).maxDuration(props.simulationTimeout)
}
