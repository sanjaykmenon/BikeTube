package local.peloton.probe;

/**
 * Adapted from OpenRide's PelotonSpeed.kt (Apache-2.0); model originally derived by PeloMon.
 * Modified for Java and finite input validation. See NOTICE.
 */
final class BikeMath {
  static double watts(long centiwatts) {
    return centiwatts / 100.0;
  }

  static double speedMph(double watts) {
    if (!Double.isFinite(watts) || watts < 0.1) return 0;
    double s = Math.sqrt(watts);
    return Math.max(
        0,
        watts < 26
            ? 0.057 - 0.172 * s + 0.759 * s * s - 0.079 * s * s * s
            : -1.635 + 2.325 * s - 0.064 * s * s + 0.001 * s * s * s);
  }

  static double displaySpeed(double mph, boolean metric) {
    return metric ? mph * 1.609344 : mph;
  }

  static boolean fresh(long now, long timestamp) {
    return timestamp > 0 && now >= timestamp && now - timestamp < 3000;
  }

  static int clamp(int n, int max) {
    return Math.max(0, Math.min(Math.max(0, max), n));
  }
}
