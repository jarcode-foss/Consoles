# Consoles
A powerful plugin that provides programmable computers and a powerful map rendering API for craftbukkit and spigot servers. **Requires Java 8!**

License: [GPL Version 3](https://www.gnu.org/licenses/gpl.html)

### Maven Repository

Add this repository in your `pom.xml`:

	<repositories>
        <repository>
            <id>jarcode-repo</id>
            <url>http://jarcode.ca/maven2</url>
        </repository>
    </repositories>

And then add the following dependency (where `VERSION` would be a valid version of Consoles, `1.8-beta` and up):

    <dependency>
        <groupId>jarcode</groupId>
        <artifactId>mc-consoles</artifactId>
        <version>VERSION</version>
        <type>jar</type>
        <scope>provided</scope>
    </dependency>

credit to wolfmitchell for website and repository hosting!

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

- Fully programmable Computers. Refer to the wiki.
- Reliable image rendering from URLs!

Notes:
 - May not be compatible with ProtocolLib. If this is the case, I will add support by either using ProtocolLib as an alternative packet listener, or by applying my packet wrapper after ProtocolLib does its instrumentation shenanigans.
 - This replaces maps, and _completely_ removes handheld map functionality. Fake map handlers/items are injected to ensure that the normal map system does not send packets and map out world regions.
 - This plugin/API is strictly for _map canvases_, which are sets of (modified) item frames in a grid containing maps that can display pixels in its own screen coordinates, for each player.
 - My code _heavily_ depends on NMS calls for various reasons. Builds of this plugin simply will be dependant on certain craftbukkit/spigot versions. If you don't like this, you can write the ~10 wrapper classes for all my NMS calls yourself.
