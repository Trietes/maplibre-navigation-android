package org.maplibre.navigation.android.navigation.v5.offroute;

import android.location.Location;

import org.maplibre.navigation.android.navigation.v5.BaseTest;
import org.maplibre.navigation.android.navigation.v5.models.LegStep;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.Point;
import org.maplibre.navigation.android.navigation.v5.navigation.MapLibreNavigationOptions;
import org.maplibre.navigation.android.navigation.v5.routeprogress.RouteProgress;
import org.maplibre.navigation.android.navigation.v5.utils.Constants;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OffRouteDetectorTest extends BaseTest {

  @Mock
  private Location mockLocation;
  @Mock
  private RouteProgress mockProgress;
  @Mock
  private OffRouteCallback mockCallback;
  private OffRouteDetector offRouteDetector;
  private MapLibreNavigationOptions options;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    options = MapLibreNavigationOptions.builder().build();

    offRouteDetector = new OffRouteDetector();
    offRouteDetector.setOffRouteCallback(mockCallback);
  }

  @Test
  public void sanity() throws Exception {
    assertNotNull(offRouteDetector);
  }

  @Test
  public void invalidOffRoute_onFirstLocationUpdate() throws Exception {
    when(mockProgress.distanceRemaining()).thenReturn(1000d);

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(mockLocation, mockProgress, options);

    assertFalse(isUserOffRoute);
  }

  @Test
  public void validOffRoute_onMinimumDistanceBeforeReroutingPassed() throws Exception {
    Location mapboxOffice = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    when(mockProgress.distanceRemaining()).thenReturn(1000d);
    offRouteDetector.isUserOffRoute(mockLocation, mockProgress, options);
    Point target = buildPointAwayFromLocation(mapboxOffice, options.minimumDistanceBeforeRerouting() + 1);
    Location locationOverMinimumDistance = buildDefaultLocationUpdate(target.longitude(), target.latitude());

    boolean validOffRoute = offRouteDetector.isUserOffRoute(locationOverMinimumDistance, routeProgress, options);

    assertTrue(validOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertTrueWhenTooFarFromStep() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();

    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);

    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 100, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);
    assertTrue(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_StepPointSize() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();
    removeAllButOneStepPoints(routeProgress);
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);
    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 50, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);

    assertFalse(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenOnStep() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();

    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);

    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 10, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);
    assertFalse(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenWithinRadiusAndStepLocationHasBadAccuracy() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();

    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);

    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 250, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());
    when(secondUpdate.getAccuracy()).thenReturn(300f);

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);
    assertFalse(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenOffRouteButCloseToUpcomingStep() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point upcomingStepManeuverPoint = routeProgress.currentLegProgress().upComingStep().maneuver().location();

    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);

    Point offRoutePoint = buildPointAwayFromPoint(upcomingStepManeuverPoint, 30, 180);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);
    assertFalse(isUserOffRoute);
    verify(mockCallback, times(1)).onShouldIncreaseIndex();
  }

  @Test
  public void isUserOffRoute_AssertTrueWhenOnRouteButMovingAwayFromManeuver() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options);

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFirstTry = offRouteDetector.isUserOffRoute(secondLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFirstTry);

    Point secondLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
      secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteSecondTry = offRouteDetector.isUserOffRoute(thirdLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSecondTry);

    Point thirdLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fourthLocationUpdate = buildDefaultLocationUpdate(
      thirdLastPointInCurrentStep.longitude(), thirdLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteThirdTry = offRouteDetector.isUserOffRoute(fourthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteThirdTry);

    Point fourthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fifthLocationUpdate = buildDefaultLocationUpdate(
      fourthLastPointInCurrentStep.longitude(), fourthLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFourthTry = offRouteDetector.isUserOffRoute(fifthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFourthTry);

    Point fifthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location sixthLocationUpdate = buildDefaultLocationUpdate(
      fifthLastPointInCurrentStep.longitude(), fifthLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFifthTry = offRouteDetector.isUserOffRoute(sixthLocationUpdate, routeProgress, options);
    assertTrue(isUserOffRouteFifthTry);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenOnRouteMovingAwayButNotFarEnoughFromManeuver() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options);

    Point lastPointInCurrentStep = coordinates.get(7);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFirstTry = offRouteDetector.isUserOffRoute(secondLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFirstTry);

    Point pointSix = coordinates.get(6);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
      pointSix.longitude(), pointSix.latitude()
    );
    boolean isUserOffRouteSecondTry = offRouteDetector.isUserOffRoute(thirdLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSecondTry);

    Location fourthLocationUpdate = buildDefaultLocationUpdate(
      pointSix.longitude(), pointSix.latitude()
    );
    boolean isUserOffRouteThirdTry = offRouteDetector.isUserOffRoute(fourthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteThirdTry);

    Location fifthLocationUpdate = buildDefaultLocationUpdate(
      pointSix.longitude(), pointSix.latitude()
    );
    boolean isUserOffRouteFourthTry = offRouteDetector.isUserOffRoute(fifthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFourthTry);

    Location sixthLocationUpdate = buildDefaultLocationUpdate(
      pointSix.longitude(), pointSix.latitude()
    );
    boolean isUserOffRouteFifthTry = offRouteDetector.isUserOffRoute(sixthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFifthTry);

    Point pointFive = coordinates.get(5);
    Location seventhLocationUpdate = buildDefaultLocationUpdate(
      pointFive.longitude(), pointFive.latitude()
    );
    boolean isUserOffRouteSixthTry = offRouteDetector.isUserOffRoute(seventhLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSixthTry);

    Point pointFour = coordinates.get(4);
    Location eighthLocationUpdate = buildDefaultLocationUpdate(
        pointFour.longitude(), pointFour.latitude()
    );
    boolean isUserOffRouteSeventhTry = offRouteDetector.isUserOffRoute(eighthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSeventhTry);
  }

  @Test
  public void isUserOffRoute_AssertTrueWhenOnRouteMovingAwayWithRightDirectionTraveling() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options);

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
        lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFirstTry = offRouteDetector.isUserOffRoute(secondLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFirstTry);

    Point secondLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
        secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteSecondTry = offRouteDetector.isUserOffRoute(thirdLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSecondTry);

    Point thirdLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fourthLocationUpdate = buildDefaultLocationUpdate(
        thirdLastPointInCurrentStep.longitude(), thirdLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteThirdTry = offRouteDetector.isUserOffRoute(fourthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteThirdTry);

    Point fourthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fifthLocationUpdate = buildDefaultLocationUpdate(
        fourthLastPointInCurrentStep.longitude(), fourthLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFourthTry = offRouteDetector.isUserOffRoute(fifthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFourthTry);

    Location eighthLocationUpdate = buildDefaultLocationUpdate(
        secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteSeventhTry = offRouteDetector.isUserOffRoute(eighthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSeventhTry);

    Point fifthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location sixthLocationUpdate = buildDefaultLocationUpdate(
        fifthLastPointInCurrentStep.longitude(), fifthLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFifthTry = offRouteDetector.isUserOffRoute(sixthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFifthTry);
  }

  @Test
  public void isUserOffRoute_AssertTrueWhenOnRouteMovingAwayWithNotEnoughRightDirectionTraveling() throws Exception {
    MapLibreNavigationOptions options = this.options.toBuilder()
      .offRouteMinimumDistanceMetersBeforeRightDirection(60)
      .build();

    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options);

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFirstTry = offRouteDetector.isUserOffRoute(secondLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFirstTry);

    Point secondLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
      secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteSecondTry = offRouteDetector.isUserOffRoute(thirdLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSecondTry);

    Point thirdLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fourthLocationUpdate = buildDefaultLocationUpdate(
      thirdLastPointInCurrentStep.longitude(), thirdLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteThirdTry = offRouteDetector.isUserOffRoute(fourthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteThirdTry);

    Point fourthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fifthLocationUpdate = buildDefaultLocationUpdate(
      fourthLastPointInCurrentStep.longitude(), fourthLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFourthTry = offRouteDetector.isUserOffRoute(fifthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFourthTry);

    Location eighthLocationUpdate = buildDefaultLocationUpdate(
      secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteSeventhTry = offRouteDetector.isUserOffRoute(eighthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSeventhTry);

    Point fifthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location sixthLocationUpdate = buildDefaultLocationUpdate(
      fifthLastPointInCurrentStep.longitude(), fifthLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFifthTry = offRouteDetector.isUserOffRoute(sixthLocationUpdate, routeProgress, options);
    assertTrue(isUserOffRouteFifthTry);
  }

  @Test
  public void isUserOffRoute_AssertFalseTwoUpdatesAwayFromManeuverThenOneTowards() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();

    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();

    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options);

    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFirstTry = offRouteDetector.isUserOffRoute(secondLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteFirstTry);

    Point secondLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
      secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteSecondTry = offRouteDetector.isUserOffRoute(thirdLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteSecondTry);

    Location fourthLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteThirdTry = offRouteDetector.isUserOffRoute(fourthLocationUpdate, routeProgress, options);
    assertFalse(isUserOffRouteThirdTry);
  }

  @Test
  public void isUserOffRoute_assertTrueWhenRouteDistanceRemainingIsZero() {
    Location location = mock(Location.class);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.distanceRemaining()).thenReturn(0d);

    boolean isOffRoute = offRouteDetector.isUserOffRoute(location, routeProgress, options);

    assertTrue(isOffRoute);
  }

  private void removeAllButOneStepPoints(RouteProgress routeProgress) {
    for (int i = routeProgress.currentStepPoints().size() - 2; i >= 0; i--) {
      routeProgress.currentStepPoints().remove(i);
    }
  }
}
