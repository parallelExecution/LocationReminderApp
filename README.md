## Location Reminder

A Todo list app with location reminders that remind the user to do something when he reaches a specific location. The app will require the user to create an account and login to set and access reminders.

### Built With

* [Koin](https://github.com/InsertKoinIO/koin) - A pragmatic lightweight dependency injection framework for Kotlin.
* [FirebaseUI Authentication](https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md) - FirebaseUI provides a drop-in auth solution that handles the UI flows for signing.
* [JobIntentService](https://developer.android.com/reference/androidx/core/app/JobIntentService) - Run background service from the background application, Compatible with >= Android O.
* [Room](https://developer.android.com/training/data-storage/room) for local database storage.

It leverages the following components from the Jetpack library:

* [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
* [LiveData](https://developer.android.com/topic/libraries/architecture/livedata)
* [Data Binding](https://developer.android.com/topic/libraries/data-binding/) with binding adapters
* [Navigation](https://developer.android.com/topic/libraries/architecture/navigation/) with the SafeArgs plugin for parameter passing between fragments

### Demo
![screen-shot-1](https://user-images.githubusercontent.com/12608658/164869016-18732a9a-1d1b-478c-8cbd-98fe43d5907b.png)
![screen-shot-2](https://user-images.githubusercontent.com/12608658/164869022-ab4befeb-be0f-499b-988d-094ccf2a38f2.png)
![screen-shot-3](https://user-images.githubusercontent.com/12608658/164869024-53dd1523-a7ca-4266-a072-8352fa2cca6a.png)
![screen-shot-4](https://user-images.githubusercontent.com/12608658/164869025-b79aa6f6-40b1-430f-bdf3-12fce280291d.png)

### Setting up the Repository

To get started with this project, simply pull the repository and import the project into Android Studio. From there, deploy the project to an emulator or device.

There are some dependencies that are needed to run the application, here are step-by-step instructions to get the application running:

```
1. Enable Firebase Authentication:
        a. Go to the authentication tab at the Firebase console and enable Email/Password and Google Sign-in methods.
        b. download `google-services.json` and add it to the app.
2. Enable Google Maps:
    a. Go to APIs & Services at the Google console.
    b. Select your project and go to APIs & Credentials.
    c. Create a new api key and restrict it for android apps.
    d. Add your package name and SHA-1 signing-certificate fingerprint.
    c. Enable Maps SDK for Android from API restrictions and Save.
    d. Copy the api key to the `google_maps_api.xml`
3. Run the app on your mobile phone or emulator with Google Play Services in it.
```

### Testing

Right click on the `test` or `androidTest` packages and select Run Tests

### Project Specification

Following is the project rubric that lists all the requirements that are met by application.

![rubric](https://user-images.githubusercontent.com/12608658/164579081-7bb47853-c434-485b-84a5-267efae70636.jpeg)