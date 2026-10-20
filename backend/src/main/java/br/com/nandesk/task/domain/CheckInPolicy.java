package br.com.nandesk.task.domain;

import br.com.nandesk.shared.geo.GeoDistance;

/**
 * Validação do check-in (RN-05): válido se {@code distância ≤ raio da feira + min(precisão, 100 m)}, pela fórmula
 * de Haversine. Precisão pior que 100 m é aceita, mas marcada {@code LOW_ACCURACY}. O geofence é da feira, não do
 * box, porque o GPS dentro dos mercados é impreciso (ADR-0008).
 */
public record CheckInPolicy(int accuracyToleranceMeters) {

    public record Result(boolean accepted, int distanceMeters, CheckinFlag flag) {}

    public Result evaluate(
            double marketLat, double marketLng, int radiusMeters, double lat, double lng, double accuracyMeters) {
        double distance = GeoDistance.meters(marketLat, marketLng, lat, lng);
        double tolerance = Math.min(Math.max(accuracyMeters, 0), accuracyToleranceMeters);
        boolean accepted = distance <= radiusMeters + tolerance;
        CheckinFlag flag = accuracyMeters > accuracyToleranceMeters ? CheckinFlag.LOW_ACCURACY : null;
        return new Result(accepted, (int) Math.round(distance), accepted ? flag : null);
    }
}
