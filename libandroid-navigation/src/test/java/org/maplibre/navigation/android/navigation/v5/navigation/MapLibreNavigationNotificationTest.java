package org.maplibre.navigation.android.navigation.v5.navigation;

import android.app.NotificationManager;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.maplibre.navigation.android.navigation.v5.BaseTest;
import org.maplibre.navigation.android.navigation.v5.models.DirectionsAdapterFactory;
import org.maplibre.navigation.android.navigation.v5.models.DirectionsResponse;
import org.maplibre.navigation.android.navigation.v5.models.DirectionsRoute;

import org.maplibre.navigation.android.navigation.v5.routeprogress.RouteProgress;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MapLibreNavigationNotificationTest extends BaseTest {

  private static final String DIRECTIONS_ROUTE_FIXTURE = "directions_v5_precision_6.json";

  @Mock
  NotificationManager notificationManager;

  private DirectionsRoute route;

  @Before
  public void setUp() throws Exception {
    final String json = loadJsonFixture(DIRECTIONS_ROUTE_FIXTURE);
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    DirectionsResponse response = gson.fromJson(json, DirectionsResponse.class);
    route = response.routes().get(0);
  }

  @Ignore
  @Test
  public void sanity() throws Exception {
    MapLibreNavigationNotification mapLibreNavigationNotification = new MapLibreNavigationNotification(
      Mockito.mock(Context.class), Mockito.mock(MapLibreNavigation.class));
    Assert.assertNotNull(mapLibreNavigationNotification);
  }

  @Ignore
  @Test
  public void updateDefaultNotification_onlyUpdatesNameWhenNew() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    MapLibreNavigationNotification mapLibreNavigationNotification = new MapLibreNavigationNotification(
      Mockito.mock(Context.class), Mockito.mock(MapLibreNavigation.class));

    mapLibreNavigationNotification.updateNotification(routeProgress);
    //    notificationManager.getActiveNotifications()[0].getNotification().contentView;
    //    verify(notificationManager, times(1)).getActiveNotifications()[0];
  }
}
