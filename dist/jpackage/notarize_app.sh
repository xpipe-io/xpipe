#!/bin/sh

set -e

APP_DIR="$1/build/dist/$3.app"
ARCHIVE="$TMPDIR/notarization.zip"

find "$APP_DIR/Contents/runtime/Contents/Home/bin/" -exec codesign -vvv --entitlements "$1/jpackage/Entitlements.plist" --options=runtime --force --strict --sign "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_NAME" {} \;
find "$APP_DIR/Contents/runtime/Contents/Home/lib/" -exec codesign -vvv --entitlements "$1/jpackage/Entitlements.plist" --options=runtime --force --strict --sign "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_NAME" {} \;
find "$APP_DIR/Contents/runtime/Contents/MacOS" -exec codesign -vvv --entitlements "$1/jpackage/Entitlements.plist" --options=runtime --force --strict --sign "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_NAME" {} \;
codesign -vvv --entitlements "$1/jpackage/Entitlements.plist" --options=runtime --force --strict --sign "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_NAME" "$APP_DIR/Contents/MacOS/xpipe" || true
codesign -vvv --entitlements "$1/jpackage/Entitlements.plist" --options=runtime --force --strict --sign "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_NAME" "$APP_DIR/Contents/MacOS/xpiped"

echo "Create keychain profile"
xcrun notarytool store-credentials "notarytool-profile" --apple-id "$MACOS_NOTARIZATION_APPLE_ID" --team-id "$MACOS_NOTARIZATION_TEAM_ID" --password "$MACOS_NOTARIZATION_APP_SPECIFIC_PASSWORD"

# We can't notarize an app bundle directly, but we need to compress it as an archive.
# Therefore, we create a zip file containing our app bundle, so that we can send it to the
# notarization service

echo "Creating temp notarization archive"
ditto -c -k --sequesterRsrc --keepParent "$APP_DIR" "$ARCHIVE"

# Here we send the notarization request to the Apple's Notarization service, waiting for the result.
# This typically takes a few seconds inside a CI environment, but it might take more depending on the App
# characteristics. Visit the Notarization docs for more information and strategies on how to optimize it if
# you're curious

set +e

# This is unreliable, so retry
for i in {1..5} ; do
    echo "Notarize app (try $i)"
    ID=$(xcrun notarytool submit "$ARCHIVE" --keychain-profile "notarytool-profile" --wait | grep -o -i -E '[0-9A-F]{8}-[0-9A-F]{4}-[4][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}' | head -1)
    echo "Submission ID is $ID"
    sleep 15
    xcrun notarytool log "$ID" --keychain-profile "notarytool-profile"
    if [ $? == 0 ]; then
        break;
    fi
done

# Finally, we need to "attach the staple" to our executable, which will allow our app to be
# validated by macOS even when an internet connection is not available.
echo "Attach staple"
xcrun stapler staple "$APP_DIR"