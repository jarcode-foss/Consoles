# Consoles
A powerful set of plugins that provides programmable computers and a powerful map rendering API for craftbukkit and spigot servers. **Requires Java 8!**

### Licensing

[LGPL Version 3](http://www.gnu.org/licenses/lgpl-3.0.en.html) for `consoles-api`

[GPL Version 3](https://www.gnu.org/licenses/gpl.html) for `consoles-core`, `consoles-computer`, `consoles-bungee`, `consoles-nms-api`, and all NMS versioned consoles modules.

License for `consoles-fetcher`: see source file(s)

### Maven Repository

There is a maven repository up at [jarcode.ca/maven2](http://jarcode.ca/maven2), and you can view all the maven modules for consoles [in the repository browser](http://jarcode.ca/modules.php).

credit to wolfmitchell for website and repository hosting!

### Builds

I also deploy jars to the maven repository that have dependencies packed with them, which can be used as working builds of the plugin. Check out my [module browser](http://jarcode.ca/modules.php) on my website to browse through releases of Consoles (and other projects).

**warning:** use snapshot builds with care, they can be extremely unstable.

### Bug Reporting

Bug reporting is done using GitHub's bug tracker. For how to create detailed reports, see **[reporting bugs](https://github.com/wacossusca34/Consoles/wiki/Reporting-Bugs)**.

### API Overview

The API provides:

- A replacement for the default map renderer in the minecraft server
- A fast interface for painting to 'canvases' (that are actually a grid of maps)
- A workaround for this issue: https://bugs.mojang.com/browse/MC-46345
- Pixel-accurate interaction events with the map canvas and its components
- Different map data for each player (per-player rendering)
- Hooks for overriding command block functionality to link up to console components

Behind the scenes, this API:

- Contains a threaded painting system, calling repaints as little as possible for each player
- Contains its own packet listener, no need for ProtocolLib!
- Provides a basic component set for building interfaces with the console
- Provides _streaming_ support, so you can effectively map input and output to console components

Non-API features:

- Fully programmable Computers (separate plugin). Refer to the wiki.
- Reliable image rendering from URLs!

Notes:
 - This replaces maps, and _completely_ removes handheld map functionality. Fake map handlers/items are injected to ensure that the normal map system does not send packets and map out world regions.
 - This plugin/API is strictly for _map canvases_, which are sets of (modified) item frames in a grid containing maps that can display pixels in its own screen coordinates, for each player.
 - My code has very large NMS backends, which means writing support for older server versions and keeping this up to date will be difficult.
