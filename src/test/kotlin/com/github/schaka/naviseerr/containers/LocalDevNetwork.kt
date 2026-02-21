package com.github.schaka.naviseerr.containers

import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.Network

private const val NETWORK_NAME = "naviseerr-local-dev"

fun createLocalDevNetwork(): Network {
    val dockerClient = DockerClientFactory.instance().client()
    dockerClient.listNetworksCmd().exec()
        .filter { it.name == NETWORK_NAME }
        .forEach { network ->
            network.containers?.keys?.forEach { containerId ->
                runCatching {
                    dockerClient.disconnectFromNetworkCmd()
                        .withNetworkId(network.id)
                        .withContainerId(containerId)
                        .withForce(true)
                        .exec()
                }
            }

            Thread.sleep(5000)
            dockerClient.removeNetworkCmd(network.id).exec()
        }

    Thread.sleep(5000)
    return Network.builder()
        .createNetworkCmdModifier { cmd -> cmd.withName(NETWORK_NAME) }
        .build()
}
