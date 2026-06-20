# ApplyTrack

ApplyTrack is an offline-first career companion and job application tracking platform, featuring a native Android application and a web companion. 

The platform is designed to prioritize local-first data integrity. Job seekers can log applications, track interview stages, manage documents (resumes, cover letters, and screenshots), and view dashboard analytics instantly with zero latency—even when completely offline. Once internet connectivity is restored, the application seamlessly synchronizes all metadata and files in the background to Cloud Firestore and Supabase Storage.

---

## Repository Structure

* **`AndroidApp/`**: Native Android client written in Kotlin, built with Jetpack Compose, Room, WorkManager, Firebase, and Supabase.
* **`WebApp/`**: Responsive web client companion.

---

## 📱 Android Application

### Key Features
* **Interactive Analytics Dashboard**: Real-time visualization tracking total applications, active interview stages, pending actions, offers, and rejection ratios.
* **Granular Application Management**: Complete control over application metadata (company name, role, URLs, current status, application dates, and rich textual preparation notes).
* **Local-First Database Architecture**: Powered by a Room SQLite database that acts as the single source of truth for the UI layer, achieving sub-millisecond response times.
* **Google Sign-In & Guest Mode**: Support for Google Identity authentication alongside an offline-first Guest mode, allowing frictionless usage that can be migrated to a cloud account later.
* **Bidirectional Cloud Sync**: Seamless reconciliation engine integrating Cloud Firestore with local Room DB, utilizing a last-write-wins (LWW) resolution policy.
* **Background Attachment Syncing**: Automated document management linking local caches to Supabase Storage buckets, allowing async uploads, downloads, and storage cleanup.

---

## 🏗️ Technical Architecture

ApplyTrack follows clean architecture principles coupled with the recommended Android MVVM design pattern:

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
* **UI Layer**: Jetpack Compose, Material 3, Lifecycle Compose Extensions.
* **Database (Local)**: Room Persistence Library (Room DAOs exposing reactive Kotlin Coroutines Flows).
* **Database (Remote)**: Cloud Firestore SDK (hierarchical user subcollections).
* **File Storage (Remote)**: Supabase Storage (OkHttp-driven REST API for binary streams).
* **Background Processing**: Android WorkManager (scheduled worker syncing metadata and media).
* **Dependency & Build**: Gradle Kotlin DSL, Version Catalogs (`libs.versions.toml`).

---

## 🔄 Bidirectional Data Sync Architecture

The synchronization engine operates under a double-insurance workflow:

```
Local Room Update ──► Trigger Sync ──► Upload Media to Supabase ──► Write Firestore Document
                                                                          │
                                                                          ▼
Remote Firestore Change ◄── Snapshot Listener ◄── Download Media ◄── Notify Device
```

1. **Write-Ordering Guarantee**: To prevent other devices from attempting to download incomplete files, metadata is only committed to Firestore *after* local attachments have been fully uploaded to Supabase Storage.
2. **Conflict Resolution**: Timestamps are tracked at the record level. Remote updates override local updates only if their modification timestamp is newer (Last-Write-Wins).
3. **Deletions (Tombstones)**: Deletions are tracked via a dedicated sync table, ensuring that when an application is deleted offline, the deletion is successfully propagated to the cloud upon the next connection.

---

## ⚙️ Development Configuration

### 1. Firebase Authentication & Firestore Setup
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with the package name `com.applytrack`.
3. Add the **SHA-1 certificate fingerprints** of your development, production, and Play Store signing keys to the project settings.
4. Download the `google-services.json` file and place it in the `AndroidApp/ApplyTrack/app/` directory.
5. Enable **Firestore Database** and **Firebase Authentication** (enable Google and Anonymous sign-in providers).

### 2. Supabase Storage Setup
1. In your Supabase project, create a new public storage bucket named `ApplyTrack`.
2. Configure a Storage Policy allowing `SELECT`, `INSERT`, `UPDATE`, and `DELETE` access to target paths for both `anon` and `authenticated` roles.
3. Add your Supabase credentials to your local environment file (`local.properties`):
   ```properties
   supabase.url=https://your-project.supabase.co
   supabase.anonkey=your-anon-public-key
   ```

---

## 📦 Building & Testing

### Compilation
Build the application using standard Gradle tasks:
```bash
./gradlew compileReleaseKotlin
```

### Running Unit Tests
Execute the local Kotlin unit test suite:
```bash
./gradlew testDebugUnitTest
```

### Building the Release Package
To compile the release bundle/APK:
```bash
./gradlew assembleRelease
```
The compiled package will be generated inside the `app/release/` directory. Sign it using Android Studio's **Generate Signed Bundle / APK** wizard with your production keystore.