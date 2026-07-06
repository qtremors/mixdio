# Privacy Policy

Mixdio is built from the ground up to respect your privacy.

## No Network Operations

Mixdio does not request the `android.permission.INTERNET` permission in its manifest. By design, the application has no way to transmit user data, communicate with external servers, fetch promotional ads, or compile analytics/telemetry metrics. All files, favorites lists, custom playlists, settings configurations, and play histories remain strictly local to your device.

## Scoped Permissions

Mixdio requests only the permissions necessary to function as a offline music player:
- **READ_EXTERNAL_STORAGE / READ_MEDIA_AUDIO**: Required to scan device storage volumes for audio files to populate the music library.
- **POST_NOTIFICATIONS**: Required on Android 13+ to display background system notification control overlays for current track details and playback states.

## Zero Diagnostic Tracking

Mixdio does not contain crash reporting SDKs (such as Firebase Crashlytics or Sentry) or tracking analytics engines.
