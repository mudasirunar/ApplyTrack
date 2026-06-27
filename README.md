# ApplyTrack 🚀

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4.svg?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room DB](https://img.shields.io/badge/Room-Local--First-3DDC84.svg?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%26%20Auth-FFCA28.svg?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Supabase](https://img.shields.io/badge/Supabase-File%20Storage-3ECF8E.svg?style=flat-square&logo=supabase&logoColor=white)](https://supabase.com/)


**ApplyTrack** is an offline-first, local-priority career companion and job application tracking platform. Designed to provide job seekers with sub-millisecond interface responsiveness and absolute data integrity, the system consists of a native Android application and a responsive web companion.

Job seekers can instantly log job applications, manage resumes/cover letters, view rich analytical dashboards, and trace interview stages with zero network latency. When connectivity is restored, a robust synchronization engine reconciles metadata and binaries in the background using Google Cloud Firestore and Supabase Storage.

---

## 📂 Repository Structure

* **`AndroidApp/`**: Native Android client written in Kotlin, built with Jetpack Compose, Room, WorkManager, Firebase, and Supabase.
* **`WebApp/`**: Web application companion (HTML/CSS/JS client for tracking and viewing applications on desktop/web devices).

---

## 📱 Android Application: Feature Breakdown

The Android application is built with modern Android development standards, delivering a fully-featured, premium experience:

### 1. Interactive Analytics Dashboard
* **Dynamic Distribution Charts**: Interactive charts displaying applications breakdown by status (Applied, Saved, Interview, Offer, Rejected) using curated HSL color palettes.
* **Platform Effectiveness**: Track which portals (LinkedIn, Indeed, Website, Email, etc.) yield the highest response, interview, and offer rates.
* **Resume Efficacy Tracking**: Automatically compiles performance metrics for different resumes used during your search (tracking total uses, interview conversion, and offer rates).
* **Monthly Activity Timelines**: Visual bar graphs illustrating monthly application volumes for the selected calendar year.
* **Key Performance Indicators (KPIs)**: Instant reporting on success rates, response ratios, interview conversion metrics, and rejection trends.

### 2. Granular Application Management
* **Comprehensive Job Metadata**: Log company name, job title/role, submission platform, application dates, URLs, point-of-contact emails, and rich markdown notes.
* **Interactive Timeline**: Automatically logs status history changes with timestamps to build an application-specific audit trail.
* **Attachment Hub**: Store associated documents directly within each job entry:
  * Dedicated slots for **Resume**, **Cover Letter**, and **Additional Documents**.
  * Multi-file upload support for **Screenshots** (capturing job specs, confirmation emails, etc.).
  * **Built-in Document Viewers**: View image screenshots with interactive pinch-to-zoom gestures and read PDF attachments directly in-app using native PDF renderers.

### 3. Advanced Filtering & Search
* **Fuzzy Global Search**: Instant filtering of application lists by matching keywords in the company name, role description, preparation notes, email addresses, external URLs, or attachment filenames.
* **Status, Resume, and Platform Filters**: Easily narrow down lists by specific status, particular resume versions, or platforms.
* **Multi-Criteria Sorting**: Order applications by latest/oldest update status time, or latest/oldest creation timestamp.

### 4. Selection Mode & Batch Operations
* **Long-Press Activation**: Long-press any job application card to activate multi-select mode.
* **Batch Operations**: Select multiple applications to perform batch deletions, making bulk data cleanups frictionless.

### 5. Local Backup & Offline Restoration
* **ZIP Archive Exports**: Package your entire local database, metadata, and all downloaded attachments into a single portable `.zip` file.
* **Conflict-Aware Restoration**: Before importing, the app reads the backup and reports the exact number of data conflicts (matching UUIDs with mismatched content).
* **Overwrite Guard**: Choose whether to overwrite existing records or merge them gracefully during import.

### 6. Authentication & Account Migration
* **Google Sign-In & Anonymous Guest Mode**: Start using the app instantly as a guest without registering.
* **Seamless Migration**: Ready to back up to the cloud? Sign in with your Google account, and ApplyTrack automatically migrates all local Guest data and attachments to Firestore and Supabase.

---

## 🏗️ Technical Architecture

ApplyTrack follows Clean Architecture guidelines partitioned into **UI**, **Domain**, and **Data** layers, conforming to the Android MVVM pattern:

```
               ┌─────────────────────────────────────┐
               │              UI Layer               │
               │  (Compose Screens / ViewModels)     │
               └──────────────────┬──────────────────┘
                                  │ (Flow Observables)
                                  ▼
               ┌─────────────────────────────────────┐
               │            Domain Layer             │
               │       (Repository Interfaces)       │
               └──────────────────┬──────────────────┘
                                  │
                                  ▼
               ┌─────────────────────────────────────┐
               │             Data Layer              │
               │   (Room Database / Supabase SDK /   │
               │       Firestore Service SDK)        │
               └─────────────────────────────────────┘
```

### Core Technologies
* **UI & Presentation**: Jetpack Compose, Material Design 3 (Dynamic Color, custom theme transitions), Compose Navigation.
* **Asynchronous Execution**: Kotlin Coroutines, StateFlow/SharedFlow for reactive state management.
* **Local Storage**: Room Persistence Library with custom Type Converters for status histories and attachment objects.
* **Remote Database**: Cloud Firestore (hierarchical structures partitioned by authenticated user IDs).
* **Remote File Storage**: Supabase Storage Buckets utilizing authenticated REST APIs.
* **Background Tasks**: Android WorkManager (handling sync tasks and retry operations).

---

## 🔄 Bidirectional Data Sync Engine

The background synchronization engine is designed with a double-insurance workflow to guarantee consistency between local database stores and cloud providers:

```
[Local Database Update]
         │
         ▼
[Trigger Sync Event] ──► [Upload Attachments to Supabase] ──► [Write Metadata to Firestore]
                                                                        │
                                                                        ▼
[Update Local UI Flow] ◄── [Notify UI] ◄── [Download Attachments] ◄── [Firestore Live Snapshot]
```

### Core Sync Rules & Guardrails
1. **Write-Ordering Guarantee**: To prevent client devices from attempting to download attachments that do not exist yet, document metadata is only pushed to Firestore *after* Supabase Storage confirms successful binary uploads.
2. **Conflict Resolution (Last-Write-Wins)**: Every record tracks an `updatedAt` millisecond timestamp. Remote modifications overwrite local data only if the remote timestamp is strictly newer.
3. **Deletions & Tombstones**: To propagate deletions that occur offline, deleted record UUIDs are stored in a local tombstone table (`deleted_jobs`). Upon restoring network access, the Sync Worker reads this table, deletes the documents from Firestore, removes the files from Supabase, and clears the tombstones.
4. **Resilient Download Queue**: In-flight downloads are tracked in state. Failed attachment downloads are automatically retried up to three times with exponential backoff delays.

---

## ⚙️ Development Configuration

### 1. Firebase Suite Setup
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android Application under package `com.applytrack` (or your modified package name).
3. Generate and add SHA-1 certificate fingerprints (debug & release keys) to your Firebase project settings to enable **Google Sign-In**.
4. Download the generated `google-services.json` config file and place it in the `AndroidApp/ApplyTrack/app/` directory.
5. In the Firebase console:
   * Enable **Firebase Authentication** and turn on **Google** and **Anonymous** sign-in providers.
   * Enable **Cloud Firestore** and deploy the security rules.

### 2. Supabase Storage Setup
1. Set up a project in the [Supabase Dashboard](https://supabase.com/).
2. Navigate to **Storage** and create a new public bucket named `ApplyTrack`.
3. Configure Row Level Security (RLS) policies allowing `SELECT`, `INSERT`, `UPDATE`, and `DELETE` access to subfolders inside the bucket matching `auth.uid()`.
4. Create a `local.properties` file in `AndroidApp/ApplyTrack/` and add your API credentials:
   ```properties
   supabase.url=https://your-project-reference.supabase.co
   supabase.anonkey=your-anon-public-api-key
   ```

---

## 📦 Building & Testing

All building and verification workflows utilize the Gradle wrapper. Run commands inside the `AndroidApp/ApplyTrack` directory:

### Compile Source Code
Verify static compilation and syntax correctness:
```bash
./gradlew compileReleaseKotlin
```

### Run Unit Tests
Execute the local JUnit test suite verifying repositories, mapping utilities, and sync helpers:
```bash
./gradlew testDebugUnitTest
```

### Build Distribution Package
Compile and assemble the release APK or Android App Bundle (AAB):
```bash
./gradlew assembleRelease
```
The resulting installation binary will be output to:
`app/build/outputs/apk/release/app-release-unsigned.apk` (or signed output if keystore configuration is integrated).

---

## 🌐 Web Application (Companion)

The web companion is planned for future implementation and will be located in the `WebApp/` directory.

* **Planned Tech Stack**: Vanilla HTML5, CSS3, and JavaScript logic.
* **Planned Capabilities**: A lightweight web dashboard to load and visualize tracked job application metrics directly from browser clients.
* *(Note: The WebApp project is currently in the planning stage. Implementation details and execution instructions will be updated here once development begins.)*
