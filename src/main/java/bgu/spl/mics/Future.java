package bgu.spl.mics;

import java.util.concurrent.TimeUnit;

/**
 * A Future object represents a promised result - an object that will
 * eventually be resolved to hold a result of some operation. The class allows
 * Retrieving the result once it is available.
 * 
 * Only private methods may be added to this class.
 * No public constructor is allowed except for the empty constructor.
 */
public class Future<T> {

	private T result;
	private boolean isResolved; 
	
	/**
	 * This should be the the only public constructor in this class.
	 */
	public Future() {
		result = null;
		isResolved = false;
	}
	
	/**
     * retrieves the result the Future object holds if it has been resolved.
     * This is a blocking method! It waits for the computation in case it has
     * not been completed.
     * <p>
     * @return return the result of type T if it is available, if not wait until it is available.
     * 	       
     */

	 // (!!!) check if try cach is neccecary
	public synchronized T get() {
		
		while (!isResolved) {
            try {
                wait(); // Wait until the result is resolved
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                return null; // Exit gracefully if interrupted
            }
        }
        return result;
	
    }
	
	/**
     * Resolves the result of this Future object.
     */
	public synchronized void resolve (T result) {
		if (!isResolved) { // Ensure result is set only once
            this.result = result;
            this.isResolved = true;
            notifyAll(); // Notify all threads waiting for the result
        }
	
    }
	/**
     * @return true if this object has been resolved, false otherwise
     */
	public boolean isDone() {
		return isResolved;
	}
	
	/**
     * retrieves the result the Future object holds if it has been resolved,
     * This method is non-blocking, it has a limited amount of time determined
     * by {@code timeout}
     * <p>
     * @param timout 	the maximal amount of time units to wait for the result.
     * @param unit		the {@link TimeUnit} time units to wait.
     * @return return the result of type T if it is available, if not, 
     * 	       wait for {@code timeout} TimeUnits {@code unit}. If time has
     *         elapsed, return null.
     */
	public synchronized T get(long timeout, TimeUnit unit) {
		if (!isResolved) {
			try {
				long millis = unit.toMillis(timeout); // Convert timeout to milliseconds
				wait(millis); // Wait for the specified duration
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // Restore interrupt status
				return null; // Exit gracefully if interrupted
			}
		}
		return result; // Return the result (may be null if timeout elapsed)
	}

}
