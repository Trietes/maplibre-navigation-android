package org.maplibre.navigation.android.navigation.v5.navigation;

import static org.maplibre.navigation.android.navigation.v5.navigation.NavigationHelper.buildSnappedLocation;
import static org.maplibre.navigation.android.navigation.v5.navigation.NavigationHelper.checkMilestones;
import static org.maplibre.navigation.android.navigation.v5.navigation.NavigationHelper.isUserOffRoute;

import android.location.Location;
import android.os.Handler;
import android.os.Message;

import org.maplibre.navigation.android.navigation.v5.milestone.Milestone;
import org.maplibre.navigation.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

class RouteProcessorHandlerCallback implements Handler.Callback {

    private final NavigationRouteProcessor routeProcessor;
    private final RouteProcessorBackgroundThread.Listener listener;
    private final Handler responseHandler;

    RouteProcessorHandlerCallback(NavigationRouteProcessor routeProcessor, Handler responseHandler,
            RouteProcessorBackgroundThread.Listener listener) {
        this.routeProcessor = routeProcessor;
        this.responseHandler = responseHandler;
        this.listener = listener;
    }

    @Override
    public boolean handleMessage(Message msg) {
        NavigationLocationUpdate update = ((NavigationLocationUpdate) msg.obj);
        handleRequest(update);
        return true;
    }

    /**
     * Takes a new location model and runs all related engine checks against it
     * (off-route, milestones, snapped location, and faster-route).
     * <p>
     * After running through the engines, all data is submitted to {@link NavigationService} via
     * {@link RouteProcessorBackgroundThread.Listener}.
     *
     * @param update hold location, navigation (with options), and distances away from maneuver
     */
    private void handleRequest(final NavigationLocationUpdate update) {
        final MapLibreNavigation mapLibreNavigation = update.mapLibreNavigation();
        final Location rawLocation = update.location();
        RouteProgress routeProgress = routeProcessor.buildNewRouteProgress(mapLibreNavigation, rawLocation);

        final boolean userOffRoute = determineUserOffRoute(update, mapLibreNavigation, routeProgress);
        final List<Milestone> milestones = findTriggeredMilestones(mapLibreNavigation, routeProgress);
        final Location location = findSnappedLocation(mapLibreNavigation, rawLocation, routeProgress, userOffRoute);

        final RouteProgress finalRouteProgress = updateRouteProcessorWith(routeProgress);
        sendUpdateToListener(userOffRoute, milestones, location, finalRouteProgress);
    }

    private List<Milestone> findTriggeredMilestones(MapLibreNavigation mapLibreNavigation, RouteProgress routeProgress) {
        RouteProgress previousRouteProgress = routeProcessor.getRouteProgress();
        return checkMilestones(previousRouteProgress, routeProgress, mapLibreNavigation);
    }

    private Location findSnappedLocation(MapLibreNavigation mapLibreNavigation, Location rawLocation,
                                         RouteProgress routeProgress, boolean userOffRoute) {
        boolean snapToRouteEnabled = mapLibreNavigation.options().snapToRoute();
        return buildSnappedLocation(mapLibreNavigation, snapToRouteEnabled,
                rawLocation, routeProgress, userOffRoute);
    }

    private boolean determineUserOffRoute(NavigationLocationUpdate navigationLocationUpdate,
                                          MapLibreNavigation mapLibreNavigation, RouteProgress routeProgress) {
        final boolean userOffRoute = isUserOffRoute(navigationLocationUpdate, routeProgress, routeProcessor);
        routeProcessor.checkIncreaseIndex(mapLibreNavigation);
        return userOffRoute;
    }

    private RouteProgress updateRouteProcessorWith(RouteProgress routeProgress) {
        routeProcessor.setRouteProgress(routeProgress);
        return routeProgress;
    }

    private void sendUpdateToListener(final boolean userOffRoute, final List<Milestone> milestones,
            final Location location, final RouteProgress finalRouteProgress) {
        responseHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onNewRouteProgress(location, finalRouteProgress);
                listener.onMilestoneTrigger(milestones, finalRouteProgress);
                listener.onUserOffRoute(location, userOffRoute);
            }
        });
    }
}
