# padMD for Android

padMD is a lightweight Markdown editor and viewer made for touch tablets. This folder wraps the same `padMD.html` page as an Android app, so it can:

- open `.md` and `.txt` files from My Files, OneDrive, email and other apps ("Open with padMD")
- save straight back over the file you opened
- share text to other apps and print to A4 or PDF

## Getting the app

Every push to this folder builds a new APK (the Android install file) with GitHub Actions and attaches it to the **padMD Android app** release (tag `padmd-latest`). On the tablet, open that release page, download `padMD.apk` and tap it. Android asks once to allow installs from your browser or My Files; allow it, then install.

## Updating the page

`app/src/main/assets/padMD.html` is the whole app. Edit it, push, and a new APK is built.

## Signing

New versions only install over old ones when they are signed with the same key. The build uses the repository secrets `PADMD_KEYSTORE_B64` and `PADMD_KEYSTORE_PASSWORD` when they exist; otherwise it creates a key once and keeps it in the GitHub Actions cache. A cached key is dropped if no build runs for 7 days, and then the next version needs the old app removed first (your files are not affected).
