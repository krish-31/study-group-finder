# 📚 Study Group Finder

A modern Android application that helps students **discover, create, and manage study groups** with real-time chat, session scheduling, and activity tracking — all built with Jetpack Compose and Firebase.

---

## ✨ Features

### 🔐 Authentication
- Email & password registration and login via **Firebase Auth**
- Animated splash screen with automatic session restore
- Profile setup during registration (name, bio, subjects of interest)

### 🏠 Home Dashboard
- Personalized greeting with the user's name
- **Your Groups** — horizontal carousel of joined groups
- **Discover Groups** — vertical feed of public groups to explore
- Quick-access search bar, notification badge, and FAB to create groups

### 🔍 Search & Discovery
- Search groups by **name** or **subject** with real-time filtering
- Shimmer loading placeholders for a polished feel
- Filter between all groups and subject-specific results

### 👥 Study Groups
- **Create Groups** — set name, subject, description, max members, and privacy (public / private)
- **Group Detail** — tabbed view with Members, Sessions, and Reviews
- **Private Groups** — join-request system with approval/rejection by the group creator
- **Ratings & Reviews** — members can rate groups (1–5 ⭐) and leave written reviews

### 💬 Real-Time Chat
- In-group messaging powered by **Firestore real-time listeners**
- Styled message bubbles with sender names and timestamps
- Automatic scroll-to-bottom on new messages

### 📅 Session Scheduling
- Group admins can schedule study sessions with:
  - Title / topic
  - Date & time picker
  - Duration (in minutes)
  - **Session Link** — a clickable meeting URL (e.g. Google Meet, Zoom)
- Session lifecycle: **Upcoming → Active → Completed**
- All group members receive in-app notifications when a new session is created
- Students can **tap the session link** to join the meeting directly from the app

### 🔔 Notifications
- In-app notification system for:
  - New session scheduling
  - Join request updates
  - Group announcements
- Unread badge counter on the home screen
- Mark as read / dismiss functionality

### 📊 Activity Tracking & Achievements
- Visual activity tracking dashboard
- **Gamification badges** for milestones (e.g. "First Session", group creation)
- Achievement display with earned timestamps

### 🛡️ Admin Panel
- Dedicated admin dashboard for users with `isAdmin = true`
- Manage users, groups, and platform-wide settings
- Admin privileges enforced at both UI and Firestore rules level

### 👤 User Profile
- Editable profile with name, bio, and subjects
- Profile picture support (via URL)
- View joined groups and personal stats

---

## 🏗️ Architecture

The app follows a **clean MVVM architecture** with clear separation of concerns:

```
com.studygroup.finder/
├── core/
│   ├── di/                  # Hilt dependency injection modules
│   ├── navigation/          # Navigation graph & screen routes
│   └── utils/               # Error handling, network utils, UI helpers
├── data/
│   ├── model/               # Data classes (Firestore documents)
│   └── repository/          # Repository interfaces & Firebase implementations
└── ui/
    ├── admin/               # Admin panel screens
    ├── auth/                # Login, Register, Splash screens
    ├── chat/                # Real-time chat UI
    ├── components/          # Shared composables (EmptyState, Loading)
    ├── groups/              # Group CRUD, detail, join requests
    ├── home/                # Home dashboard & bottom navigation
    ├── notifications/       # Notification list & items
    ├── profile/             # Profile view & edit screens
    ├── search/              # Search with shimmer effects
    ├── sessions/            # Session scheduling, cards, detail
    └── tracking/            # Activity tracking & achievement badges
```

### Data Flow

```
UI (Composable) → ViewModel → Repository (Interface) → Firebase (Implementation)
                                                            ↕
                                                      Cloud Firestore
```

- **ViewModels** expose `StateFlow` for reactive UI updates
- **Repositories** abstract Firebase operations behind clean interfaces
- **Hilt** provides dependency injection across the entire app

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Navigation** | Navigation Compose |
| **DI** | Dagger Hilt |
| **Backend** | Firebase (Auth, Firestore, Cloud Messaging, Analytics) |
| **Image Loading** | Coil |
| **Async** | Kotlin Coroutines + Flow |
| **Architecture** | MVVM |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 35 |

---

## 📦 Firestore Collections

| Collection | Description |
|------------|-------------|
| `users` | Registered user profiles |
| `study_groups` | Study group metadata and member lists |
| `study_sessions` | Scheduled sessions linked to groups |
| `chat_messages` | Real-time group chat messages |
| `join_requests` | Private group join requests (pending/accepted/rejected) |
| `notifications` | In-app notifications per user |
| `reviews` | Group ratings and written reviews |
| `achievements` | Gamification badges earned by users |

> Security rules are defined in [`firestore.rules`](firestore.rules) with role-based access control (member, creator, admin).

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17**
- A **Firebase project** with Firestore, Authentication (Email/Password), and Cloud Messaging enabled

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/krish-31/study-group-finder.git
   cd study-group-finder
   ```

2. **Configure Firebase**
   - Go to the [Firebase Console](https://console.firebase.google.com/)
   - Create a new project (or use an existing one)
   - Add an Android app with package name: `com.studygroupfinder.app`
   - Download `google-services.json` and place it in the `app/` directory

3. **Enable Firebase services**
   - **Authentication** → Enable Email/Password sign-in
   - **Cloud Firestore** → Create database in production mode
   - Deploy the security rules from `firestore.rules`

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or simply open the project in Android Studio and hit **Run ▶️**

---

## 📂 Project Structure

```
study-group-finder/
├── app/
│   ├── build.gradle.kts          # App-level dependencies & config
│   ├── google-services.json      # Firebase config (not tracked in git)
│   └── src/main/
│       ├── java/com/studygroup/finder/
│       │   ├── core/             # DI, Navigation, Utilities
│       │   ├── data/             # Models & Repositories
│       │   └── ui/               # All Composable screens & ViewModels
│       └── res/                  # Resources (themes, strings, drawables)
├── firestore.rules               # Firestore security rules
├── build.gradle.kts              # Project-level Gradle config
├── gradle.properties
└── settings.gradle.kts
```

---

## 🔒 Security

- `google-services.json` is excluded from version control via `.gitignore`
- Firestore rules enforce authentication, membership, and admin checks
- Users cannot self-promote to admin (enforced at rules level)
- Chat messages validate `senderId` matches the authenticated user
- Private group access is restricted to members and the creator

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

---

<p align="center">
  Built with ❤️ using <b>Jetpack Compose</b> & <b>Firebase</b>
</p>
