/*
 * Copyright (C) 2024 sharedwonder (Liu Baihao).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sharedwonder.mc.ptbridge

import java.io.File
import java.util.Hashtable
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.sharedwonder.mc.ptbridge.addon.AddonLoader
import net.sharedwonder.mc.ptbridge.config.ConfigManager
import net.sharedwonder.mc.ptbridge.utils.PlayerProfile
import org.apache.logging.log4j.LogManager

class ProxyServer(val bindPort: Int, host: String, port: Int, accountsFile: File, addonsDir: File, configDir: File) {
    val remoteAddress: String

    val remotePort: Int

    val accounts: MutableMap<String, PlayerProfile>?

    init {
        try {
            LOGGER.info("Initializing...")

            ConfigManager.init(configDir)
            AddonLoader.init(addonsDir)

            var accounts: MutableMap<String, PlayerProfile>? = null
            if (accountsFile.isFile) {
                try {
                    accounts = readAccountsFile(accountsFile)
                } catch (exception: Exception) {
                    LOGGER.error("An error occurred while reading the accounts file: $exception")
                }
                if (accounts != null) {
                    if (refreshTokensIfExpired(accounts)) {
                        try {
                            writeAccountsFile(accountsFile, accounts)
                        } catch (exception: Exception) {
                            LOGGER.error("An error occurred while writing the accounts file: $exception")
                        }
                    }
                }
            }
            this.accounts = accounts

            val lookupResult =
                if (port == 25565) {
                    try {
                        lookupServer(host)
                    } catch (exception: Exception) {
                        null
                    }
                } else null
            remoteAddress = lookupResult?.first ?: host
            remotePort = lookupResult?.second ?: port

            if (LOGGER.isInfoEnabled) {
                LOGGER.info("Remote server: ${if (':' in remoteAddress) "[$remoteAddress]" else remoteAddress}:$remotePort")
            }
        } catch (exception: Throwable) {
            LOGGER.fatal("A fatal exception occurred when initializing", exception)
            throw RuntimeException(exception)
        }
    }

    fun run(): Int {
        LOGGER.info("Starting Minecraft proxy server...")

        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()

        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(channel: SocketChannel) {
                        channel.pipeline().addLast(ProxyServerHandler(this@ProxyServer))
                    }
                })

            val channelFuture = serverBootstrap.bind(bindPort).sync()
            LOGGER.info("Listening on the port $bindPort")
            channelFuture.channel().closeFuture().sync()
            return 0
        } catch (exception: Throwable) {
            LOGGER.fatal("A fatal exception occurred", exception)
            return 1
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ProxyServer::class.java)

        @JvmStatic
        fun start(bindPort: Int, remoteAddress: String, remotePort: Int, accountsFile: File, addonsDir: File, configDir: File): Int {
            val server: ProxyServer
            try {
                server = ProxyServer(bindPort, remoteAddress, remotePort, accountsFile, addonsDir, configDir)
            } catch (exception: RuntimeException) {
                return 2
            }
            return server.run()
        }

        private fun lookupServer(hostname: String): Pair<String, Int> {
            val environment = Hashtable<String, String>()
            environment[DirContext.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.dns.DnsContextFactory"
            val dirContext = InitialDirContext(environment)
            val domain = "_minecraft._tcp.$hostname"
            val attributes = dirContext.getAttributes(domain, arrayOf("SRV"))
            dirContext.close()

            val srv = attributes["srv"].get().toString()
            LOGGER.info("Found a SRV record on $domain: $srv")

            val content = srv.split(' ')
            return content[3] to content[2].toInt()
        }

        private fun refreshTokensIfExpired(accounts: MutableMap<String, PlayerProfile>): Boolean {
            var isModified = false
            for (entry in accounts) {
                val (username, uuid, auth) = entry.value
                if (System.currentTimeMillis() >= (auth ?: continue).expirationTime) {
                    isModified = true
                    LOGGER.info("The access token of the Minecraft account '${entry.key}' is expired, refreshing...")
                    try {
                        entry.setValue(PlayerProfile(username, uuid, auth.refresh()))
                    } catch (exception: RuntimeException) {
                        LOGGER.error("Failed to refresh the access token of the Minecraft account '${entry.key}'", exception)
                    }
                }
            }
            return isModified
        }
    }
}
