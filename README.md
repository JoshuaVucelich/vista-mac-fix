# Vista Mac Fix

Client-only NeoForge 1.21.1 mod (`vista_mac_fix`), version 1.0.7.

Joshua's Mac (Apple M4, macOS 27, OpenGL 4.1) aborts in the driver with `gldCopyBufferSubData: NEEDS IMPLEMENTATION`. Vista `1.21.1-4.4.11` is the build in the crash. Vista `1.21.1-5.5.7` does not replace the copy. The calls that reach the driver are Sodium's staging upload (`GL31C.glCopyBufferSubData` in `GLRenderDevice$ImmediateCommandList.copyBufferSubData`) and Sable's airship buffer resize (`ARBCopyBuffer.glCopyBufferSubData` in `BucketRenderBuffer.resize`).

On a Mac (`os.name` contains `Mac`), this mod wraps those two calls and copies the bytes on the CPU: `glGetBufferSubData` into a temporary buffer, then `glBufferSubData`, with the read and write bindings saved and restored. A buffer that is already mapped is read from its map pointer. The map offset and length both come from `glGetBufferParameteri64v`, and the read must stay inside that range. Windows and Linux keep the native call. Each mixin uses `require = 0`, and the mixin config is not required, so a missing Sodium or Sable method does not crash.

That readback stalls the GPU. On a Mac, frame rate can dip while Sodium is building chunks.

`-Dvista_mac_fix.force=true` (or `VISTA_MAC_FIX_FORCE=true`) turns the Mac path on elsewhere. `-Dvista_mac_fix.force=false` turns it off, including on a Mac.

The jar is an ordinary client mod: `META-INF/neoforge.mods.toml` is at the top level, and the only hooks are Mixins. It loads on the client (`Dist.CLIENT`).

## Televisions on ships

Vista 4.4.11 measures a television from the camera to the block's position. On a Sable ship that position is the plot, not the ship on screen, so the screen is culled. This mod draws that screen with Vista 4.4.11's `getVideoFrameBuilder` (two `Vec2i` sizes and a trailing boolean) and skips that distance cull. A television in the normal world still uses Vista's renderer.

Vista Aeronautics Fix 1.0.0 (`vistafix`) overwrites the same renderer. Its overwrite calls `getVideoFrameBuilder` with three ints, which 4.4.11 does not have, and its quad method is empty. The jar is MIT and this mod does not copy it. When that broken mixin is present, a mixin config plugin removes it before the renderer class loads, and this mod draws the ship television. If the mixin is still in the method, the original renderer is cancelled for every television, including one whose screen is off, so the missing call does not run. When vistafix is absent, a failed draw on a lit screen leaves Vista's renderer to draw it. A linkage error, such as Vista 5.x missing the 4.4.11 method, disables this draw after one log. Other render errors log a stack at most once every five seconds.

vistafix has no gameplay classes. The mixin is listed in the common mixin array and targets a client renderer. A dedicated server does not need it. Remove it from the server: Vista marks `vista-aeronautics-fix` through 1.0.0 incompatible, and the mixin config is required.

## Build

```
./gradlew test build
```

The jar is `build/libs/vista_mac_fix-1.0.7.jar`. Put it in the client `mods` folder. Compiling the television mixin pulls the NeoForge Vista `1.21.1-4.4.11` and Moonlight `1.21.1-3.7.2` jars from the Modrinth Maven repository (version ids, because the shared version numbers also publish Fabric jars). The Distant Horizons bind compiles against Distant Horizons `3.0.3-b` (Modrinth version `oYXIfeus`). Those jars are compile-only.

## Distant Horizons and television feeds

Vista 4.4.11's `DistantHorizonsCompat.decorateRenderWithoutLOD` writes Distant Horizons horizontal quality on every live-feed update, and in OFF mode it also toggles rendering. Distant Horizons fires its config listeners on every API write. The horizontal-quality listener starts a thread named `DH-AbstractDelayedConfigTimer` and, two seconds later, clears LOD render data. That is the flicker when a television is active.

With both `vista` and `distanthorizons` loaded, this mod's mixin returns the feed task once and does not call the Distant Horizons config API. A `DhApiBeforeRenderEvent` cancels LOD drawing while that feed is on the stack, so the television still renders exactly once and the world LODs stay put. Client config `dhGuard` (default true) turns the fix off. `tvUpdateFps` (default 0) optionally caps Vista's live-feed scheduler. 0 leaves Vista's `update_fps` alone. Vista stores that cap as updates per tick, which is frames per second divided by 20.

`-Dvista_mac_fix.dhProbe=true` calls the feed wrapper five times during client setup and writes `run/dh-probe.txt`. `-Dvista_mac_fix.dhProbeExit=true` then exits. `./gradlew runClient -PdhProbe` sets both.

## In game

1. Start the client and open `latest.log`.
2. On a Mac, look for `Vista Mac Fix 1.0.7 loaded. Apple buffer-copy fallback active=true`.
3. Look for `redirect applied to Sodium` and `redirect applied to Sable`. The first real copy also logs `redirected Sodium` or `redirected Sable`.
4. Join the world. A powered television on an assembled Sable ship should keep its screen, with or without Vista Aeronautics Fix installed. With that mod installed, the log says `stopped vistafix TvRendererMixin`, or that this mod is drawing the screen so the missing call does not run.
5. The client should stay open. `drew a Vista television on a Sable contraption` is written the first time a ship television is drawn.
6. With Distant Horizons installed, look for `Distant Horizons feed guard mixin applied` and `bound DhApiBeforeRenderEvent`. The client config file is `config/vista_mac_fix-client.toml`.

## License

[MIT](LICENSE). Copyright (c) 2026 Joshua Vucelich.
