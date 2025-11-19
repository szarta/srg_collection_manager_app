# Setup Guide for SRG Card Inventory App

## Quick Start

### 1. Install Android Studio
Download and install Android Studio Hedgehog (2023.1.1) or later from:
https://developer.android.com/studio

### 2. Open the Project
1. Launch Android Studio
2. Click "Open" and select this project directory
3. Wait for Gradle sync to complete

### 3. Install Required SDKs
Android Studio will prompt you to install:
- Android SDK Platform 34
- Android Build Tools
- Android SDK Command-line Tools

Click "Install" when prompted.

### 4. Set Up an Emulator (Optional)
If you don't have a physical device:
1. Tools → Device Manager
2. Click "Create Device"
3. Select a phone (e.g., Pixel 6)
4. Select Android 14.0 (API 34) system image
5. Click "Finish"

### 5. Run the App

#### On Physical Device:
1. Enable Developer Options on your Android device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → Developer Options
   - Enable "USB Debugging"
2. Connect device via USB
3. Click the green "Run" button in Android Studio
4. Select your device from the list

#### On Emulator:
1. Click the green "Run" button
2. Select the emulator you created
3. Wait for the emulator to boot

## Important Notes

### OpenCV Setup
The app uses OpenCV for Android. The build.gradle file includes:
```gradle
implementation("org.opencv:opencv:4.8.0")
```

If you encounter OpenCV initialization issues, you may need to:
1. Download OpenCV Android SDK from: https://opencv.org/releases/
2. Extract and copy to your project
3. Update the build.gradle to use the local SDK

### Database Setup
The `card_hashes.db` file is located in `app/src/main/assets/`. This database contains:
- 3,635 card entries
- Perceptual hashes for each card
- ORB feature descriptors

The app will automatically copy this database on first run.

### Camera Permissions
The app requires camera permission. On first launch:
1. The app will request camera permission
2. Grant the permission to use the scanning feature

## Troubleshooting

### Gradle Sync Failed
- Check your internet connection
- Click File → Invalidate Caches → Invalidate and Restart

### OpenCV Errors
If you see OpenCV initialization errors:
1. Check that OpenCV dependency is properly resolved
2. May need to use OpenCV Manager or bundle OpenCV with the app

### Camera Not Working
- Ensure camera permission is granted
- Check that your device/emulator has a working camera
- Physical devices work better than emulators for camera testing

### Build Errors
Common fixes:
```bash
# Clean and rebuild
./gradlew clean build

# Or in Android Studio:
# Build → Clean Project
# Build → Rebuild Project
```

## Project Structure Overview

```
srg_image_scan_app/
├── app/
│   ├── build.gradle.kts          # App-level build configuration
│   ├── proguard-rules.pro        # ProGuard rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── assets/
│           │   └── card_hashes.db    # Card reference database
│           ├── kotlin/com/srg/inventory/
│           │   ├── MainActivity.kt   # App entry point
│           │   ├── data/            # Database layer
│           │   ├── ui/              # UI screens & components
│           │   └── utils/           # Image processing utilities
│           └── res/                 # Android resources
├── build.gradle.kts              # Project-level build config
├── settings.gradle.kts           # Gradle settings
├── gradle.properties             # Gradle properties
├── README.md                     # Project documentation
└── SETUP.md                      # This file
```

## Testing the App

### Test Scanning Feature:
1. Launch the app
2. Grant camera permission
3. Point camera at a card image (can test with cards on screen)
4. Tap the camera button
5. App will attempt to match the card

### Test Collection Feature:
1. Add some cards via scanning or manual entry
2. Navigate to "Collection" tab
3. Filter by Owned/Wishlist
4. Tap a card to edit quantities

## Next Steps

After successful setup:
1. Test the scanning feature with actual SRG cards
2. Verify image recognition accuracy
3. Check collection management features
4. Consider adding features like:
   - Card images in the database
   - Export/import functionality
   - Statistics and analytics

## Getting Help

If you encounter issues:
1. Check Android Studio's "Build" output for error messages
2. Review the logcat for runtime errors
3. Ensure all dependencies are properly resolved
4. Check that the card_hashes.db file is in the assets folder

## Performance Tips

- Image recognition is CPU-intensive
- ORB feature extraction may take 1-2 seconds on older devices
- Consider adding a progress indicator for better UX
- The matching algorithm is optimized to filter by pHash first

## Development Tips

### Running Tests:
```bash
./gradlew test              # Unit tests
./gradlew connectedAndroidTest  # Instrumentation tests
```

### Creating a Release Build:
```bash
./gradlew assembleRelease
```

### Debugging:
- Use Android Studio's debugger
- Check logcat for log messages
- Set breakpoints in the code

Enjoy building with SRG Card Inventory!
