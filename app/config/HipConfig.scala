package config

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject

class HipConfig @Inject() (servicesConfig: ServicesConfig) {

  def baseUrl: String = servicesConfig.baseUrl("hip")

  def clientId: String = servicesConfig.getConfString("hip.clientId", "")

  def clientSecret: String = servicesConfig.getConfString("hip.clientSecret", "")

  def originatorId: String = servicesConfig.getConfString("hip.originatorId", "")
}
