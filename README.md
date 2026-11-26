# Building Gadgets 2 - Unofficial Port

Five energy-powered construction tools for building, exchanging, copying, moving and destroying blocks, with a template manager for saved structures.

Original project: [Direwolf20-MC/BuildingGadgets2](https://github.com/Direwolf20-MC/BuildingGadgets2), version 26.1.2.3, revision 529c2962ffe0f5d2ef20125ccab9ad2426bc7a56.

## Installation
Minecraft 26.3, NeoForge 26.3.0.118, Java 25. Download the mod JAR from [Releases](https://github.com/JGorbachof/buildinggadgets2-26-3/releases). Install it on client and server. No additional mod is required; a compatible energy charger is needed for survival use.

## Alpha status
Headless server tests cover building, exchange, destruction, undo, material and energy accounting, sparse copies, interrupted paste with retained cargo, template transfers and chest contents. The suite also passed with Curios 17.0.1, AE2 26.3.2-alpha and GuideME 26.3.7-alpha installed. Client previews, real multiplayer, specific optional integration features and full modpack compatibility remain unverified. Back up worlds before use.

## Build and credits
Build with gradlew.bat build on Windows or ./gradlew build on Linux using Java 25. Original MIT license retained in LICENSE. Original project by Direwolf20 and contributors. Compatibility changes adapted from [don-pablo-bablo, PR #257](https://github.com/Direwolf20-MC/BuildingGadgets2/pull/257); sparse-copy repair adapted from [voidsip, PR #261](https://github.com/Direwolf20-MC/BuildingGadgets2/pull/261). See [PORT_NOTES.md](PORT_NOTES.md) and [original README](README.upstream.md). AI-assisted port development.

[ModHub](https://jgorbachof.github.io/) | [Discord](https://discord.gg/3zPAp2vk6Q)
