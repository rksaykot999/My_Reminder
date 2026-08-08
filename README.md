# My Reminder - Android App

A comprehensive Android reminder and productivity application built with **Kotlin Compose** and **Material 3** design. This app combines task reminder functionality with integrated productivity tools to help you stay organized and focused.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Core Features](#core-features)
- [Tech Stack](#tech-stack)
- [Installation Guide](#installation-guide)
- [Project Structure](#project-structure)
- [How to Use](#how-to-use)
- [Permissions](#permissions)
- [Building & Running](#building--running)
- [Development](#development)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

**My Reminder** is a feature-rich Android application designed to help users:
- Set and manage reminders for important tasks and events
- Track time with integrated productivity tools
- Stay notified with push notifications
- Optimize workflow with productivity timing techniques

The app is built using modern Android development practices with Kotlin and Jetpack Compose, ensuring a smooth and responsive user experience across all Android devices (API 24+).

---

## ✨ Core Features

### 1. **Reminder Management**
- Create, edit, and delete reminders
- Set reminders for specific dates and times
- Recurring reminder support
- Visual organization with staggered grid layout
- Persistent storage of all reminders

### 2. **Notification System**
- Real-time push notifications for reminders
- Customizable notification channels
- Notification permissions handling (Android 13+)
- Notification scheduling with exact alarm precision

### 3. **Productivity Tools** (via Foreground Service)
- **Pomodoro Timer**: Default 25-minute focus sessions with configurable break times
- **Custom Timer**: Set any duration timer for specific tasks
- **Stopwatch**: Precise time tracking with lap functionality
- Real-time updates while app runs in background
- Persistent notifications for active productivity sessions

### 4. **User Interface**
- Modern Material 3 design system
- Smooth animations and transitions
- Responsive layouts with Compose
- Dark mode support
- Edge-to-edge display support

### 5. **Background Processing**
- Foreground service for background timer/stopwatch tracking
- Broadcast receiver for alarm handling
- Continuous notification updates during active sessions

---

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Kotlin | Latest |
| **UI Framework** | Jetpack Compose | Latest |
| **Material Design** | Material 3 | Latest |
| **Target Android** | Android 14 (API 37) | Compile SDK 37 |
| **Min Android** | Android 7 (API 24) | Min SDK 24 |
| **Build Tool** | Gradle | KTS |
| **Java Compatibility** | Java 11 | OpenJDK 11 |
| **Testing** | JUnit, Espresso | Latest |
| **Image Loading** | Coil | Latest |
| **JSON Parsing** | GSON | Latest |

---

## 📥 Installation Guide

### Prerequisites
- Android Studio (latest version recommended)
- Java Development Kit (JDK 11 or higher)
- Android SDK with API 37
- Gradle 8.0 or higher

### Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/rksaykot999/My_Reminder.git
   cd My_Reminder
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Click "File" → "Open"
   - Select the project directory
   - Wait for Gradle sync to complete

3. **Configure SDK**
   - Ensure you have API 37 installed
   - Update `local.properties` if needed with your SDK path

4. **Build the Project**
   ```bash
   ./gradlew build
   ```

5. **Run on Device/Emulator**
   - Connect an Android device or start an emulator
   - Click "Run" (green play button) in Android Studio
   - Or use: `./gradlew installDebug`

---

## 📁 Project Structure

```
My_Reminder/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/reminder/
│   │   │   │   ├── MainActivity.kt          # Main UI and entry point
│   │   │   │   ├── ProductivityService.kt   # Background service for timers
│   │   │   │   ├── ReminderReceiver.kt      # Broadcast receiver for alarms
│   │   │   │   └── ui/theme/
│   │   │   │       ├── Theme.kt             # Material 3 theme
│   │   │   │       ├── Color.kt             # Color palette
│   │   │   │       └── Type.kt              # Typography
│   │   │   ├── AndroidManifest.xml          # App configuration
│   │   │   └── res/                         # Resources (strings, colors, etc.)
│   │   ├── test/                            # Unit tests
│   │   └── androidTest/                     # UI tests
│   └── build.gradle.kts                     # App-level build config
├── build.gradle.kts                         # Project-level build config
├── settings.gradle.kts                      # Gradle settings
├── gradle.properties                        # Gradle properties
└── README.md                                # This file
```

### Key Files Explained

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Contains the main UI logic, reminder creation/editing, and Compose UI setup |
| `ProductivityService.kt` | Manages background timers, stopwatch, and pomodoro sessions |
| `ReminderReceiver.kt` | Handles alarm broadcasts and triggers notifications |
| `AndroidManifest.xml` | Declares app permissions, activities, services, and receivers |

---

## 📖 How to Use

### Creating a Reminder

1. **Open the App** - Launch My Reminder from your home screen
2. **Tap "+" Button** - Access the reminder creation interface
3. **Fill Details**:
   - Enter reminder title and description
   - Select date using the date picker
   - Select time using the time picker
4. **Save** - Tap the "Save" button to create the reminder
5. **Receive Notification** - When the reminder time arrives, you'll get a notification

### Using the Pomodoro Timer

1. **Navigate to Productivity** - Tap the Productivity tab
2. **Select Pomodoro** - Choose the Pomodoro option
3. **Start Session** - Tap "Start" to begin a 25-minute focus session
4. **Work** - Focus on your task; you'll receive a notification when the session ends
5. **Break** - The app will automatically suggest a break time

### Using the Timer

1. **Select Timer Tool** - Choose Timer from productivity options
2. **Set Duration** - Input the desired time (hours, minutes, seconds)
3. **Start** - Tap "Start" to begin counting down
4. **Notifications** - Receive an alert when the timer finishes

### Using the Stopwatch

1. **Select Stopwatch** - Choose Stopwatch from productivity options
2. **Start/Stop** - Tap to start and pause the timer
3. **Record Laps** - Tap "Lap" to record split times
4. **Reset** - Clear all data and start fresh

---

## 🔐 Permissions

The app requests the following permissions:

| Permission | Purpose |
|-----------|---------|
| `POST_NOTIFICATIONS` | Send reminder and timer notifications (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Set precise reminder alarms |
| `USE_EXACT_ALARM` | Use exact alarm timing for reminders |
| `FOREGROUND_SERVICE` | Run timers/stopwatch in the background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Identify productivity timing as special use |

**Note**: The app gracefully handles permission denials and will prompt you to grant permissions when needed.

---

## 🔨 Building & Running

### Build Variants

- **Debug Build** (Development)
  ```bash
  ./gradlew assembleDebug
  ```
  
- **Release Build** (Production)
  ```bash
  ./gradlew assembleRelease
  ```

### Run on Emulator

```bash
# Start emulator
emulator -avd <emulator_name> &

# Install and run
./gradlew installDebug
adb shell am start -n com.example.reminder/.MainActivity
```

### Run Tests

```bash
# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew testDebugUnitTest --tests="*ClassName*"
```

### Clean Build

```bash
./gradlew clean build
```

---

## 👨‍💻 Development

### Project Setup for Developers

1. **Fork the Repository**
   ```bash
   # On GitHub, click the "Fork" button
   ```

2. **Clone Your Fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/My_Reminder.git
   cd My_Reminder
   ```

3. **Create a Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Install Dependencies**
   - Gradle will automatically download dependencies on first build

5. **Make Changes**
   - Follow Kotlin style guide
   - Keep functions small and focused
   - Add comments for complex logic

6. **Test Your Changes**
   ```bash
   ./gradlew connectedAndroidTest
   ```

7. **Commit & Push**
   ```bash
   git add .
   git commit -m "Add: Description of your changes"
   git push origin feature/your-feature-name
   ```

8. **Create Pull Request**
   - Go to GitHub and create a PR to the main branch

### Code Style Guidelines

- **Naming**: Use camelCase for variables/functions, PascalCase for classes
- **Line Length**: Keep under 120 characters
- **Comments**: Use for complex logic only
- **Imports**: Remove unused imports
- **Formatting**: Use IDE auto-format (Ctrl+Alt+L / Cmd+Option+L)

### Architecture Patterns

- **Compose for UI**: Reactive, component-based UI
- **Service for Background Work**: ProductivityService handles background timers
- **BroadcastReceiver for Alarms**: ReminderReceiver handles alarm broadcasts
- **GSON for Data Serialization**: Storing and retrieving reminder data

---

## 🐛 Troubleshooting

### Common Issues & Solutions

#### Issue: "Gradle sync failed"
**Solution**:
- Update Android Studio to the latest version
- Check internet connection
- Delete `.gradle` folder and resync
- Ensure correct SDK paths in `local.properties`

#### Issue: "Notifications not appearing"
**Solution**:
- Grant notification permission in app settings
- Check Android version (Android 13+ requires runtime permission)
- Verify notification channel is created
- Disable battery saver mode temporarily

#### Issue: "Timer/Stopwatch not working in background"
**Solution**:
- Ensure foreground service is enabled
- Check foreground service notification is visible
- Verify `FOREGROUND_SERVICE` permission is granted
- Disable battery optimization for the app

#### Issue: "Reminders not triggering at set time"
**Solution**:
- Verify `SCHEDULE_EXACT_ALARM` permission is granted
- Ensure device clock is correct
- Check that reminder time is in the future
- Disable Doze mode for the app (on Android 6+)

#### Issue: "App crashes on startup"
**Solution**:
- Clear app cache: Settings → Apps → My Reminder → Clear Cache
- Force stop and restart the app
- Uninstall and reinstall the app
- Check logcat for error messages: `adb logcat | grep reminder`

#### Issue: "Gradle build timeout"
**Solution**:
- Increase Gradle heap size in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4096m
  ```
- Disable offline mode if enabled
- Try `./gradlew clean build --refresh-dependencies`

---

## 📞 Support & Contact

- **Issues**: Report bugs on [GitHub Issues](https://github.com/rksaykot999/My_Reminder/issues)
- **Discussions**: Use [GitHub Discussions](https://github.com/rksaykot999/My_Reminder/discussions)
- **Author**: rksaykot999

---

## 📜 License

This project is provided as-is. Please check the repository for specific license information.

---

## 🎉 Contributing

Contributions are welcome! Please follow the development guidelines above and submit a pull request with:
- Clear description of changes
- Tests for new features
- Updated documentation if needed

---

## 📝 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Current | Initial release with reminders and productivity tools |

---

## 🚀 Future Enhancements

- [ ] Reminder categories and tags
- [ ] Custom notification sounds
- [ ] Reminder templates
- [ ] Cloud synchronization
- [ ] Dark mode refinements
- [ ] Widget support
- [ ] Export/Import functionality
- [ ] Advanced analytics dashboard

---

**Last Updated**: August 2026  
**Built with ❤️ using Kotlin & Jetpack Compose**
