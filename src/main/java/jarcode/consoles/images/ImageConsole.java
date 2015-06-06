package jarcode.consoles.images;

import jarcode.consoles.Consoles;
import jarcode.consoles.internal.ConsoleCreateException;
import jarcode.consoles.internal.ManagedConsole;
import jarcode.consoles.util.Position2D;
import jarcode.consoles.util.sync.SyncTaskScheduler;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.IOException;
import java.net.URL;

/*

A wrapper class for a Console that instantiates a console with components
and programming necessary to render an image onto the canvas

 */
public class ImageConsole {

	protected URL url;
	protected ManagedConsole console;
	private boolean created = false;
	private BlockFace face;
	private Location location;

	public ImageConsole(URL url, BlockFace face, Location location) {
		this(url, face, location, true);
	}
	public ImageConsole(URL url, BlockFace face, Location location, boolean save) {
		this.url = url;
		this.location = location;
		this.face = face;
		if (save) {
			ImageConsoleHandler.getInstance().imageConsoles.add(this);
		}
	}
	public void remove() {
		if (console != null)
			console.remove();
	}
	public URL getUrl() {
		return url;
	}
	public void create() {
		create(true);
	}
	public void create(final boolean save) {
		if (created) return;
		SyncTaskScheduler.getInstance().runSyncTask(() -> {
			PreparedMapImage image = null;
			try {
				image = ImageComponent.render(url);
				// perform manipulation on the image
				image.center();
				image.background((byte) 84);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return image;
		}, image -> {
			if (image == null) {
				remove();
				ImageConsoleHandler.getInstance().getImageConsoles().remove(ImageConsole.this);
				return;
			}
			else if (created) return;
			else
				created = true;
			int wt = image.getWidth() / 128;
			int w = image.getWidth() % 128 == 0 ? wt : wt + 1;
			int ht = image.getHeight() / 128;
			int h = image.getHeight() % 128 == 0 ? ht : ht + 1;
			console = new ManagedConsole(w, h, false);
			console.setType("image");
			console.putComponent(new Position2D(0, 0), new ImageComponent(console, image));
			try {
				console.create(face, location);
				if (save)
					ImageConsoleHandler.getInstance().save();
			} catch (ConsoleCreateException e) {
				console.remove();
				if (Consoles.debug)
					e.printStackTrace();
			}
		});
	}
}
