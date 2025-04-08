package com.example.assingment04;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

public class PhotoAdapter extends ArrayAdapter<PhotoItem> {

    public PhotoAdapter(Context context, ArrayList<PhotoItem> photos) {
        super(context, 0, photos);
        Collections.reverse(photos);

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.photo_list, parent, false);
        }

        PhotoItem photo = getItem(position);

        ImageView photoImage = convertView.findViewById(R.id.photoImage);
        TextView photoTags = convertView.findViewById(R.id.photoTags);
        TextView photoDate = convertView.findViewById(R.id.photoDate);
        TextView photoTime = convertView.findViewById(R.id.photoTime);

        photoImage.setImageBitmap(photo.getImage());
        photoTags.setText("Tags: " + photo.getTags());
        photoDate.setText("Date: " + photo.getDate());
        photoTime.setText("Time: " + photo.getTime());

        return convertView;
    }
}

