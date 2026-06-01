# EditFlow Pro - Firebase Backend Setup Guide

We have successfully integrated the **Firebase SDK** (including Firestore and Firebase Auth) into the codebase and pre-configured the build environment with necessary plugins and a placeholder configuration. 

Follow these simple steps to link your own live Firebase Project:

---

## 1. Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** and name it (e.g., `EditFlow Pro`).
3. Click **Continue** and complete the project creation.

---

## 2. Register Your Android App
1. On your Firebase project home page, click the **Android** icon to add an Android app.
2. Enter your **Android Package Name**:
   ```
   com.aistudio.editflowpro.tqzxlw
   ```
3. (Optional) Provide an App Nickname (e.g., `EditFlow Pro Android`).
4. Click **Register app**.

---

## 3. Place Your `google-services.json` File
1. Download the `google-services.json` configuration file provided by the Firebase Console.
2. In the AI Studio file explorer on the left, upload/overwrite the existing placeholder file:
   ```
   /app/google-services.json
   ```
   *(Simply drag and drop your downloaded file over the `/app` folder to replace the placeholder).*

---

## 4. Enable Firebase Authentication
1. On the left sidebar of the Firebase Console, go to **Build** > **Authentication**.
2. Click **Get Started**.
3. Under the **Sign-in method** tab, click **Google** (or any other required providers).
4. Turn on the switch to **Enable**, choose your project support email, and click **Save**.

---

## 5. Enable Firestore Database
1. Go to **Build** > **Firestore Database** on the left menu.
2. Click **Create database**.
3. Choose your database location and click **Next**.
4. Select **Start in test mode** (for instant development/read-write capability) or **Start in production mode** (with rules restricting access), then click **Create**.
5. Your Firestore database is ready! The app will automatically sync its SQLite `Room` database records to a security-first `"projects"` collection inside Cloud Firestore dynamically.

---

## What We've Pre-Configured for You:
1. **Google Services Gradle Plugin**: Bound the official Google GMS plugin into both the top-level `build.gradle.kts` and app-level `build.gradle.kts`.
2. **Standard BOM Configuration**: Added Firebase BoM, Firestore, and Auth dependencies to synchronize with the app model seamlessly.
3. **Placeholder Safe-Launch**: Added a fallback template of `google-services.json` to ensure compilation and gradle builds pass flawlessly even before you set up your personal project credentials.
4. **Offline Sync & Multi-User Support**: Handled automatic SQLite merging so that your pipelines operate offline-first and sync to Cloud Firestore when internet connectivity is restored.
