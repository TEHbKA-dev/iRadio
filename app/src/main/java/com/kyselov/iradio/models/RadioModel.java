package com.kyselov.iradio.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class RadioModel {

    private String title;
    private String prefix;


    private String icon;
    private String iconPng;
    private String stream;

    public RadioModel() {
        // Default constructor required for calls to DataSnapshot.getValue(Radio.class)
    }

    public String getTitle() {
        return title;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getIcon() {
        return icon;
    }

    @PropertyName("icon_png")
    public String getIconPng() {
        return iconPng;
    }

    @PropertyName("icon_png")
    public void setIconPng(String iconPng) {
        this.iconPng = iconPng;
    }

    public String getStream() {
        return stream;
    }

    @Exclude
    public Map<String, Object> toMap() {
        final HashMap<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("prefix", prefix);
        result.put("icon", icon);
        result.put("icon_png", iconPng);
        result.put("stream", stream);

        return result;
    }
}
