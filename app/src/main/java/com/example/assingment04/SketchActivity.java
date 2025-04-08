package com.example.assingment04;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SketchActivity extends AppCompatActivity {


    MyDrawingArea myDrawingArea;
    //database
    private SQLiteDatabase db;

    private final String API_KEY = "AIzaSyBq5mrBxypHWyADztl7ObJJiCAZr69LcGk";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketch);

        myDrawingArea = findViewById(R.id.myDrawingArea);
        ListView listView = findViewById(R.id.listView);

        db = openOrCreateDatabase("drawing.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS images (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "image BLOB, " +
                "date TEXT, " +
                "time TEXT, " +
                "tags TEXT);");

        // Fetch sketches and display them in the ListView
        ArrayList<SketchItem> sketches = getSketchesFromDatabase();
        SketchAdapter adapter = new SketchAdapter(this, sketches);
        listView.setAdapter(adapter);
    }


    public void saveDrawing(View view) {
        Bitmap bitmap = myDrawingArea.getBitmap();

        // Initialize tags as an array
        EditText tags = findViewById(R.id.tagInput);
        String userInput = tags.getText().toString();

        // Comma delineate user input tags
        String[] userTags = userInput.split(",");

        // Create a background thread for image classification
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Get classification tags based on image analysis
            List<String> classifiedTags = getImageClassificationTags(bitmap);

            // Merge user input tags with classified tags (ensure no duplicates)
            Set<String> allTags = new HashSet<>(Arrays.asList(userTags));
            allTags.addAll(classifiedTags);  // Add classified tags

            Log.d("SaveDrawing", "User Tags: " + Arrays.toString(userTags));
            Log.d("SaveDrawing", "Classified Tags: " + classifiedTags.toString());


            // Convert final tags to an array
            String[] finalTags = allTags.toArray(new String[0]);

            // Insert data into the database with final tags (must run on the main thread)
            runOnUiThread(() -> insertData(bitmap, finalTags));
        });
    }

    public List<String> getImageClassificationTags(Bitmap image) {
        List<String> tags = new ArrayList<>();

        try {
            // Encode image
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 90, bout);
            Image myimage = new Image();
            myimage.encodeContent(bout.toByteArray());

            // Prepare the AnnotateImageRequest
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
            annotateImageRequest.setImage(myimage);
            Feature f = new Feature();
            f.setType("LABEL_DETECTION");
            f.setMaxResults(5);
            annotateImageRequest.setFeatures(Collections.singletonList(f));

            // Build the Vision API client
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
            builder.setVisionRequestInitializer(new VisionRequestInitializer(API_KEY));
            Vision vision = builder.build();

            // Call Vision.Images.Annotate
            BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
            batchAnnotateImagesRequest.setRequests(Collections.singletonList(annotateImageRequest));
            Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
            BatchAnnotateImagesResponse response = task.execute();

            Log.d("ImageClassification", "Full Response: " + response.toString());


            // Process response
            if (response != null) {
                List<EntityAnnotation> labelAnnotations = response.getResponses().get(0).getLabelAnnotations();
                if (labelAnnotations != null) {
                    // Add all labels with score > 0.85
                    for (EntityAnnotation annotation : labelAnnotations) {
                        if (annotation.getScore() > 0.85) {
                            tags.add(annotation.getDescription());
                        }
                    }
                    // If no tags were added, add the first label regardless of score
                    if (tags.isEmpty() && !labelAnnotations.isEmpty()) {
                        tags.add(labelAnnotations.get(0).getDescription());
                    }
                }
            }
        } catch (IOException e) {
            Log.e("ImageClassification", "Error during image classification", e);
        }

        return tags;
    }


    public void insertData(Bitmap image, String[] tags) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] imageBytes = outputStream.toByteArray();

        // Get date and time for tagging
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // Convert array of tags to a string for easy storage in the database
        String tagsString = TextUtils.join(",", tags);

        // Create content values for insertion
        ContentValues values = new ContentValues();
        values.put("image", imageBytes);
        values.put("date", currentDate);
        values.put("time", currentTime);
        values.put("tags", tagsString);

        Log.d("InsertData", "Final Tags: " + Arrays.toString(tags));


        // Insert the data into the database
        db.insert("images", null, values);
    }



    private ArrayList<SketchItem> getSketchesFromDatabase() {
        ArrayList<SketchItem> sketchList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT image, tags, date, time FROM images", null);

        if (cursor.moveToFirst()) {
            do {
                byte[] imageData = cursor.getBlob(0);
                String tags = cursor.getString(1);
                String date = cursor.getString(2);
                String time = cursor.getString(3);

                Log.d("GetSketches", "Retrieved Tags: " + tags);


                // Convert byte array to Bitmap
                Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                // Create a SketchItem and add it to the list
                sketchList.add(new SketchItem(image, tags, date, time));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return sketchList;
    }


    public void clearDrawing(View view) {
        myDrawingArea.clearDrawing();
    }


    public void returnMain(View view) {
        Intent intent = new Intent(SketchActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void findDrawing(View view) {
        EditText tagInput = findViewById(R.id.findInput); // EditText for entering the tag
        String tagToFind = tagInput.getText().toString().trim(); // Get the input tag

        ArrayList<SketchItem> resultList = new ArrayList<>();
        Cursor cursor;

        if (!tagToFind.isEmpty()) {
            // Query the database to get sketches with the matching tag, ordered by date and time descending
            cursor = db.rawQuery("SELECT image, tags, date, time FROM images WHERE tags LIKE ? ORDER BY date DESC, time DESC", new String[]{"%" + tagToFind + "%"});
        } else {
            cursor = db.rawQuery("SELECT image, tags, date, time FROM images ORDER BY date DESC, time DESC", null);
        }

        if (cursor.moveToFirst()) {
            do {
                byte[] imageData = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));
                String imageTags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));

                Log.d("FindDrawing", "Fetched Tags: " + imageTags);


                // Convert byte array to Bitmap
                Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                // Create a SketchItem and add it to the list
                resultList.add(new SketchItem(image, imageTags, date, time));
            } while (cursor.moveToNext());
        }

        cursor.close();



        // Populate the ListView with the SketchAdapter
        ListView listView = findViewById(R.id.listView);
        SketchAdapter adapter = new SketchAdapter(this, resultList);
        listView.setAdapter(adapter);

        if (resultList.isEmpty()) {
            // If no items found, show a message to the user
            Toast.makeText(this, "No drawings found.", Toast.LENGTH_SHORT).show();
        }
    }









}