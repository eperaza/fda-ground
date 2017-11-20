package com.boeing.cas.supa.ground.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class FileUtil {

	private FileUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static Optional<File> getFileByNameFromDirectory(Path dirPath, String fileName) {

		return Arrays.stream(
				dirPath.toFile().listFiles(
						f -> f.getName()
						.equalsIgnoreCase(fileName)
						)
				)
				.max((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
	}
}
