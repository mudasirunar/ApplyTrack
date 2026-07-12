# ApplyTrack 🚀

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4.svg?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-Local%20Database-3DDC84.svg?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/room)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%26%20Firestore-FFCA28.svg?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Supabase](https://img.shields.io/badge/Supabase-Storage-3ECF8E.svg?style=flat-square&logo=supabase&logoColor=white)](https://supabase.com/)
[![React](https://img.shields.io/badge/React-19-61DAFB.svg?style=flat-square&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF.svg?style=flat-square&logo=vite&logoColor=white)](https://vite.dev/)

ApplyTrack is an offline-first job application tracker built for people who want a fast, reliable way to manage opportunities from both mobile and web. The project currently contains two working clients:

- an Android app built with Jetpack Compose and Room for local-first storage
- a React + Vite web companion for desktop and browser access

Both clients support local data capture, rich analytics, attachment handling, and cloud sync through Firebase Firestore and Supabase Storage.

---

## 📂 Repository Structure

- AndroidApp/ApplyTrack: native Android application written in Kotlin
- WebApp: React web companion with Vite and Firebase-backed sync

---

## 📱 Android App

The Android client is the primary experience in this repository. It is built around a local-first architecture with a Compose UI and Room persistence, and it supports:

- job tracking with company, role, platform, status, notes, URL, and email
- status history and timeline-style tracking
- resume, cover letter, additional document, and screenshot attachments
- image and PDF viewing inside the app
- dashboard analytics for status distribution, platform performance, resume effectiveness, and monthly activity
- filtering, search, sorting, and batch selection for large result sets
- local backup/export and import workflows
- Google sign-in and anonymous guest mode
- background sync to Firestore and Supabase when a network connection is available

### Android stack

- Kotlin
- Jetpack Compose + Material 3
- Room Database
- Coroutines + StateFlow
- WorkManager for background sync
- Firebase Authentication and Firestore
- Supabase Storage

---

## 🌐 Web App

The web companion is implemented and runs as a modern React/Vite app. It provides a browser-based interface for:

- authentication and protected routes
- dashboard views with analytics and summaries
- applications listing, search, filtering, and sorting
- add/edit/detail flows for jobs
- attachment handling and metadata editing
- settings, theme support, and data management actions
- synchronization with the same Firebase/Firestore + Supabase storage model used by Android

### Web stack

- React 19
- Vite 8
- Firebase Auth + Firestore + Analytics
- Supabase Storage REST integration
- Local browser storage for cache and UI state

---

## 🏗️ Architecture Overview

The project uses a split approach:

- Android app: local Room database + Compose UI + repository/sync layer
- Web app: React UI + local browser storage + Firestore/Supabase sync utilities
- Shared sync concept: metadata is stored in Firestore while binary attachments are uploaded to Supabase Storage

This gives the app a practical offline-first workflow: users can create and edit data locally first, then sync it to the cloud when connectivity is available.

---

## ⚙️ Setup

### Android setup

1. Create a Firebase project in the Firebase console.
2. Enable Firebase Authentication with Google and Anonymous sign-in.
3. Enable Cloud Firestore.
4. Download google-services.json and place it in AndroidApp/ApplyTrack/app/.
5. Create AndroidApp/ApplyTrack/local.properties with your Supabase values:

```properties
supabase.url=https://your-project-reference.supabase.co
supabase.anonkey=your-anon-public-api-key
```

6. Open the Android project and run the app from Android Studio or build it from the command line.

### Web setup

1. Create a .env file in WebApp with your Firebase and Supabase settings:

```env
VITE_FIREBASE_API_KEY=your_api_key
VITE_FIREBASE_AUTH_DOMAIN=your_auth_domain
VITE_FIREBASE_PROJECT_ID=your_project_id
VITE_FIREBASE_STORAGE_BUCKET=your_storage_bucket
VITE_FIREBASE_MESSAGING_SENDER_ID=your_messaging_sender_id
VITE_FIREBASE_APP_ID=your_app_id
VITE_FIREBASE_MEASUREMENT_ID=your_measurement_id
VITE_SUPABASE_URL=https://your-project-reference.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-public-api-key
```

2. Install dependencies:

```bash
cd WebApp
npm install
```

3. Start the development server:

```bash
npm run dev
```

---

## 🧪 Build and Test Status

The current repository has been verified locally with the following commands:

### Android

```bash
cd AndroidApp/ApplyTrack
./gradlew testDebugUnitTest
```

Result: unit tests completed successfully.

### Web

```bash
cd WebApp
npm run build
```

Result: production build completed successfully.

---

## ▶️ Run Commands

### Android

```bash
cd AndroidApp/ApplyTrack
./gradlew assembleRelease
```

### Web

```bash
cd WebApp
npm run dev
```
