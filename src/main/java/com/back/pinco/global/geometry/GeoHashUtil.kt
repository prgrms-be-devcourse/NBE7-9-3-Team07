package com.back.pinco.global.geometry

import ch.hsr.geohash.BoundingBox
import ch.hsr.geohash.GeoHash

object GeoHashUtil {

    fun getCoveringGeoHashes(latMin: Double, lngMin: Double, latMax: Double, lngMax: Double, precision: Int = 5): Set<String> {
        val hashes = mutableSetOf<String>()

        val step = 0.01 //격자 단위 (위도 경도 각 0.01도)
        var lat = latMin
        while (lat <= latMax) {
            var lng = lngMin
            while (lng <= lngMax) {
                val hash = GeoHash.withCharacterPrecision(lat, lng, precision).toBase32()
                hashes.add(hash)
                lng += step
            }
            lat += step
        }

        return hashes
    }

    fun generateGeoCacheKey(hash: String) = "pin:cache:$hash"

    fun boundingBoxOfGeoHash(hash: String): List<Double> {
        val geoHash = GeoHash.fromGeohashString(hash)
        val bbox = geoHash.boundingBox

        val sw = bbox.southWestCorner
        val ne = bbox.northEastCorner

        return listOf(
            sw.longitude,
            sw.latitude,
            ne.longitude,
            ne.latitude,
        )
    }



}
