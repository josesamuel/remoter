package util.remoter.service

import remoter.annotations.Remoter

@Remoter
interface ISampleKotlinServiceListener {

    suspend fun onEcho(echo: String?)
}