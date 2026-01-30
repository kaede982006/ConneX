package com.connex.app.data.remote.ws

import kotlinx.coroutines.flow.Flow

interface ChatSocket {
    fun connect(roomId: String, channelId: String)
    fun disconnect()
    fun send(event: OutgoingWsEvent)
    fun events(): Flow<IncomingWsEvent>
}
