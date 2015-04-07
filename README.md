# Consoles
A replacement map API and plugin for craftbukkit &amp; spigot servers

Just porting this over from a private repository - so this won't compile yet, but all the code is done & working.

The API is meant to do this following:

- Serve as a replacement for maps in the minecraft server
- Provide a fast interface for painting to 'canvases' (that are actually a grid of maps)
- Provide a workaround for this issue: https://bugs.mojang.com/browse/MC-46345
- Provide pixel-accurate interaction events with the map canvas and its components
- Provide different map data for each player (per-player rendering)
- Provide hooks for overriding commandblock functionality to link up to console components

Behind the scenes, this API:

- Contains a threaded painting system, calling repaints as little as possible for each player
- Contains its own packet listener, no need for ProtocolLib!
- Provides a basic component set for building interfaces with the console
- Provides _streaming_ support, so you can effectively map input and output to console components

In progress:

- A ComputerCraft-inspired feature that allows players to create and manage fully-featured UNIX-like computers. See source code.
