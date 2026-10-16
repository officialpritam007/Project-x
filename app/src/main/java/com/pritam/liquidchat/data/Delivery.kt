package com.pritam.liquidchat.data

/** A push submission never advances delivery. Only recipient watermarks can do so. */
object Delivery {
    fun label(status:String,seq:Long,delivered:Long,read:Long):String = when {
        status!="SENT"->status.lowercase().replaceFirstChar {it.uppercase()}
        seq>0 && read>=seq->"Read"
        seq>0 && delivered>=seq->"Delivered"
        else->"Sent"
    }
    fun retryable(status:String,attempts:Int,transient:Boolean):Boolean = status!="FAILED" && attempts<5 && transient
}
