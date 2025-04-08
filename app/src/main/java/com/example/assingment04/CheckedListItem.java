package com.example.assingment04;

import android.graphics.Bitmap;

public class CheckedListItem {
    private boolean isChecked;
    private Bitmap image; // Drawable resource ID for the image
    private String tags;
    private String date;
    private String time;
    private boolean isSketch; // New field to indicate if it's a sketch or not

    public CheckedListItem(boolean isChecked, Bitmap image, String tags, String date, String time, boolean isSketch) {
        this.isChecked = isChecked;
        this.image = image;
        this.tags = tags;
        this.date = date;
        this.time = time;
        this.isSketch = isSketch;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getTags() {
        return tags;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public boolean isSketch() {
        return isSketch;
    }

    public void setSketch(boolean sketch) {
        isSketch = sketch;
    }
}


