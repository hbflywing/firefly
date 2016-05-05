package com.firefly.utils.concurrent;

/**
 * <p>
 * A callback abstraction that handles completed/failed events of asynchronous
 * operations.
 * </p>
 *
 * @param <C>
 *            the type of the context object
 */
public interface Promise<C> {
	/**
	 * <p>
	 * Callback invoked when the operation completes.
	 * </p>
	 *
	 * @param result
	 *            the context
	 * @see #failed(Throwable)
	 */
	public void succeeded(C result);

	/**
	 * <p>
	 * Callback invoked when the operation fails.
	 * </p>
	 *
	 * @param x
	 *            the reason for the operation failure
	 */
	public void failed(Throwable x);

	/**
	 * <p>
	 * Empty implementation of {@link Promise}.
	 * </p>
	 *
	 * @param <U>
	 *            the type of the result
	 */
	public static class Adapter<U> implements Promise<U> {
		@Override
		public void succeeded(U result) {
		}

		@Override
		public void failed(Throwable x) {
			x.printStackTrace();
		}
	}
}
