package com.pritam.liquidchat.ui

/** Navigation destinations retain their original route IDs when reordered. */
internal object MbNavigationOrder {
    const val DEFAULT="0,1,2,3,4"
    val titles=listOf("Chats","Calls","Contacts","Settings","Profile")

    fun parse(raw:String?):List<Int> {
        val tokens=raw?.split(",") ?: return listOf(0,1,2,3,4)
        if(tokens.size!=5)return listOf(0,1,2,3,4)
        val values=tokens.map {it.trim().toIntOrNull() ?: return listOf(0,1,2,3,4)}
        return if(values.toSet()==setOf(0,1,2,3,4)) values
            else listOf(0,1,2,3,4)
    }

    fun normalize(raw:String?):String=parse(raw).joinToString(",")

    fun move(raw:String?,destination:Int,direction:Int):String {
        require(destination in 0..4) {"Unknown navigation tab"}
        require(direction==1 || direction == -1) {"Only adjacent moves are supported"}
        val order=parse(raw).toMutableList()
        val from=order.indexOf(destination)
        val to=from+direction
        if(to in order.indices) {
            val other=order[to]
            order[to]=destination
            order[from]=other
        }
        return order.joinToString(",")
    }
}
