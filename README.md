# Sketch and Photo Tagger

## Overview
This Android mobile app uses the google vision api paired with the TextCortex API to capture images with the users camera, or allows the user to draw images,
and classify them. It then places the images into an SQLite database along with the tags given by the google vision api, and
the user can then go into the storyteller section of the app and select classified images for the TextCortex api to write a short story about. Then it uses the built
in android text to speech to speak your story


## Features
- Sketch Classifier and Tagging
- Photo Classifier and Tagging
- Storytelling
- Text to Speech

## Usage
Simply start the app on Android, then click on either photo or sketch tagger and take a photo or draw something. Then click save, and the
google vision api will automatically classify it for you and save it to the database with the three most relevant tags. You can then 
go to story teller and select images for the AI to tell you.

## Technologies Used
- Java/Kotlin
- Android SDK
- SQLite/Room
- Google Vision API
- TextCortex API
- Built In Text to Speech 


