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

public class SketchAdapter extends ArrayAdapter<SketchItem> {

    public SketchAdapter(Context context, ArrayList<SketchItem> sketches) {
        super(context, 0, sketches);

        // Reverse the list so the most recent items appear first
        Collections.reverse(sketches);

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.sketch_list, parent, false);
        }

        SketchItem sketch = getItem(position);

        ImageView sketchImage = convertView.findViewById(R.id.sketchImage);
        TextView sketchTags = convertView.findViewById(R.id.sketchTags);
        TextView sketchDate = convertView.findViewById(R.id.sketchDate);
        TextView sketchTime = convertView.findViewById(R.id.sketchTime);

        sketchImage.setImageBitmap(sketch.getImage());
        sketchTags.setText("Tags: " + sketch.getTags());
        sketchDate.setText("Date: " + sketch.getDate());
        sketchTime.setText("Time: " + sketch.getTime());

        return convertView;
    }
}
