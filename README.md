# Prefix Blocker

An Android app that blocks incoming calls by number prefix, such as toll-free
800, 833, 844, 855, 866, 877 and 888, with exceptions for numbers you trust.

Repository: https://github.com/BinShalom/Android-APK_SpamCallsFilter

- Block any prefix of 1 to 10 digits, or tap a toll-free preset
- Exceptions: always allow specific numbers, and optionally everyone in your contacts
- Blocked calls still appear in your call history
- A log of recently blocked calls inside the app
- No internet access, no ads, no accounts, no tracking

## Install

1. Download the latest `.apk` from the
   [Releases page](https://github.com/BinShalom/Android-APK_SpamCallsFilter/releases).
2. Open it on your phone and allow installs from your browser or file manager if asked.
3. Open **Prefix Blocker** and tap **Set as call screening app**.
4. Add prefixes and exceptions.

Requires **Android 10 (API 29) or newer**. One APK works on all devices.

### Verify your download

Each release includes `SHA256SUMS.txt`. Check the APK with:

```
sha256sum -c SHA256SUMS.txt
```

Releases are signed with a key whose certificate SHA-256 fingerprint is:

```
e296158761776e0620e6a90943a92fddc6f22611330875d77fc46ac3adaf8d7e
```

Check it with `apksigner verify --print-certs <file>.apk`.

## How calls are decided

1. No blocked prefix matches: the call rings.
2. The number is on the exception list: the call rings.
3. The contacts exception is on and the number is a contact: the call rings.
4. Otherwise the call is rejected.

Calls from hidden or withheld numbers are always allowed, since there is no
number to match. A leading country code `1` is ignored, so `800` and `1800`
behave the same. Numbers outside the US and Canada are matched on raw digits.

## Permissions

| Permission | Why |
| --- | --- |
| Call screening role | Required by Android to screen incoming calls |
| Contacts (optional) | Only to allow callers saved in your contacts |

## Privacy

All settings and the blocked-call log are stored on your device only. The app
has no network permission and sends nothing anywhere. The contacts permission is
optional and is used only to check whether an incoming number is saved.

## Build from source

Requirements: JDK 17 or newer and the Android SDK (platform 35).

```
git clone https://github.com/BinShalom/Android-APK_SpamCallsFilter.git
cd Android-APK_SpamCallsFilter
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Without a keystore, `./gradlew assembleRelease` produces an unsigned APK. To
sign your own build, create `keystore.properties` in the project root:

```
storeFile=/path/to/your.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Limitations

- Blocks calls only, not SMS. Android has no screening API for texts.
- Some manufacturers aggressively stop background apps. If calls slip through,
  set the app's battery usage to Unrestricted.

## Contributing

Issues and pull requests are welcome at
https://github.com/BinShalom/Android-APK_SpamCallsFilter/issues

## License

[MIT](LICENSE)


