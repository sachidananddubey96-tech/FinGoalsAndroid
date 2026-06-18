# Dubey's FinGoals Android APK

This project builds your FinGoals app as an installable native Android APK with a live home-screen widget.

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

After installing the APK, long-press the Android home screen, choose **Widgets**, find **FinGoals Widget**, and place it on the home screen.

The widget refreshes from your Google Apps Script data and shows:

- Total investments
- Current month UPI spending
- Current unbilled credit card amount

## Notes

- The app is implemented with native Android views, not WebView.
- The app requires internet access for Google Apps Script sync.
- This native version covers the core dashboard, inflow/outflow entry, recent records, card unbilled totals, and live widget numbers.
