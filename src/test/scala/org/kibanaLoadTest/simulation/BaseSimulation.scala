package org.kibanaLoadTest.simulation

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import org.kibanaLoadTest.{KibanaConfiguration, RunConfiguration}
import org.kibanaLoadTest.helpers.{
  CloudHttpClient,
  HttpHelper,
  HttpProtocol,
  SimulationActions
}
import org.kibanaLoadTest.scenario.Login
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class BaseSimulation extends Simulation {
  // should be overridden in simulation class
  var defaultNbUsers = 50

  object props {
    val nbUsers: Int =
      Option(System.getProperty("users").toInt).getOrElse(defaultNbUsers)
    val simulationTimeout: FiniteDuration =
      FiniteDuration(
        10,
        TimeUnit.MINUTES
      )
    val loginPause: FiniteDuration =
      FiniteDuration(
        2,
        TimeUnit.SECONDS
      )
    val scnPause: FiniteDuration =
      FiniteDuration(
        20,
        TimeUnit.SECONDS
      )
    val run = new RunConfiguration()
  }

  def scenarioName(module: String): String = {
    s"Cloud  atOnce $module ${appConfig.buildVersion}"
  }

  val logger: Logger = LoggerFactory.getLogger("Base Simulation")

  // appConfig is used to run load tests
  val appConfig: KibanaConfiguration = SimulationActions.getKibanaConfiguration(
    cloudDeployVersion = props.run.cloudDeployVersion,
    cloudDeployConfigFile = props.run.cloudDeployConfigFile,
    envConfigurationFile = props.run.envConfigurationFile,
    logger = logger
  )
  // can be overridden
  var defaultHeaders: Map[String, String] = HttpProtocol.getDefaultHeaders(
    appConfig.buildVersion,
    appConfig.baseUrl
  )
  // can be overridden
  var defaultTextHeaders: Map[String, String] = HttpProtocol.getTextHeaders
  // can be overridden
  var loginStep: ChainBuilder = Login
    .doLogin(
      appConfig.isSecurityEnabled,
      appConfig.loginPayload,
      appConfig.loginStatusCode
    )

  if (appConfig.isSecurityEnabled) {
    defaultHeaders += ("Cookie" -> "${Cookie}")
    defaultTextHeaders += ("Cookie" -> "${Cookie}")
  }

  before {
    appConfig.print()
    // saving deployment info to target/lastDeployment.txt"
    SimulationActions.saveDeploymentMetaToFile(appConfig, props.nbUsers)
    // load sample data
    logger.info(s"Loading sample data")
    new HttpHelper(appConfig).addSampleData("ecommerce")
  }

  after {
    if (appConfig.deploymentId.isDefined) {
      // delete deployment
      new CloudHttpClient().deleteDeployment(appConfig.deploymentId.get)
    } else {
      // remove sample data
      try {
        logger.info(s"Removing sample data")
        new HttpHelper(appConfig).removeSampleData("ecommerce")
      } catch {
        case e: java.lang.RuntimeException =>
          println(s"Can't remove sample data\n ${e.printStackTrace()}")
      }
    }
  }

  logger.info(s"Running ${getClass.getSimpleName} simulation")

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(appConfig.baseUrl)
    .inferHtmlResources(
      BlackList(
        """.*\.js""",
        """.*\.css""",
        """.*\.gif""",
        """.*\.jpeg""",
        """.*\.jpg""",
        """.*\.ico""",
        """.*\.woff""",
        """.*\.woff2""",
        """.*\.(t|o)tf""",
        """.*\.png""",
        """.*detectportal\.firefox\.com.*"""
      ),
      WhiteList()
    )
    .acceptHeader(
      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
    )
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-GB,en-US;q=0.9,en;q=0.8")
    .userAgentHeader(
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"
    )
}
