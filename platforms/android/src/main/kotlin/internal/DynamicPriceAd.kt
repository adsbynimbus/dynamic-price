package com.adsbynimbus.dynamicprice.internal

import androidx.collection.LruCache
import com.adsbynimbus.NimbusAd
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
internal class DynamicPriceAd(
    @SerialName("na_id") val auctionId: String,
    @SerialName("ga_click") val clickTracker: String,
) {
    @Transient val nimbusAd: NimbusAd? = cache.remove(auctionId)

    fun trackClick(scope: CoroutineScope = coroutineScope) {
        scope.launch(Dispatchers.IO) {
            val isSuccessful = OneShotConnection(clickTracker).use { it.isSuccessful }
            // TODO Maybe add logging?
        }
    }

    companion object {
        fun from(info: String): DynamicPriceAd = jsonSerializer.decodeFromString(info)

        val cache = LruCache<String, NimbusAd>(20)

        @OptIn(ExperimentalSerializationApi::class)
        val jsonSerializer = Json {
            coerceInputValues = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

        val coroutineScope = CoroutineScope(Dispatchers.Default) + CoroutineName("DynamicPrice")
    }
}
