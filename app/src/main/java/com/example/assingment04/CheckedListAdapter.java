package com.example.assingment04;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class CheckedListAdapter extends BaseAdapter {
    private Context context;
    private List<CheckedListItem> checkedListItems;
    private LayoutInflater inflater;
    private TextView selectedTagsTextView;  // Reference to the TextView to update

    public CheckedListAdapter(Context context, List<CheckedListItem> checkedListItems, TextView selectedTagsTextView) {
        this.context = context;
        this.checkedListItems = checkedListItems;
        this.inflater = LayoutInflater.from(context);
        this.selectedTagsTextView = selectedTagsTextView;
    }

    public void updateList(List<CheckedListItem> newList) {
        this.checkedListItems = newList;
        notifyDataSetChanged();  // This will update the ListView
    }

    @Override
    public int getCount() {
        return checkedListItems.size();
    }

    @Override
    public Object getItem(int position) {
        return checkedListItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.checked_photo_list, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkBox);
            holder.imageView = convertView.findViewById(R.id.photoImage);
            holder.tagsTextView = convertView.findViewById(R.id.photoTags);
            holder.dateTextView = convertView.findViewById(R.id.photoDate);
            holder.timeTextView = convertView.findViewById(R.id.photoTime);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CheckedListItem checkedListItem = checkedListItems.get(position);

        // Restore the CheckBox state based on the data model
        holder.checkBox.setOnCheckedChangeListener(null); // Prevent triggering listener while setting state
        holder.checkBox.setChecked(checkedListItem.isChecked());

        // Set other item details
        holder.imageView.setImageBitmap(checkedListItem.getImage());
        holder.tagsTextView.setText(checkedListItem.getTags());
        holder.dateTextView.setText(checkedListItem.getDate());
        holder.timeTextView.setText(checkedListItem.getTime());

        // Handle CheckBox state changes
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Enforce the limit of 3 selections
                int checkedCount = getCheckedItems().size();
                if (checkedCount >= 3) {
                    holder.checkBox.setChecked(false);
                    Toast.makeText(context, "You can only select up to 3 items.", Toast.LENGTH_SHORT).show();
                } else {
                    checkedListItem.setChecked(true); // Save state in the model
                }
            } else {
                checkedListItem.setChecked(false); // Save state in the model
            }
            updateSelectedTags();  // Update the selectedTags TextView when a checkbox is changed
        });

        return convertView;
    }

    private void updateSelectedTags() {
        // Get all the tags of the checked items
        List<String> selectedTags = new ArrayList<>();
        for (CheckedListItem item : checkedListItems) {
            if (item.isChecked()) {
                selectedTags.add(item.getTags());  // Add the tags of checked items
            }
        }

        // Update the selectedTags TextView with the selected tags
        selectedTagsTextView.setText("Selected Tags: " + String.join(", ", selectedTags));
    }

    private static class ViewHolder {
        CheckBox checkBox;
        ImageView imageView;
        TextView tagsTextView;
        TextView dateTextView;
        TextView timeTextView;
    }

    private ArrayList<CheckedListItem> getCheckedItems() {
        ArrayList<CheckedListItem> selectedItems = new ArrayList<>();
        for (CheckedListItem item : checkedListItems) {
            if (item.isChecked()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }
}

