package org.kibanaLoadTest.simulation.cloud

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import org.kibanaLoadTest.scenario.{Canvas, Dashboard, Discover}
import org.kibanaLoadTest.simulation.BaseSimulation

class AtOnceJourney extends BaseSimulation {
  // overriding default users count for cloud testing
  defaultNbUsers = 80

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
