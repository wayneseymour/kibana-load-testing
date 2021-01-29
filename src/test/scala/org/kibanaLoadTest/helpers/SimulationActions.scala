package org.kibanaLoadTest.helpers

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.kibanaLoadTest.KibanaConfiguration
import org.slf4j.Logger

import java.io.File
import java.nio.file.Paths

object SimulationActions {

  def createDeployment(
      stackVersion: String,
      deployFile: String,
      logger: Logger
  ): KibanaConfiguration = {
    logger.info(
      s"Reading deployment configuration: $deployFile"
    )
    val config =
      Helper.readResourceConfigFile(deployFile)
    val version = new Version(stackVersion)
    val providerName = if (version.isAbove79x) "cloud-basic" else "basic-cloud"
    val cloudClient = new CloudHttpClient
    val payload = cloudClient.preparePayload(stackVersion, config)
    val metadata = cloudClient.createDeployment(payload)
    val isReady = cloudClient.waitForClusterToStart(metadata("deploymentId"))
    if (!isReady) {
      cloudClient.deleteDeployment(metadata("deploymentId"))
      throw new RuntimeException("Stop due to failed deployment...")
    }
    val host = cloudClient.getKibanaUrl(metadata("deploymentId"))
    val cloudConfig = ConfigFactory
      .load()
      .withValue(
        "deploymentId",
        ConfigValueFactory.fromAnyRef(metadata("deploymentId"))
      )
      .withValue("app.host", ConfigValueFactory.fromAnyRef(host))
      .withValue(
        "app.version",
        ConfigValueFactory.fromAnyRef(version.get)
      )
      .withValue("security.on", ConfigValueFactory.fromAnyRef(true))
      .withValue("auth.providerType", ConfigValueFactory.fromAnyRef("basic"))
      .withValue(
        "auth.providerName",
        ConfigValueFactory.fromAnyRef(providerName)
      )
      .withValue(
        "auth.username",
        ConfigValueFactory.fromAnyRef(metadata("username"))
      )
      .withValue(
        "auth.password",
        ConfigValueFactory.fromAnyRef(metadata("password"))
      )

    new KibanaConfiguration(cloudConfig)
  }

  def getKibanaConfiguration(
      cloudDeployVersion: Option[String],
      cloudDeployConfigFile: String,
      envConfigurationFile: String,
      logger: Logger
  ): KibanaConfiguration = {
    // appConfig is used to run load tests
    if (cloudDeployVersion.isDefined) {
      // create new deployment on Cloud
      SimulationActions
        .createDeployment(
          cloudDeployVersion.get,
          cloudDeployConfigFile,
          logger
        )
        .syncWithInstance()
      // use existing deployment or local instance
    } else
      new KibanaConfiguration(
        Helper.readResourceConfigFile(envConfigurationFile)
      ).syncWithInstance()
  }

  def saveDeploymentMetaToFile(
      config: KibanaConfiguration,
      users: Int
  ): Unit = {
    val meta = Map(
      "deploymentId" -> (if (config.deploymentId.isDefined)
                           config.deploymentId.get
                         else ""),
      "baseUrl" -> config.baseUrl,
      "buildHash" -> config.buildHash,
      "buildNumber" -> config.buildNumber,
      "version" -> config.version,
      "isSnapshotBuild" -> config.isSnapshotBuild,
      "branch" -> (if (config.branchName.isDefined)
                     config.branchName.get
                   else ""),
      "usersCount" -> users
    )
    val lastDeploymentFilePath: String = Paths
      .get("target")
      .toAbsolutePath
      .normalize
      .toString + File.separator + "lastDeployment.txt"
    Helper.writeMapToFile(meta, lastDeploymentFilePath)
  }

}
