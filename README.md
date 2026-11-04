# AQW Mobile Companion

Android companion app for reading visible AdventureQuest Worlds monster labels through Android screen capture and on-device ML Kit OCR.

## Monster catalogue
- Includes a built-in offline fallback list.
- Downloads public monster and boss names from the AdventureQuest Worlds community wiki when the overlay service starts and internet is available.
- Stores the downloaded names locally so recognition can continue offline.
- Normalizes wiki suffixes such as `(Monster)`, `(1)`, and `(Version 2)` before matching.

The catalogue is intended to cover monsters across maps; OCR quality and wiki completeness still affect recognition. It does not automate gameplay or read game memory.

## Build
The GitHub Actions workflow builds the debug APK and publishes it as a GitHub release.
