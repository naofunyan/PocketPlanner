# Privacy Policy

**PocketPlanner**
**Last Updated: August 27, 2026**

---

## Introduction

PocketPlanner ("we," "our," or "us") is a travel planning and safety companion application for Android. This Privacy Policy explains how we collect, use, store, share, and protect your personal information when you use the PocketPlanner mobile application (the "App").

By downloading, installing, or using PocketPlanner, you agree to the practices described in this Privacy Policy. If you do not agree, please do not use the App.

---

## 1. Information We Collect

### 1.1 Account & Identity Information

- **Email address and username** — collected when you register for an account.
- **Google profile information** — including your display name, email address, and profile photo URL, collected when you sign in with Google.
- **Profile avatar** — photos you upload as your profile picture.
- **User identifier (UID)** — a unique identifier assigned to your account by Firebase Authentication.

You may also use the App as a guest without providing any personal credentials.

### 1.2 Location Data

We collect location data only when you grant the corresponding permissions:

- **Precise and approximate location** — used to display local weather, provide location-aware AI chat responses, and search for nearby places.
- **Background location** — used exclusively for proximity geofencing, which sends you a notification when you are near a planned itinerary destination (within 200 meters).
- **Emergency GPS coordinates** — during an SOS emergency event, high-accuracy GPS coordinates are obtained to generate a Google Maps link that is sent to your designated emergency contacts.

### 1.3 Camera & Media

- **Ticket and receipt photos** — images you capture or select from your gallery for ticket scanning, receipt scanning, and document organization.
- **Live camera frames** — used in real-time for the Gemini Live multimodal assistant and for live camera text translation (OCR).
- **Profile avatar images** — photos you choose to upload as your profile picture.
- **Emergency snapshots** — if you enable the emergency photo capture feature, a still photo may be automatically captured during a fall-detection SOS event and sent to your emergency contacts.

### 1.4 Audio & Voice

- **Voice chat audio** — microphone audio is streamed in real-time to Google Vertex AI (Gemini Live) for the voice assistant and interpreter features.
- **Voice-to-text input** — the App uses your device's built-in speech recognition for voice-based text input in the chat.
- **Emergency audio recording** — if you enable the emergency audio capture feature, a 5-second ambient audio clip may be recorded during a fall-detection SOS event and sent to your emergency contacts.

### 1.5 Sensor & Health-Related Data

- **Accelerometer data** — 3-axis accelerometer readings are collected at 50 Hz by the Fall Detection Service (foreground service) to detect potential falls using an on-device machine learning model. This data is processed entirely on your device and is never transmitted externally.
- **Medical profile (optional)** — you may voluntarily enter personal health information including: name, blood type, allergies, medical conditions, medications, weight, height, date of birth, address, organ donor status, and medical notes. This information is stored locally on your device and is only shared via SMS with your emergency contacts if you explicitly enable the "Share during emergency" setting.

### 1.6 Financial & Expense Data

- **Expense records** — category, amount, currency, converted amount, description, and date of each expense you log.
- **Trip budgets** — budget amounts and currencies associated with your trips.
- **Receipt data** — information extracted from scanned receipts (merchant name, amount, currency, category) using AI-powered parsing.

### 1.7 Travel & Itinerary Data

- **Trip details** — trip name, destination, start and end dates, status, cover photo, and AI-generated travel diary.
- **Itinerary places** — place names, coordinates (latitude/longitude), categories, estimated costs, notes, visit times, user ratings, and photos.
- **Tickets and passes** — title, type, date/time, QR/barcode content, OCR-extracted text, confirmation codes, and attached images.
- **Saved/bookmarked places** — destination names you save for future reference.

### 1.8 Emergency Contact Information

- **Contact names and phone numbers** — the emergency contacts you configure in the App's safety settings. These are stored locally on your device and are used solely for sending SOS messages during emergencies.

### 1.9 App Preferences

- Theme mode, notification preferences, location access toggle, fall detection sensitivity settings, and language preferences. All stored locally on your device.

---

## 2. Permissions We Request & Why

PocketPlanner requests the following Android permissions. Each permission is optional — you can grant or deny them individually through your device settings. Denying a permission will only disable the specific feature that requires it; the rest of the App will continue to function normally.

| Permission | Why We Need It |
|---|---|
| **`INTERNET`** | Required to sync your data with the cloud, communicate with the AI travel assistant (Gemini API), fetch maps, place information, destination photos, and currency exchange rates. |
| **`ACCESS_NETWORK_STATE`** | Allows the App to check whether your device is connected to the internet before attempting cloud sync or API calls, and to gracefully fall back to offline mode when no connection is available. |
| **`ACCESS_FINE_LOCATION`** | Used to show your precise location on the map, fetch local weather for your current city, provide location-aware AI chat responses, search for nearby places, and obtain exact GPS coordinates during an SOS emergency to share with your emergency contacts. |
| **`ACCESS_COARSE_LOCATION`** | Used as a fallback for approximate location when precise GPS is unavailable, for weather lookups and general place search. |
| **`ACCESS_BACKGROUND_LOCATION`** | Used **only** for proximity geofencing — when you enable the trip tracker, the App monitors your location in the background to notify you when you are near a planned itinerary destination (within 200 meters). This permission is never used for advertising or analytics. |
| **`FOREGROUND_SERVICE`** | Required by Android to run the Fall Detection Service and location-based geofencing as visible foreground services with persistent notifications, ensuring they are not killed by the system. |
| **`FOREGROUND_SERVICE_LOCATION`** | Required by Android 14+ to declare that the foreground service accesses location data for geofencing proximity alerts. |
| **`FOREGROUND_SERVICE_HEALTH`** | Required by Android 14+ to declare that the Fall Detection foreground service monitors health-related sensor data (accelerometer) for safety purposes. |
| **`CAMERA`** | Used to scan tickets, boarding passes, and receipts; to scan QR codes and barcodes; to perform live camera text translation (OCR); to stream live video to the Gemini Live multimodal assistant; to capture your profile avatar; and optionally to take an emergency snapshot during a fall-detection SOS event. |
| **`RECORD_AUDIO`** | Used for the Gemini Live voice assistant and real-time interpreter features (streaming voice to AI), for voice-to-text chat input, and optionally to record a 5-second ambient audio clip during an emergency SOS event to send to your emergency contacts. |
| **`POST_NOTIFICATIONS`** | Used to display proximity alerts when you are near an itinerary destination, fall detection service status notifications, and emergency alarm notifications. |
| **`ACTIVITY_RECOGNITION`** | Used by the Fall Detection Service to access motion-related sensor data and distinguish between normal movement and potential falls. |
| **`HIGH_SAMPLING_RATE_SENSORS`** | Used by the Fall Detection Service to read accelerometer data at 50 Hz (high sampling rate) for accurate real-time fall detection using the on-device machine learning model. |
| **`SEND_SMS`** | Used **only** during an emergency SOS event to automatically send text messages containing your GPS location (and optionally media links and medical information) to your pre-configured emergency contacts. Never used for marketing or any other purpose. |
| **`CALL_PHONE`** | Used **only** during an emergency SOS event to automatically place a phone call to your designated emergency number (e.g., 113 or a personal contact). Never used for any other purpose. |
| **`VIBRATE`** | Used to provide haptic feedback during emergency alerts and fall detection alarms to get your attention. |
| **`USE_FULL_SCREEN_INTENT`** | Used to display the emergency alert screen on top of the lock screen during a fall detection event, ensuring you see the SOS countdown even if your phone is locked. |
| **`RECEIVE_BOOT_COMPLETED`** | Used to automatically restart the Fall Detection Service after your device reboots, if you have enabled fall detection in the App's safety settings. |
| **`WAKE_LOCK`** | Used to keep the device processor awake during emergency SOS processing (GPS acquisition, audio recording, photo capture, and SMS dispatch) to ensure the emergency sequence completes reliably. |

> **Note:** Permissions related to emergency and safety features (`SEND_SMS`, `CALL_PHONE`, `ACCESS_BACKGROUND_LOCATION`, `ACTIVITY_RECOGNITION`, `HIGH_SAMPLING_RATE_SENSORS`) are only active when you explicitly enable the corresponding features in the App's settings. They are never used passively or for any purpose other than what is described above.

---

## 3. How We Use Your Information

We use the information collected for the following purposes:

| Purpose | Data Used |
|---|---|
| **Account management** | Email, username, Google profile, avatar |
| **Trip planning & itinerary generation** | Destination, dates, budget, preferences |
| **AI-powered travel assistant** | Chat messages, trip context, location (if enabled) |
| **Live voice assistant & interpreter** | Real-time audio streams, camera frames |
| **Expense tracking & budgeting** | Expense records, receipts, currency rates |
| **Ticket & document management** | Scanned ticket images, OCR text, QR/barcode data |
| **Map navigation & place search** | Location coordinates, search queries |
| **Proximity notifications** | Background location, itinerary place coordinates |
| **Fall detection & emergency safety** | Accelerometer data, GPS location, emergency contacts, medical profile |
| **Cloud sync across devices** | Trips, places, tickets, expenses (for authenticated users) |
| **Trip sharing** | Published trip name, destination, places, AI diary |

---

## 4. Data Storage & Security

### 4.1 Local Storage

- **Room Database** — trips, places, tickets, expenses, and place details are stored in an encrypted local SQLite database on your device.
- **DataStore Preferences** — app settings, emergency contacts, and medical profile information are stored locally using Android DataStore.
- **Internal file storage** — ticket images and temporary files are stored in the App's private internal storage directory.

### 4.2 Cloud Storage (Firebase)

For authenticated (non-guest) users who enable cloud sync:

- **Firebase Authentication** — securely manages your account credentials and authentication tokens.
- **Cloud Firestore** — stores your trips, itinerary places, and ticket metadata under your authenticated user ID.
- **Firebase Cloud Storage** — stores your profile avatar, ticket images, and (if enabled) emergency media files (SOS audio and photos).

All cloud data is transmitted over encrypted HTTPS connections. Firebase services are provided by Google and are governed by [Google's Privacy Policy](https://policies.google.com/privacy) and [Firebase Terms of Service](https://firebase.google.com/terms).

---

## 5. Data Sharing & Third-Party Services

We do not sell your personal data. We share data with third-party service providers only as necessary to deliver the App's features:

### 5.1 Google / Firebase Services

- **Firebase Authentication** — for account management and identity verification.
- **Cloud Firestore & Cloud Storage** — for cloud data synchronization.
- **Google Vertex AI (Gemini API)** — chat prompts, itinerary generation requests, receipt/ticket images for AI parsing, and real-time audio/video streams for the voice assistant are sent to Google's Gemini API for processing. See [Google's AI Privacy Policy](https://policies.google.com/privacy).

### 5.2 Mapbox

- Search queries, coordinates, and routing waypoints are sent to Mapbox for map rendering, geocoding, and navigation directions. See [Mapbox Privacy Policy](https://www.mapbox.com/legal/privacy).

### 5.3 Foursquare

- Place names, city queries, and coordinates are sent to Foursquare to retrieve place details, photos, ratings, and operating hours. See [Foursquare Privacy Policy](https://foursquare.com/legal/privacy).

### 5.4 Unsplash

- Destination name queries are sent to Unsplash to fetch cover photos for your trips. No personal data is transmitted. See [Unsplash Privacy Policy](https://unsplash.com/privacy).

### 5.5 Currency Exchange Rates

- Daily exchange rate data is fetched from a public open-source CDN. No personal data is transmitted.

### 5.6 On-Device ML Processing

The following features use Google ML Kit and TensorFlow Lite models that run **entirely on your device** — no personal data is sent to external servers for these features:

- Language identification
- On-device text translation
- Text recognition (OCR)
- Barcode and QR code scanning
- Fall detection (TensorFlow Lite)
- QR code generation

---

## 6. Trip Sharing

When you choose to publish a trip, the following information is made publicly accessible via a shareable link:

- Trip name, destination, duration, and AI-generated travel diary.
- Itinerary places including names, categories, coordinates, day numbers, and user ratings.

The following information is **never** included in shared trips:

- Personal expenses, budgets, or financial data.
- Ticket barcodes, confirmation codes, or scanned documents.
- Emergency contacts or medical information.
- Your email address or account details.

---

## 7. Emergency & Safety Features

### 7.1 Fall Detection

When enabled, the App runs a foreground service that monitors accelerometer data to detect falls. All sensor data processing occurs on your device using an on-device machine learning model.

### 7.2 SOS Emergency Alerts

Upon a confirmed fall detection or manual SOS activation, the App may (based on your settings):

- Send SMS messages to your configured emergency contacts containing your GPS location as a Google Maps link.
- Attach links to a 5-second audio recording and/or a camera snapshot (uploaded to Firebase Cloud Storage).
- Include your medical profile summary (if you enabled "Share during emergency").
- Initiate a phone call to your designated emergency number.

These actions only occur if you have explicitly enabled them in the App's safety settings and granted the necessary permissions.

---

## 8. Children's Privacy

PocketPlanner is not directed at children under the age of 13. We do not knowingly collect personal information from children under 13. If you believe we have inadvertently collected such information, please contact us so we can promptly delete it.

---

## 9. Data Retention & Deletion

- **Local data** — all locally stored data (Room database, DataStore preferences, cached files) can be cleared at any time by clearing the App's data through your device settings or by uninstalling the App.
- **Cloud data** — when you delete your account through the App's settings, we delete your profile avatar from Firebase Cloud Storage, clear your local preferences, and delete your Firebase Authentication account. Trip and ticket data stored in Cloud Firestore is associated with your user ID and follows Firebase's data retention policies.
- **Shared trips** — published shared trip data remains accessible via its share link. You can unpublish a trip at any time to remove it from public access.
- **Emergency media** — SOS audio recordings and photos stored in Firebase Cloud Storage are associated with your user ID and are deleted when you delete your account.

---

## 10. Your Rights & Choices

You have the following rights regarding your data:

- **Access & portability** — you can view all your data within the App at any time.
- **Correction** — you can edit your profile, trips, expenses, and other data within the App.
- **Deletion** — you can delete individual trips, expenses, and tickets, or delete your entire account through the App's settings.
- **Permissions** — you can grant or revoke any permission (location, camera, microphone, SMS, phone, notifications, sensors) at any time through your device's system settings. Revoking a permission will disable the corresponding feature.
- **Guest mode** — you can use the App without creating an account, in which case no data is transmitted to our cloud services.
- **Emergency data sharing** — you can toggle whether your medical profile is included in SOS messages.

---

## 11. Changes to This Privacy Policy

We may update this Privacy Policy from time to time. We will notify you of any material changes by posting the updated policy within the App and updating the "Last Updated" date at the top of this document. Your continued use of the App after such changes constitutes your acceptance of the revised Privacy Policy.

---

## 12. Contact Us

If you have any questions, concerns, or requests regarding this Privacy Policy or our data practices, please contact us at:

📧 **Email:** [your-email@example.com]

---

*This Privacy Policy is effective as of August 27, 2026.*

