# ⚡ HamBlade Mod — Minecraft 1.21.1 (Fabric)

A lightsaber-style sword that charges up on each hit and unleashes armor-piercing damage!

## ✨ Features
- **HamBlade** — a glowing cyan lightsaber sword
- **Charge mechanic** — hits charge the blade (3 hits to full charge)
- **Armor-piercing** — full charge deals **3 hearts of true damage** (bypasses all armor)
- Lightsaber particle effects and sounds
- Charge bar shown in action bar while fighting

## 🛠️ How to Build & Install

### Requirements
- Java 21 JDK ([Download](https://adoptium.net/))
- Minecraft 1.21.1
- Fabric Loader 0.16.5+ ([Download](https://fabricmc.net/use/installer/))
- Fabric API 0.102.0+1.21.1 (put in your mods folder too)

### Build Steps
1. Open a terminal in this folder
2. Run: `./gradlew build` (Mac/Linux) or `gradlew.bat build` (Windows)
3. Find the built `.jar` in `build/libs/hamblade-1.0.0.jar`
4. Drop the `.jar` into your `.minecraft/mods/` folder
5. Launch Minecraft with Fabric!

## ⚔️ Crafting Recipe
```
 D 
 D 
 S 
```
- `D` = Diamond
- `S` = Blaze Rod

## 🎮 How It Works
- Hit a player/mob **1st time**: Charge 1/3 — yellow spark
- Hit **2nd time**: Charge 2/3 — orange spark  
- Hit **3rd time**: **FULL CHARGE** — massive lightning + 3 hearts armor-piercing damage!
- Charge resets after the full-charge strike

## 📁 Source Structure
```
src/main/java/com/hamblade/
  HamBladeMod.java         — Main mod entry
  item/HamBladeItem.java   — Sword logic (charge + damage)
  item/ModItems.java       — Item registration
```
