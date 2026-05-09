# TaskFlow - Intelligent Task Automation

TaskFlow is a native Android application that automatically captures actionable tasks from your notifications and organizes them into a smart to-do list with reminders.

## Features

- **Notification Capture** - Automatically detects task-like messages from notifications using keyword matching
- **Smart Task Management** - Organize, prioritize, and complete tasks from a clean Material Design interface
- **Reminders** - Set exact alarms to remind you about upcoming tasks
- **Boot Persistence** - Reminders survive device restarts
- **Configurable Settings** - Choose which apps to monitor, customize notification keywords, and more

## Tech Stack

- **Language:** Java
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)
- **Database:** Room (SQLite abstraction)
- **UI:** Material Components, RecyclerView, ConstraintLayout, CardView
- **Architecture:** Activity-based with Room for persistence

## Project Structure

```
app/src/main/java/com/taskflow/automate/
    model/       - Data models (Task entity)
    database/    - Room database, DAO, and type converters
    service/     - NotificationListenerService implementation
    receiver/    - BroadcastReceivers (reminders, boot, task completion)
    ui/          - Activities and adapters
    util/        - Utility/helper classes
```

## Setup Instructions

1. **Clone the repository**
   ```
   git clone <repository-url>
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory and select it
   - Wait for Gradle sync to complete

3. **Build and Run**
   - Connect an Android device or start an emulator (API 26+)
   - Click "Run" or press Shift+F10

4. **Grant Permissions**
   - On first launch, the app will guide you to enable Notification Access in system settings
   - Grant notification permission when prompted (Android 13+)

## Permissions

| Permission | Purpose |
|-----------|---------|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read incoming notifications to detect tasks |
| `RECEIVE_BOOT_COMPLETED` | Reschedule reminders after device restart |
| `SCHEDULE_EXACT_ALARM` | Set precise reminder alarms |
| `POST_NOTIFICATIONS` | Show reminder notifications (Android 13+) |

## Building

The project uses Gradle with the Android Gradle Plugin 8.2.0. Ensure you have:
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with API level 34 installed

## License

This project is for demonstration and educational purposes.
