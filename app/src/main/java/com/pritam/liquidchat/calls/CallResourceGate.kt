package com.pritam.liquidchat.calls

import java.io.IOException

/** Short native operations and disposal share a monitor; never hold it across suspension. */
internal class CallResourceGate {
    @Volatile var closed = false
        private set

    @Synchronized fun <T> withOpen(operation:()->T):T {
        if(closed)throw IOException("Call already ended")
        return operation()
    }

    @Synchronized fun ifOpen(operation:()->Unit) {
        if(!closed)operation()
    }

    @Synchronized fun close(dispose:()->Unit) {
        if(closed)return
        closed=true
        dispose()
    }
}
