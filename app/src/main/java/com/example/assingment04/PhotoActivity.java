package com.example.assingment04;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 212;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 213;

    private SQLiteDatabase photodb;
    private ImageView resultImage;
    private ListView lv;
    private final String API_KEY = "AIzaSyBq5mrBxypHWyADztl7ObJJiCAZr69LcGk";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        resultImage = findViewById(R.id.resultImage);
        lv = findViewById(R.id.listView);

        photodb = openOrCreateDatabase("photo.db", MODE_PRIVATE, null);
        photodb.execSQL("CREATE TABLE IF NOT EXISTS images (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "image BLOB, " +
                "date TEXT, " +
                "time TEXT, " +
                "tags TEXT);");

        // Fetch photos and display in ListView
        ArrayList<PhotoItem> photos = getPhotosFromDatabase();
        PhotoAdapter adapter = new PhotoAdapter(this, photos);
        lv.setAdapter(adapter);
    }

    // Open camera after checking permissions
    public void openCamera(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            Intent x = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(x, REQUEST_CODE_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK && data != null) {
            Bitmap newBitmap = (Bitmap) data.getExtras().get("data");
            if (newBitmap != null) {
                resultImage.setImageBitmap(newBitmap);
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void saveDrawing(View view) {
        // Ensure resultImage has an image
        Bitmap bitmap;
        if (resultImage.getDrawable() != null && resultImage.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) resultImage.getDrawable()).getBitmap();
        } else {
            Log.e("SaveDrawing", "No bitmap found in ImageView");
            return;
        }

        // Initialize tags as an array
        EditText tags = findViewById(R.id.tagInput);
        String userInput = tags.getText().toString();

        // Comma-delineate user input tags
        String[] userTags = userInput.split(",");

        // Create a background thread for image classification
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Get classification tags based on image analysis
            List<String> classifiedTags = getImageClassificationTags(bitmap);

            // Merge user input tags with classified tags (ensure no duplicates)
            Set<String> allTags = new HashSet<>(Arrays.asList(userTags));
            allTags.addAll(classifiedTags);

            Log.d("SaveDrawing", "User Tags: " + Arrays.toString(userTags));
            Log.d("SaveDrawing", "Classified Tags: " + classifiedTags.toString());

            // Convert final tags to an array
            String[] finalTags = allTags.toArray(new String[0]);

            // Insert data into the database with final tags
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
        String tagsString = TextUtils.join(", ", tags);

        // Create content values for insertion
        ContentValues values = new ContentValues();
        values.put("image", imageBytes);
        values.put("date", currentDate);
        values.put("time", currentTime);
        values.put("tags", tagsString);

        Log.d("InsertData", "Final Tags: " + Arrays.toString(tags));


        // Insert the data into the database
        photodb.insert("images", null, values);
    }

    private ArrayList<PhotoItem> getPhotosFromDatabase() {
        ArrayList<PhotoItem> photoList = new ArrayList<>();
        Cursor cursor = photodb.rawQuery("SELECT image, tags, date, time FROM images", null);

        if (cursor.moveToFirst()) {
            do {
                byte[] imageData = cursor.getBlob(0);
                String tags = cursor.getString(1);
                String date = cursor.getString(2);
                String time = cursor.getString(3);

                // Convert byte array to Bitmap
                Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                // Create a PhotoItem and add it to the list
                photoList.add(new PhotoItem(image, tags, date, time));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return photoList;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(null);
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void returnMain(View view) {
        Intent intent = new Intent(PhotoActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void findPhoto(View view) {
        EditText tagInput = findViewById(R.id.findInput); // EditText for entering the tag
        String tagToFind = tagInput.getText().toString().trim(); // Get the input tag

        ArrayList<PhotoItem> resultList = new ArrayList<>();
        Cursor cursor;

        if (!tagToFind.isEmpty()) {
            // Query the database to get photos with the matching tag
            cursor = photodb.rawQuery("SELECT image, tags, date, time FROM images WHERE tags LIKE ? ORDER BY date DESC, time DESC", new String[]{"%" + tagToFind + "%"});
        } else {
            cursor = photodb.rawQuery("SELECT image, tags, date, time FROM images ORDER BY date DESC, time DESC", null);
        }


        if (cursor.moveToFirst()) {
            do {
                byte[] imageData = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));
                String imageTags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));

                // Convert byte array to Bitmap
                Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                // Create a PhotoItem and add it to the list
                resultList.add(new PhotoItem(image, imageTags, date, time));
            } while (cursor.moveToNext());
        }

        cursor.close();

        // Reverse the order of the resultList so the most recent sketch appears first in the adapter
        Collections.reverse(resultList);

        // Populate the ListView with the PhotoAdapter
        ListView listView = findViewById(R.id.listView);
        PhotoAdapter adapter = new PhotoAdapter(this, resultList);
        listView.setAdapter(adapter);

        if (resultList.isEmpty()) {
            // If no items found, show a message to the user
            Toast.makeText(this, "No photos found.", Toast.LENGTH_SHORT).show();
        }
    }

}
