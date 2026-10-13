# Vista Mac Fix

Client-only NeoForge 1.21.1 mod (`vista_mac_fix`) for a crash on Apple OpenGL.

Joshua's Mac (Apple M4, macOS 27, OpenGL 4.1) dies while joining a world that already has a powered Vista television. The Apple driver logs `gldCopyBufferSubData: NEEDS IMPLEMENTATION` and then aborts. That call is reached from Vista's live viewfinder render (Sodium chunk upload and Sable's airship buffer resize). Vista `1.21.1-4.4.11` is the build in the crash log. Vista `1.21.1-5.5.7` still does not replace the copy; its changelog has no Apple `glCopyBufferSubData` fix.

On macOS this mod cancels `VistaLevelRenderer.render` before the world picture is drawn and clears the feed targets to a flat placeholder. Windows and Linux keep the normal picture. If Vista is missing, or its render method does not match 4.4.11 / 5.5.x, the mixin is not applied.

Force the Mac path on another OS with `-Dvista_mac_fix.force=true`. Force it off with `-Dvista_mac_fix.force=false`.

## Build

```
./gradlew test build
```

The jar is `build/libs/vista_mac_fix-1.0.0.jar`. Put it in the client `mods` folder next to Vista. Do not put it on a dedicated server; the mod loads on the client only.

## In game

1. Start the client and open `latest.log`.
2. Look for `Vista Mac Fix 1.0.0 loaded. Live viewfinder skip active=true` on a Mac.
3. Join the world that has the powered television. The game should stay open. The TV shows a flat dark screen instead of the camera picture.
4. On Windows or Linux the same log line says `active=false`, and the television still shows the live picture.
