# Vista Mac Fix

Client-only NeoForge 1.21.1 mod (`vista_mac_fix`) for a crash on Apple OpenGL.

Joshua's Mac (Apple M4, macOS 27, OpenGL 4.1) aborts in the driver with `gldCopyBufferSubData: NEEDS IMPLEMENTATION`. Vista `1.21.1-4.4.11` is the build in the crash. Vista `1.21.1-5.5.7` does not replace the copy. The call is not limited to Vista's viewfinder: Sodium's staging upload and Sable's airship buffer resize hit the same unimplemented entry point, including during resource reload before the world joins.

On a Mac (`os.name` contains `Mac`), this mod rewrites every `glCopyBufferSubData` / `nglCopyBufferSubData` call on LWJGL `GL31C`, `GL31`, and `ARBCopyBuffer` into a CPU copy: `glGetBufferSubData` into a temporary buffer, then `glBufferSubData`, with the read and write bindings saved and restored. A buffer that is already mapped is read from its map pointer, because `glGetBufferSubData` is illegal on a mapped buffer. Windows and Linux keep the native call.

`-Dvista_mac_fix.force=true` (or `VISTA_MAC_FIX_FORCE=true`) turns the Mac path on elsewhere. `-Dvista_mac_fix.force=false` turns it off, including on a Mac.

The jar in `mods` is a small launch service. It adds the nested mod and attaches `vista-mac-fix-agent.jar`, which rewrites caller classes as they load. LWJGL is already initialized by then, so the native method itself is not patched.

`-Dvista_mac_fix.trap=true` makes the fallback throw if it would call the real GL function, and the agent replaces the LWJGL methods with that same throw. A missed call site fails the run instead of reaching the driver.

Sable 2.0.3 looks for `ViewFinderAccess$Block`, which Vista 4.4.11 and 5.5.7 do not ship. The jar includes that class so Sable's required mixin can apply. Nothing in the game calls it.

## Build

```
./gradlew test build
```

The jar is `build/libs/vista_mac_fix-1.0.1.jar`. Put it in the client `mods` folder. The mod loads on the client only.

## In game

1. Start the client and open `latest.log`.
2. On a Mac, look for `Vista Mac Fix agent installed`, `Vista Mac Fix 1.0.1 loaded. Apple buffer-copy fallback active=true`, and `redirected` lines for Sodium or Sable.
3. The first redirected copy logs `glCopyBufferSubData is using a CPU readback on this client`.
4. Join the world that has the powered television. The client should stay open, and the television can show its picture.
5. On Windows or Linux the same load line says `active=false`, and the native copy is left in place.

## License

[MIT](LICENSE). Copyright (c) 2026 Joshua Vucelich.
