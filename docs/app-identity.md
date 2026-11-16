# App identity

The product is **Logisim Evolution Mobile**, an independent Kotlin Multiplatform port derived from Logisim Evolution. It has no affiliation with the upstream project or promise of upstream support. The name reflects the origin of the file format and user experience. The icon is original artwork from this repository.

Both apps use the permanent identifier `thalesmms.logisim.mobile`, as chosen by the maintainer. The internal Kotlin namespace remains `dev.logisim.mobile` and is distinct from the installed app identifier. Android declares the fully qualified Activity class name to preserve this separation.

The earlier identifier `dev.logisim.mobile` belongs to the prototype. The new app installs separately and cannot read the prototype's private storage. Export your `.circ` projects from the prototype and import them into the new app. Verify the exported files before uninstalling the prototype.

## Change the version and build number

Edit only `app.properties`. Use a three-part version and a positive, increasing build number. Each published distribution requires a fresh build number. The current version is `0.2.0`, build `3`.

```sh
python3 tools/generate_app_identity.py
python3 tools/generate_app_identity.py --check
python3 -m unittest discover -s tools/tests -p 'test_app_identity.py'
```

Android reads the file directly. The generator updates `iosApp/Config/Identity.xcconfig`, `AppIdentity.kt`, and `docs/releases/app-identity.json`. Xcode uses the same version and build number for `CFBundleShortVersionString` and `CFBundleVersion`. The generated files are checked in so that opening the Xcode project requires no extra tooling.

The distribution scripts add the commit and artifact hashes to the output metadata. Recording the current commit in the checked-in identity files would create a reference that changes with every commit.

## Regenerate the icon

```sh
python3 tools/generate_app_icons.py
python3 tools/generate_app_icons.py --check
```

`tools/generate_app_icons.py` renders the same outlined AND gate, green terminals, and dotted canvas in two palettes. The generated artwork is in `artwork/app-icon.svg` and `artwork/app-icon-dark.svg`. The geometry is original to this project and uses the GPLv3 license in `LICENSE.md`. The artwork uses no upstream images or marks.

On Android, the adaptive layers use the launcher’s light or night resource configuration. The launcher can retain its cached icon after a theme change until it reloads the icon. The generator also supplies a monochrome layer for themed icons and bitmaps for older launchers. See the [Android adaptive icon guide](https://source.android.com/docs/core/display/adaptive-icons).

On iOS, the asset catalog contains opaque 1024×1024 RGB PNGs for Any and Dark. The system masks the artwork and selects Dark according to the Home Screen icon appearance. See the [Apple app icon asset catalog documentation](https://developer.apple.com/documentation/xcode/configuring-your-app-icon).

## Validation of this change

The debug APK built with `:androidApp:assembleDebug`. `aapt2 dump badging` confirmed identifier `thalesmms.logisim.mobile`, version `0.2.0`, and build `2`. `xcodebuild -showBuildSettings` confirmed the same values for the Apple host. `actool` compiled the catalog for iPhone and iPad. All four identity tests and the generated-file checks passed. These checks covered configuration and assets. Distribution signing and installation on physical devices require separate validation.
