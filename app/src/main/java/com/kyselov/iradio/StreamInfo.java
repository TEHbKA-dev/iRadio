package com.kyselov.iradio;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class StreamInfo {
    private static volatile StreamInfo Instance;

    private String radioStation;
    private String radioStationMeta;
    private String radioStationPrefix;
    private Bitmap radioStationCover;

    public static StreamInfo get() {
        StreamInfo localInstance = Instance;
        if (localInstance == null) {
            synchronized (StreamInfo.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new StreamInfo();
                }
            }
        }
        return localInstance;
    }

    public String getRadioStation() {
        return radioStation;
    }

    public void setRadioStation(String radioStation) {
        this.radioStation = radioStation;
    }

    public String getRadioStationMeta() {
        return radioStationMeta;
    }

    public void setRadioStationMeta(String radioStationMeta) {
        this.radioStationMeta = radioStationMeta;
    }

    public String getRadioStationPrefix() {
        return radioStationPrefix;
    }

    public void setRadioStationPrefix(String radioStationPrefix) {
        this.radioStationPrefix = radioStationPrefix;
    }

    public Bitmap getRadioStationCover() {
        return radioStationCover;
    }

    public void setRadioStationCover(Bitmap radioStationCover) {
        this.radioStationCover = radioStationCover;
    }

    public void setRadioStationCover(BitmapDrawable radioStationCover) {
        setRadioStationCover(radioStationCover.getBitmap());
    }
}
