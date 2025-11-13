package com.back.pinco.global.geometry;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;


@RequiredArgsConstructor
public class GeometryUtil {

    public static final int SRID = 4326;   // WGS84 좌표계
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

    /**
     * @param longitude 경도
     * @param latitude  위도
     */
    public static Point createPoint(double longitude, double latitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    /** Point 객체에서 경도를 추출 */
    public static double getLongitude(Point point) {
        return point.getX();
    }

    /** Point 객체에서 위도를 추출 */
    public static double getLatitude(Point point) {
        return point.getY();
    }

}
