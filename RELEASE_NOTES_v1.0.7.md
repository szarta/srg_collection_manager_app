# SRG Collection Manager - Release v1.0.7

**Release Date:** November 29, 2025
**Version Code:** 10
**Version Name:** 1.0.7
**APK Size:** 162 MB (minified & optimized)

## 🎉 What's New

### QR Code Import/Export - Complete Implementation

This release completes the QR code sharing ecosystem, allowing users to share and import collections and decks seamlessly.

#### ✨ New Features

**1. QR Code Import**
- Scan QR codes from the new "Scan" tab
- Import collections to any folder (existing or new)
- Import decks with full structure preservation:
  - Entrance card (slot 0)
  - Competitor card (slot 0)
  - Deck cards (slots 1-30)
  - Finish cards
  - Alternate cards
  - Spectacle type (NEWMAN/VALIANT)
- Automatic camera permission handling
- User-friendly import dialogs with folder selection
- Success/error feedback

**2. Enhanced QR Code Export**
- Export collections from folder detail screen
- Export decks from deck editor
- QR codes display with clickable URLs
- Share with other users or between your own devices

**3. Website Integration**
- Deck articles on get-diced.com now display QR codes
- Click "Copy Shareable Link" on any deck article
- QR code appears with the shareable URL
- Scan with your phone to import the deck instantly

**4. UI Improvements**
- Bottom navigation tab renamed from "Collection" to "Lists" (better fit)
- Clean Material 3 design throughout
- Improved loading states and error messages

## 📱 Complete User Flow

### Export Flow
1. Open any collection folder or deck
2. Tap the 📱 button in the top bar
3. QR code appears with shareable URL
4. Share the QR code or URL with others

### Import Flow
1. Tap the "Scan" tab in bottom navigation
2. Grant camera permission (first time only)
3. Tap "Start Scanner"
4. Scan a QR code from get-diced.com
5. Choose destination folder (or create new)
6. Tap "Import"
7. Cards/deck imported with full structure

### Web to App Flow
1. Visit get-diced.com deck article
2. Click "Copy Shareable Link"
3. QR code displays on screen
4. Scan with your phone
5. Deck imports to app with all slots preserved

## 🔧 Technical Details

**New Components:**
- `ImportCollectionDialog.kt` - Folder selection dialog
- `ImportDeckDialog.kt` - Deck folder selection dialog
- Enhanced `QRCodeScanScreen.kt` - Complete import logic
- Enhanced `CollectionViewModel.kt` - Import functionality
- Enhanced `DeckViewModel.kt` - Deck import with structure

**API Integration:**
- `GET /api/shared-lists/{id}` - Fetch shared list data
- Automatic detection of COLLECTION vs DECK type
- Full deck structure parsing and recreation

**Dependencies Added:**
- `androidx.lifecycle:lifecycle-runtime-compose:2.6.2`

## 🛠️ Build Information

**Signing:**
- Release signed with production keystore
- Certificate DN: CN=SRG Inventory, OU=Mobile, O=SRG Collection Manager
- SHA-256: 7dda6cd03c1f52f7ee0df8ea8a54e20913d6de83f35da2b223f21eeae7cd089a

**Optimizations:**
- ProGuard/R8 minification enabled
- Resource shrinking enabled
- Size reduced from 176MB (debug) to 162MB (release)

## 📦 Installation

**APK Location:**
```
app/build/outputs/apk/release/app-release.apk
```

**Install via ADB:**
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Manual Install:**
1. Copy APK to device
2. Open file manager
3. Tap APK to install
4. Allow "Install from unknown sources" if prompted

## 🔒 Permissions

This release requires:
- **Camera** - For QR code scanning (requested at runtime)
- **Internet** - For fetching shared lists and card data
- **Storage** - For database and image storage

## ⚠️ Known Issues

None at this time.

## 🙏 Acknowledgements

Special thanks to:
- Steve Resk (SRG Universe) for permission and support
- SRG Universe team and wrestlers
- SRGPC.net contributors
- The SRG community
- Claude (Anthropic) for development assistance

## 📝 Notes for Test Users

1. **First Time Setup:**
   - App will request camera permission when you first use the Scan tab
   - Grant permission to enable QR code scanning

2. **Testing QR Import:**
   - Visit https://get-diced.com/article/d2-deck
   - Click "Copy Shareable Link"
   - Scan the displayed QR code
   - Import the deck to test the flow

3. **Feedback:**
   - Please report any issues or bugs
   - Test both collection and deck import
   - Try creating new folders during import

## 🔜 Coming Soon

- Additional deck articles with QR codes
- Deck validation features
- Enhanced sharing options
- Performance optimizations

---

**Download:** `app/build/outputs/apk/release/app-release.apk`
**Size:** 162 MB
**Min Android:** 8.0 (API 26)
**Target Android:** 15.0 (API 35)
