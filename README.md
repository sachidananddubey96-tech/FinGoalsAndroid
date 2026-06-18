# Dubey's FinGoals Android APK

This project builds your FinGoals app as an installable Android APK with a native home-screen widget entry.

## Build in GitHub

1. Create a new GitHub repository.
2. Upload everything inside this `FinGoalsAndroid` folder to the repository root.
3. Open the repository's **Actions** tab.
4. Run **Build APK** manually, or push to `main`.
5. Download the `FinGoals-debug-apk` artifact from the finished workflow.

The APK file will be:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install on Android

Download the APK artifact to your phone and install it. Android may ask you to allow installs from your browser or file manager.

## Widget

After installing the APK, long-press the Android home screen, choose **Widgets**, find **FinGoals Widget**, and place it on the home screen. The included widget opens the FinGoals dashboard.

## Notes

- The current app UI and finance logic are bundled from `app/src/main/assets/index.html`.
- The app requires internet access for Google Apps Script sync and remote chart/font resources used by the original app.
- This is a real APK build, but the current implementation preserves the existing app by running it inside an Android WebView. A fully native Android rewrite would be a separate larger project.
