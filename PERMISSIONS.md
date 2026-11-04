# Permissions

| Permission | Why |
|---|---|
| SYSTEM_ALERT_WINDOW | Draw the Dynamic Island overlay. User grants it in system settings; the app only asks when the module is switched on. |
| FOREGROUND_SERVICE + FOREGROUND_SERVICE_SPECIAL_USE | Keep the overlay alive. The service runs only while the module is on. |
| POST_NOTIFICATIONS (Android 13+) | Show the required foreground-service notification. |

Without the overlay permission the module stays off; the rest of the app works.

Other notes: The launcher uses the standard HOME role (chosen by the user in system settings) and a `<queries>` entry to list launchable apps.
