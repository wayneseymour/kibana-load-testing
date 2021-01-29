package org.kibanaLoadTest

class RunConfiguration {
  // -DdeploymentConfig=path/to/config, default one deploys basic instance on GCP
  val cloudDeployConfigFile =
    System.getProperty("deploymentConfig", "config/deploy/default.conf")
  // -DcloudDeployVersion=8.0.0-SNAPSHOT, optional to deploy Cloud instance
  val cloudDeployVersion: Option[String] = Option(
    System.getProperty("cloudStackVersion")
  )
  // -DenvConfig=path/to/config, default is a local instance
  val envConfigurationFile: String =
    System.getProperty("env", "config/local.conf")
}
