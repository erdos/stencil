package io.github.erdos.stencil;

public final class PrepareOptions {

	private final static PrepareOptions instance = new PrepareOptions(false);

	private final boolean onlyIncludes;

	private PrepareOptions(boolean onlyIncludes) {this.onlyIncludes = onlyIncludes;}

	public static PrepareOptions options() {
		return instance;
	}

	public boolean isOnlyIncludes() {
		return onlyIncludes;
	}

	/**
	 * Prepared template should contain only fragment include directories but not other expressions.
	 */
	public PrepareOptions withOnlyIncludes() {
		return new PrepareOptions(true);
	}
}
