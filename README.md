# Pumpkin ESP

Fabric client mod for **Minecraft 1.21.1**. Highlights pumpkins, carved pumpkins, and jack o'lanterns with ESP up to **250 blocks** (within loaded chunks).

## Features
- Detects `pumpkin`, `carved_pumpkin`, `jack_o_lantern`
- Configurable range (default **250**)
- Box ESP + optional tracers
- Toggle keybind (default **P**)
- Incremental chunk scan (no lag spikes)

## Requirements
- Minecraft **1.21.1**
- Fabric Loader **0.16+**
- Fabric API

## Install
1. Install Fabric Loader for 1.21.1
2. Put `pumpkin-esp-1.0.0.jar` and Fabric API into `.minecraft/mods`
3. Press **P** in-game to toggle

## Build
```bash
./gradlew build
```
Output: `build/libs/pumpkin-esp-1.0.0.jar`

## Note
The client only has block data for **loaded chunks**. With render distance 16 (256 blocks), 250-block detection works for anything loaded around you. Unloaded terrain cannot be scanned client-side.
