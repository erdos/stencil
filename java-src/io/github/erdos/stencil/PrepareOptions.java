package io.github.erdos.stencil;

import io.github.erdos.stencil.impl.FileHelper;

import java.io.File;

public final class PrepareOptions {

	private final static PrepareOptions instance = new PrepareOptions(false, null);

	private final boolean onlyIncludes;

	private final File temporaryDirectory;

	private PrepareOptions(boolean onlyIncludes, File temporaryDirectory) {
		this.onlyIncludes = onlyIncludes;
		this.temporaryDirectory = temporaryDirectory;
	}

	public static PrepareOptions options() {
		return instance;
	}

	public boolean isOnlyIncludes() {
		return onlyIncludes;
	}

	/**
	 * Used to override the default temporary directory that is used to store prepared templates.
	 */
	public File getTemporaryDirectoryOverride() {
		return temporaryDirectory;
	}

	public PrepareOptions withTemporaryDirectoryOverride(File tmpDir) {
		if (!tmpDir.exists()) {
			throw new IllegalArgumentException("Temporary directory does not exist: " + tmpDir);
		} else if (!tmpDir.isDirectory()) {
			throw new IllegalArgumentException("Temporary directory parameter is not a directory: " + tmpDir);
		} else {
			return new PrepareOptions(onlyIncludes, tmpDir);
		}
	}

	/**
	 * When marked withOnlyIncludes, then  the prepared template will evaluate only fragment include directives and
	 * not other expressions.
	 */
	public PrepareOptions withOnlyIncludes() {
		return new PrepareOptions(true, temporaryDirectory);
	}
}
