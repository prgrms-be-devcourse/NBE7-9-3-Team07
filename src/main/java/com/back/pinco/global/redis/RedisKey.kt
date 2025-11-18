package com.back.pinco.global.redis

enum class RedisKey(val prefix: String) {
    ID_PIN("pin"),
    GEO_ID("pin:geo:cache");

    //다른 키도 여기서 관리하면 좋을듯

    fun key(suffix: String) = "$prefix:$suffix"
}
