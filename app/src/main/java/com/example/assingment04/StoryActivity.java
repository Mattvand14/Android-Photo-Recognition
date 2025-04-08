package com.example.assingment04;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StoryActivity extends AppCompatActivity {

    private SQLiteDatabase photodb;
    private SQLiteDatabase sketchdb;
    private ArrayList<CheckedListItem> combinedItems;
    private CheckedListAdapter adapter;
    private TextView storyText;
    private CheckBox includeBox;
    private TextView selectedTagsTextView;

    private TextToSpeech textToSpeech;


    String url = "https://api.textcortex.com/v1/texts/social-media-posts";
    String API_KEY = "gAAAAABnKkTxHP3dLZjoA1b9jgJ6vXsbq288CYezhTUHsIPu9gAL3N4m3rURN5eDVPhiMzQlnjfMRtadAHFa8JY5z6TsLqr2iVQ6XrA9ZXQi6uUkfbRhIAtVQCAFbbUdfhIYDdldUBMn";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        photodb = openOrCreateDatabase("photo.db", MODE_PRIVATE, null);
        sketchdb = openOrCreateDatabase("drawing.db", MODE_PRIVATE, null);

        storyText = findViewById(R.id.storyText);
        includeBox = findViewById(R.id.includeBox);
        selectedTagsTextView = findViewById(R.id.selectedTags);  // Initialize the TextView

        setupListView(); // Populate the ListView initially

        // Add listener for checkbox to update the list
        includeBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateListViewBasedOnCheckbox(isChecked));

        if (savedInstanceState != null) {
            restoreCheckboxState(savedInstanceState);
        }

        textToSpeech = new TextToSpeech(this, status -> {
            if(status == TextToSpeech.SUCCESS){
                int result = textToSpeech.setLanguage(Locale.US);
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void setupListView() {
        ArrayList<CheckedListItem> photoItems = getPhotosFromDatabase();
        ArrayList<CheckedListItem> sketchItems = getSketchesFromDatabase();

        // Initially combine both lists
        combinedItems = new ArrayList<>();
        combinedItems.addAll(photoItems);
        combinedItems.addAll(sketchItems);

        // Initialize and bind the adapter, passing the selectedTags TextView
        adapter = new CheckedListAdapter(this, combinedItems, selectedTagsTextView);  // Pass the TextView reference
        ListView listView = findViewById(R.id.checkedList);
        listView.setAdapter(adapter);
    }

    private void updateListViewBasedOnCheckbox(boolean includeSketches) {
        ArrayList<CheckedListItem> filteredItems = new ArrayList<>();

        for (CheckedListItem item : combinedItems) {
            if (includeSketches || !item.isSketch()) {  // Include sketches if checked, otherwise filter out sketches
                filteredItems.add(item);
            }
        }

        // Update the adapter with the filtered list
        adapter = new CheckedListAdapter(this, filteredItems, selectedTagsTextView);  // Pass the TextView reference
        ListView listView = findViewById(R.id.checkedList);
        listView.setAdapter(adapter);
    }

    public void backButton(View view) {
        Intent intent = new Intent(StoryActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void findButton(View view) {
        EditText keywordsEditText = findViewById(R.id.keyWords);
        String inputKeywords = keywordsEditText.getText().toString().trim();

        CheckBox sketchesCheckBox = findViewById(R.id.includeBox); // Assume you have this checkbox in your layout
        boolean includeSketches = sketchesCheckBox.isChecked();

        ArrayList<CheckedListItem> filteredItems = new ArrayList<>();

        if (inputKeywords.isEmpty()) {
            // No keywords, show all items but apply the includeSketches filter
            for (CheckedListItem item : combinedItems) {
                if (includeSketches || !item.isSketch()) {
                    filteredItems.add(item);
                }
            }
        } else {
            // Filter based on keywords and apply the includeSketches filter
            String[] keywords = inputKeywords.split("\\s+");

            for (CheckedListItem item : combinedItems) {
                for (String keyword : keywords) {
                    if (item.getTags().toLowerCase().contains(keyword.toLowerCase())
                            && (includeSketches || !item.isSketch())) {
                        filteredItems.add(item);
                        break; // Only add once per keyword match
                    }
                }
            }
        }

        // Update the adapter with the filtered items
        adapter = new CheckedListAdapter(this, filteredItems, selectedTagsTextView); // Pass the TextView reference
        ListView listView = findViewById(R.id.checkedList);
        listView.setAdapter(adapter);
    }



    private ArrayList<CheckedListItem> getCheckedItems() {
        ArrayList<CheckedListItem> selectedItems = new ArrayList<>();
        for (CheckedListItem item : combinedItems) {
            if (item.isChecked()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    private ArrayList<CheckedListItem> getPhotosFromDatabase() {
        ArrayList<CheckedListItem> items = new ArrayList<>();
        Cursor cursor = photodb.rawQuery("SELECT image, date, time, tags FROM images", null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                byte[] imageBytes = cursor.getBlob(0);
                String date = cursor.getString(1);
                String time = cursor.getString(2);
                String tags = cursor.getString(3);

                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                items.add(new CheckedListItem(false, image, tags, date, time, false)); // false for photo (not sketch)
            } while (cursor.moveToNext());

            cursor.close();
        }
        return items;
    }

    private ArrayList<CheckedListItem> getSketchesFromDatabase() {
        ArrayList<CheckedListItem> items = new ArrayList<>();
        Cursor cursor = sketchdb.rawQuery("SELECT image, date, time, tags FROM images", null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                byte[] imageBytes = cursor.getBlob(0);
                String date = cursor.getString(1);
                String time = cursor.getString(2);
                String tags = cursor.getString(3);

                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                items.add(new CheckedListItem(false, image, tags, date, time, true)); // true for sketch
            } while (cursor.moveToNext());

            cursor.close();
        }
        return items;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save checkbox states
        boolean[] checkboxStates = new boolean[combinedItems.size()];
        for (int i = 0; i < combinedItems.size(); i++) {
            checkboxStates[i] = combinedItems.get(i).isChecked();
        }

        outState.putBooleanArray("checkboxStates", checkboxStates);
    }

    private void restoreCheckboxState(Bundle savedInstanceState) {
        boolean[] checkboxStates = savedInstanceState.getBooleanArray("checkboxStates");
        if (checkboxStates != null) {
            for (int i = 0; i < checkboxStates.length; i++) {
                combinedItems.get(i).setChecked(checkboxStates[i]);
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    public void generateStory(View view) {
        // Get the selected items
        ArrayList<CheckedListItem> selectedItems = getCheckedItems();

        if (selectedItems.size() == 0) {
            Toast.makeText(this, "Please select at least one item to generate a story.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build a string of tags from selected items
        StringBuilder tagsBuilder = new StringBuilder();
        for (CheckedListItem item : selectedItems) {
            tagsBuilder.append(item.getTags()).append(", ");
        }

        // Remove the last comma and space if the builder is not empty
        if (tagsBuilder.length() > 0) {
            tagsBuilder.setLength(tagsBuilder.length() - 2);
        }

        // Pass the tags to the HTTP request method
        String keywords = tagsBuilder.toString();
        String context = "Write a short story based on these tags:";

        // Set up the TTS to first speak the tags
        if (textToSpeech != null) {
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    // Do nothing
                }

                @Override
                public void onDone(String utteranceId) {
                    if (utteranceId.equals("TAGS_UTTERANCE")) {
                        // Once tags are spoken, proceed with the HTTP request
                        try {
                            makeHttpRequest(context, keywords);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e("TTS", "Error speaking utterance: " + utteranceId);
                }
            });

            textToSpeech.speak("Selected tags are: " + keywords, TextToSpeech.QUEUE_FLUSH, null, "TAGS_UTTERANCE");
        }
    }

    void makeHttpRequest(String context, String keywordsInput) throws JSONException {
        String[] keywordsArray = keywordsInput.split("\\s*,\\s*");
        JSONArray keywordsJsonArray = new JSONArray(keywordsArray);

        JSONObject data = new JSONObject();
        data.put("context", context);
        data.put("max_tokens", 200);
        data.put("mode", "twitter");
        data.put("model", "claude-3-5-sonnet");
        data.put("keywords", keywordsJsonArray);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data,
                response -> {
                    try {
                        JSONObject responseData = response.getJSONObject("data");

                        if (responseData.has("outputs")) {
                            JSONArray outputs = responseData.getJSONArray("outputs");

                            if (outputs.length() > 0) {
                                JSONObject firstOutput = outputs.getJSONObject(0);
                                String story = firstOutput.getString("text");
                                storyText.setText(story);

                                // Speak the story using TextToSpeech
                                if (!story.isEmpty()) {
                                    textToSpeech.speak("Here is your story: " + story, TextToSpeech.QUEUE_FLUSH, null, "STORY_UTTERANCE");
                                }
                            } else {
                                storyText.setText("No story generated.");
                            }
                        } else {
                            storyText.setText("Error: 'outputs' field missing in response.");
                        }
                    } catch (JSONException e) {
                        Log.e("error", e.toString());
                        storyText.setText("Error parsing story output.");
                    }
                },
                error -> {
                    Log.e("error", error.toString());
                    storyText.setText("Error: " + error.toString());
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + API_KEY);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }


    private void speakStory(String story){
        if(textToSpeech != null){
            textToSpeech.speak(story, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photodb != null) {
            photodb.close();
        }
        if (sketchdb != null) {
            sketchdb.close();
        }

        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
