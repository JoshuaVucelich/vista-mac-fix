# Vista Mac Fix

Client-only NeoForge 1.21.1 mod (`vista_mac_fix`) for a crash on Apple OpenGL.

Joshua's Mac (Apple M4, macOS 27, OpenGL 4.1) aborts in the driver with `gldCopyBufferSubData: NEEDS IMPLEMENTATION`. Vista `1.21.1-4.4.11` is the build in the crash. Vista `1.21.1-5.5.7` does not replace the copy. The calls that reach the driver are Sodium's staging upload (`GL31C.glCopyBufferSubData` in `GLRenderDevice$ImmediateCommandList.copyBufferSubData`) and Sable's airship buffer resize (`ARBCopyBuffer.glCopyBufferSubData` in `BucketRenderBuffer.resize`).

On a Mac (`os.name` contains `Mac`), this mod wraps those two calls and copies the bytes on the CPU: `glGetBufferSubData` into a temporary buffer, then `glBufferSubData`, with the read and write bindings saved and restored. A buffer that is already mapped is read from its map pointer. Windows and Linux keep the native call. Each mixin uses `require = 0`, and the mixin config is not required, so a missing Sodium or Sable method does not crash.

`-Dvista_mac_fix.force=true` (or `VISTA_MAC_FIX_FORCE=true`) turns the Mac path on elsewhere. `-Dvista_mac_fix.force=false` turns it off, including on a Mac.

The jar is an ordinary client mod: `META-INF/neoforge.mods.toml` is at the top level, and the only hooks are Mixins. It loads on the client (`Dist.CLIENT`).

## Build

```
./gradlew test build
```

The jar is `build/libs/vista_mac_fix-1.0.2.jar`. Put it in the client `mods` folder.

## In game

1. Start the client and open `latest.log`.
2. On a Mac, look for `Vista Mac Fix 1.0.2 loaded. Apple buffer-copy fallback active=true`.
3. Look for `redirect applied to Sodium` and `redirect applied to Sable`. The first real copy also logs `redirected Sodium` or `redirected Sable`.
4. Join the world. The client should stay open.

## License

[MIT](LICENSE). Copyright (c) 2026 Joshua Vucelich.
