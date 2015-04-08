# Consoles
A replacement map API and plugin for craftbukkit &amp; spigot servers. **Requires Java 8!**

The API is meant to does the following:

- Serves as a replacement for maps in the minecraft server
- Provides a fast interface for painting to 'canvases' (that are actually a grid of maps)
- Provides a workaround for this issue: https://bugs.mojang.com/browse/MC-46345
- Provides pixel-accurate interaction events with the map canvas and its components
- Provides different map data for each player (per-player rendering)
- Provides hooks for overriding commandblock functionality to link up to console components

Behind the scenes, this API:

- Contains a threaded painting system, calling repaints as little as possible for each player
- Contains its own packet listener, no need for ProtocolLib!
- Provides a basic component set for building interfaces with the console
- Provides _streaming_ support, so you can effectively map input and output to console components

In progress:

- A ComputerCraft-inspired feature that allows players to create and manage fully-featured UNIX-like computers. See source code.

Non-API features:

- Reliable image rendering from URLs!

Notes:
 - May not be compatible with ProtocolLib. If this is the case, I will add support by either using ProtocolLib as an alternative packet listener, or by applying my packet wrapper after ProtocolLib does its instrumentation shenanigans.
 - This replaces maps, and _completely_ removes handheld map functionality. Fake map handlers/items are injected to ensure that the normal map system does not send packets and map out world regions.
 - This plugin/API is strictly for _map canvases_, which are sets of (modified) item frames in a grid containing maps that can display pixels in its own screen coordinates, for each player.
 - My code _heavily_ depends on NMS calls for various reasons. Builds of this plugin simply will be dependant on certain craftbukkit/spigot versions.
