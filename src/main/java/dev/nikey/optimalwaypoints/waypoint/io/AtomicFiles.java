package dev.nikey.optimalwaypoints.waypoint.io;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicFiles {
	private AtomicFiles() {
	}

	public static void write(Path path, String content) throws IOException {
		Files.createDirectories(path.getParent());
		Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
		Files.writeString(tmp, content);
		try {
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
