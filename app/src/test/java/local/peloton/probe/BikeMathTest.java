package local.peloton.probe;

import static org.junit.Assert.*;

import org.junit.Test;

public class BikeMathTest {
  @Test
  public void rawPowerIsCentiwatts() {
    assertEquals(59.0, BikeMath.watts(5900), 0.0);
    assertEquals(1.01, BikeMath.watts(101), 0.00001);
  }

  @Test
  public void invalidAndStoppedPowerHaveZeroSpeed() {
    for (double input : new double[] {0, -1, 0.09, Double.NaN, Double.POSITIVE_INFINITY})
      assertEquals(0, BikeMath.speedMph(input), 0);
  }

  @Test
  public void speedReferenceValues() {
    assertEquals(0.565, BikeMath.speedMph(1), 0.00001);
    assertEquals(16.215, BikeMath.speedMph(100), 0.00001);
    assertTrue(BikeMath.speedMph(26) > BikeMath.speedMph(25));
  }

  @Test
  public void speedRisesAcrossNormalWorkouts() {
    double previous = 0;
    for (int watts = 1; watts <= 1500; watts++) {
      double speed = BikeMath.speedMph(watts);
      assertTrue("power=" + watts, speed >= previous);
      previous = speed;
    }
  }

  @Test
  public void metricConversion() {
    assertEquals(16.09344, BikeMath.displaySpeed(10, true), 0.000001);
    assertEquals(10, BikeMath.displaySpeed(10, false), 0);
  }

  @Test
  public void staleOrFutureFramesAreNotLive() {
    assertTrue(BikeMath.fresh(3999, 1000));
    assertFalse(BikeMath.fresh(4000, 1000));
    assertFalse(BikeMath.fresh(1000, 1001));
    assertFalse(BikeMath.fresh(1000, 0));
  }

  @Test
  public void panelCannotEscapeSmallScreens() {
    assertEquals(0, BikeMath.clamp(1400, -20));
    assertEquals(500, BikeMath.clamp(1400, 500));
    assertEquals(0, BikeMath.clamp(-100, 500));
  }
}
