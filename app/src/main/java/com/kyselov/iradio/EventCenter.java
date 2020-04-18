package com.kyselov.iradio;

import android.util.SparseArray;

import androidx.annotation.UiThread;

import java.util.ArrayList;

public class EventCenter {

    private static int totalEvents = 1;

    private SparseArray<ArrayList<EventCenterDelegate>> observers = new SparseArray<>();
    private SparseArray<ArrayList<EventCenterDelegate>> removeAfterBroadcast = new SparseArray<>();
    private SparseArray<ArrayList<EventCenterDelegate>> addAfterBroadcast = new SparseArray<>();

    private int broadcasting = 0;

    public static final int radioPlayingPlayStateChanged = totalEvents++;
    public static final int radioPlayingUpdateStreamPosition = totalEvents++;
    public static final int radioUpdateTitleAndArtist = totalEvents++;
    public static final int radioUpdateCover = totalEvents++;
    public static final int radioShowAlertError = totalEvents++;
    public static final int screenStateChanged = totalEvents++;
    public static final int restartPlaying = totalEvents++;
    public static final int restartPlayingError = totalEvents++;
    public static final int itemPlayingPlayStateChanged = totalEvents++;

    public interface EventCenterDelegate {
        void didReceivedEvent(int id, Object... args);
    }

    private static volatile EventCenter Instance;

    @UiThread
    public static EventCenter get() {

        EventCenter localInstance = Instance;

        if (localInstance == null) {
            synchronized (EventCenter.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new EventCenter();
                }
            }
        }

        return localInstance;
    }

    @UiThread
    public void postEventName(int id, Object... args) {

        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("postEventName allowed only from MAIN thread");
            }
        }

        broadcasting++;
        ArrayList<EventCenterDelegate> objects = observers.get(id);
        if (objects != null && !objects.isEmpty()) {
            for (int a = 0; a < objects.size(); a++) {
                EventCenterDelegate obj = objects.get(a);
                obj.didReceivedEvent(id, args);
            }
        }

        broadcasting--;
        if (broadcasting == 0) {
            if (removeAfterBroadcast.size() != 0) {
                for (int a = 0; a < removeAfterBroadcast.size(); a++) {
                    int key = removeAfterBroadcast.keyAt(a);
                    ArrayList<EventCenterDelegate> arrayList = removeAfterBroadcast.get(key);
                    for (int b = 0; b < arrayList.size(); b++) {
                        removeObserver(arrayList.get(b), key);
                    }
                }
                removeAfterBroadcast.clear();
            }

            if (addAfterBroadcast.size() != 0) {
                for (int a = 0; a < addAfterBroadcast.size(); a++) {
                    int key = addAfterBroadcast.keyAt(a);
                    ArrayList<EventCenterDelegate> arrayList = addAfterBroadcast.get(key);
                    for (int b = 0; b < arrayList.size(); b++) {
                        addObserver(arrayList.get(b), key);
                    }
                }
                addAfterBroadcast.clear();
            }
        }
    }

    public void addObserver(EventCenterDelegate observer, int id) {
        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("addObserver allowed only from MAIN thread");
            }
        }

        if (broadcasting != 0) {
            ArrayList<EventCenterDelegate> arrayList = addAfterBroadcast.get(id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                addAfterBroadcast.put(id, arrayList);
            }
            arrayList.add(observer);
            return;
        }

        ArrayList<EventCenterDelegate> objects = observers.get(id);
        if (objects == null) {
            observers.put(id, (objects = new ArrayList<>()));
        }

        if (objects.contains(observer)) {
            return;
        }

        objects.add(observer);
    }

    public void removeObserver(EventCenterDelegate observer, int id) {
        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("removeObserver allowed only from MAIN thread");
            }
        }

        if (broadcasting != 0) {
            ArrayList<EventCenterDelegate> arrayList = removeAfterBroadcast.get(id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                removeAfterBroadcast.put(id, arrayList);
            }
            arrayList.add(observer);
            return;
        }

        ArrayList<EventCenterDelegate> objects = observers.get(id);

        if (objects != null) {
            objects.remove(observer);
        }
    }

    public boolean hasObservers(int id) {
        return observers.indexOfKey(id) >= 0;
    }
}
