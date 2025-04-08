package com.example.assingment04;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = MediaPlayer.create(this, R.raw.background);

        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    // This method will be called when the button is clicked
    public void openSketchApp(View view) {
        stopMusic();
        Intent intent = new Intent(MainActivity.this, SketchActivity.class);
        startActivity(intent);
    }

    public void openPhotoApp(View view) {
        stopMusic();
        Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
        startActivity(intent);
    }

    public void returnMain(View view) {
        stopMusic();
        Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
        startActivity(intent);
    }

    public void openStoryTeller(View view){
        stopMusic();
        Intent intent = new Intent(MainActivity.this, StoryActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop and release the MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // Helper method to stop the music
    private void stopMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null; // Release resources and avoid memory leaks
        }
    }
}