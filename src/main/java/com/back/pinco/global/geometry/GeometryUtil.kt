package com.back.pinco.global.geometry

import lombok.RequiredArgsConstructor
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

@RequiredArgsConstructor
object GeometryUtil {
    const val SRID: Int = 4326 // WGS84 좌표계
    private val geometryFactory = GeometryFactory(PrecisionModel(), SRID)

    /**
     * @param longitude 경도
     * @param latitude  위도
     */
    @JvmStatic
    fun createPoint(longitude: Double, latitude: Double): Point =
        geometryFactory.createPoint(Coordinate(longitude, latitude))

    /** Point 객체에서 경도를 추출  */
    val Point.longitude: Double
        get() = x;

    /** Point 객체에서 위도를 추출  */
    val Point.latitude: Double
        get() = x;
}
