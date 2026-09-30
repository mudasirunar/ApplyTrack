# Contributing to ApplyTrack 🚀

Thank you for your interest in contributing to **ApplyTrack**! We welcome contributions from developers, designers, and career enthusiasts of all skill levels.

ApplyTrack is an open-source, privacy-first career tracker designed to work offline-first across two clients:
- **Android App:** Built with Kotlin, Jetpack Compose, Material 3, and Room.
- **Web App:** Built with React 19, Vite, and modern CSS.
- **Backend / Sync:** Cloud Firestore for structured metadata and Supabase Storage for resume/document attachments.

---

## Table of Contents
1. [Monorepo Architecture](#monorepo-architecture)
2. [Prerequisites & Development Setup](#prerequisites--development-setup)
   - [Web App Setup](#web-app-setup)
   - [Android App Setup](#android-app-setup)
3. [Contribution Workflow](#contribution-workflow)
4. [Commit Message Guidelines](#commit-message-guidelines)
5. [Testing & Quality Verification](#testing--quality-verification)
6. [Pull Request Checklist](#pull-request-checklist)
7. [Code of Conduct](#code-of-conduct)

---

## Monorepo Architecture

The repository is organized into distinct project roots:

```
ApplyTrack/
├── AndroidApp/
│   └── ApplyTrack/         # Native Android application (Kotlin + Gradle)
├── WebApp/                 # React 19 + Vite web companion application
├── docs/                   # Documentation and screenshot assets
├── .github/                # Issue templates, PR templates, and CI workflows
├── CONTRIBUTING.md          # This guide
├── CODE_OF_CONDUCT.md     # Community standards
└── SECURITY.md             # Vulnerability disclosure policy
```

You can contribute to **either** client without needing to set up both!

---

## Prerequisites & Development Setup

### Web App Setup

1. **Prerequisites:**
   - Node.js (v18.x or later)
   - npm (v9.x or later)

2. **Navigate to the WebApp folder:**
   ```bash
   cd WebApp
   ```

3. **Install dependencies:**
   ```bash
   npm install
   ```

4. **Configure environment variables:**
   Copy the example environment template:
   ```bash
   cp .env.example .env.local
   ```
   *(Fill in your Firebase & Supabase test credentials, or use dummy values if running in offline mode).*

5. **Start the local Vite development server:**
   ```bash
   npm run dev
   ```
   Open `http://localhost:5173` in your browser.

---

### Android App Setup

1. **Prerequisites:**
   - Android Studio (Ladybug or newer recommended)
   - JDK 17 or JDK 21
   - Android SDK (API 34 / 36)

2. **Open the project:**
   In Android Studio, select **Open** and choose the directory: `ApplyTrack/AndroidApp/ApplyTrack`.

3. **Configure local configuration:**
   Copy the example configuration:
   ```bash
   cd AndroidApp/ApplyTrack
   cp local.properties.example local.properties
   ```
   Add your Android SDK path and test Supabase credentials if testing cloud sync.

4. **Configure Firebase Services:**
   Copy the example dummy configuration:
   ```bash
   cp app/google-services.json.example app/google-services.json
   ```
   *(Or download your own `google-services.json` from the Firebase Console if you wish to test real Google Authentication).*

5. **Build and Run:**
   - Run the app on an Android Emulator (API 26+) or a physical device with USB debugging enabled.
   - Alternatively, build via CLI:
     ```bash
     ./gradlew assembleDebug
     ```

---

## Contribution Workflow

1. **Find or Open an Issue:**
   - Check existing [GitHub Issues](https://github.com/mudasirunar/ApplyTrack/issues).
   - If proposing a major feature or redesign, open an issue first to discuss the approach before writing code.
2. **Fork the Repository:**
   - Click **Fork** on GitHub to create your own copy.
3. **Create a Feature Branch:**
   ```bash
   git checkout -b feat/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```
4. **Implement your changes:**
   - Keep changes focused on the specific issue or feature.
   - Respect existing architectural patterns (Room DB on Android, CSS design system on Web).
5. **Test your code locally:**
   - Verify that all builds and tests pass cleanly (see [Testing & Quality Verification](#testing--quality-verification)).
6. **Submit a Pull Request:**
   - Push your branch to your fork and submit a PR to `main`.
   - Fill out the PR template with all required details.

---

## Commit Message Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/):

Format:
```
<type>(<scope>): <short description>
```

Types:
- `feat`: A new feature or capability
- `fix`: A bug fix
- `docs`: Documentation updates (README, guides)
- `style`: Formatting, CSS tweaks, missing semicolons (no code logic changes)
- `refactor`: Code refactoring without changing functionality
- `test`: Adding or updating tests
- `chore`: Dependency updates, build tooling, CI configuration

Examples:
- `feat(web): add export applications to CSV`
- `fix(android): resolve status badge contrast in dark mode`
- `docs(readme): add screenshot preview of analytics dashboard`

---

## Testing & Quality Verification

Before submitting your PR, ensure the corresponding tests pass:

### For Web Changes:
```bash
cd WebApp
npm run lint     # Check for lint errors (oxlint)
npm run build    # Verify production compilation
```

### For Android Changes:
```bash
cd AndroidApp/ApplyTrack
./gradlew testDebugUnitTest    # Run unit test suite
./gradlew lintDebug            # Check Android lint warnings
```

---

## Pull Request Checklist

When opening a Pull Request, please confirm:
- [ ] Your branch is rebased on the latest `main`.
- [ ] Code passes all linting and build checks without errors.
- [ ] **No personal API keys, credentials, or private keystores** are committed.
- [ ] **For UI Changes:** You have attached screenshots or a short GIF demonstrating the change (before & after).
- [ ] You have linked the corresponding issue (e.g. `Fixes #12`).

---

## Code of Conduct

Participation in ApplyTrack is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). Please ensure respectful and inclusive communication at all times.

Have questions or need help? Feel free to open a Discussion or reach out to the maintainer at **unarmudasir@gmail.com**. Happy coding! 🚀
