# ApplyTrack

A refined, offline-first personal job application tracker Android app. Built using modern Android development tools to help you track your career search records seamlessly.

---

## Key Features

- **Dynamic Analytics Dashboard**: Instantly track your job application statistics (Total Applications, Interviews, Pending/Saved, and Rejections) in real time.
- **Detailed Application Editor**: Track important details for each position:
  - Company Name & Role Title
  - Application Platform (LinkedIn, Email, Website, etc.)
  - Current Status (Applied, Interview, Offer, Rejected, Saved)
  - Date Applied (with native Material 3 calendar date picker)
  - Job Description & Posting Links
  - Personal Notes (recruiters, preparation points, next steps)
- **Instant Search & Filter Chips**: Find applications immediately by searching company names or job titles, and filter list views dynamically using status chips.
- **Local JSON Backup Management**: Export your entire job application records database as a JSON string to easily copy and save, or import it to restore your data on another device.
- **Offline-First Local Storage**: Uses a local Room SQLite database structure to guarantee lightning-fast performance and full availability offline.
- **Optional Firebase Cloud Synchronization**: Sync your data bi-directionally with Firebase Firestore. It uses a custom reconciliation layer featuring last-write-wins conflict resolution and remote deletion-override synchronization.

---

## Technical Stack

- **UI Framework**: Jetpack Compose (Modern Declarative UI)
- **Theme**: Material Design 3 (Dynamic Color compatibility and responsive components)
- **Local Database**: Android Room Persistence Library (SQLite wrapper with Kotlin Coroutines Flow integration)
- **JSON Serialization**: Moshi JSON Adapter (Reflect compiler)
- **Remote Database**: Firebase Firestore SDK (BOM platform dependency)
- **Architecture**: MVVM (Model-View-ViewModel Architecture Pattern)
- **Language**: Kotlin 2.2.10
- **Build System**: Gradle 9.3.1 (with Kotlin DSL configurations)

---

## How to Build & Run Locally

### Prerequisites
- [Android Studio Ladybug (or newer)](https://developer.android.com/studio)
- JDK 17 (configured in Android Studio project structure)

### Step-by-Step Setup

1. **Clone or Open the Project**:
   - Open Android Studio.
   - Choose **Open** and select the `AndoridApp/ApplyTrack` directory of this repository.

2. **Gradle Sync**:
   - Allow Android Studio to automatically sync the project configuration and download dependencies.

3. **Deploy the App**:
   - Start an Android Emulator or connect a physical Android device with USB debugging enabled.
   - Click the green **Run** icon in the toolbar to build and install the app.

---

## Enabling Firebase Cloud Sync (Optional)

ApplyTrack is designed to gracefully degrade to local Room storage if Firebase is not configured. To enable cloud synchronization:

1. **Create a Firebase Project**:
   - Go to the [Firebase Console](https://console.firebase.google.com/).
   - Register an Android App in the console with package name `com.applytrack`.

2. **Add Configuration File**:
   - Download the `google-services.json` file from your Firebase console.
   - Copy the file into the `app/` folder of this project (`AndoridApp/ApplyTrack/app/google-services.json`).
   - Add the Google Services Gradle Plugin to your build configuration if you wish to initialize automatically, or configure your credentials manually.

3. **Enable Firestore**:
   - Create a Cloud Firestore database in Test mode or define read/write rules.
   - The app will automatically detect Firestore compatibility and enable the Sync (Refresh) button on the dashboard toolbar.
