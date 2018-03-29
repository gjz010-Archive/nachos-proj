package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
	threadCount=0;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	conditionLock.release();
	//ExperimentNachos Begin: Condition Variable
	boolean intStatus = Machine.interrupt().disable();
	threadCount++;
	waitQueue.waitForAccess(KThread.currentThread());
	KThread.sleep();
	Machine.interrupt().restore(intStatus);
	//ExperimentNachos End.
	conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	//ExperimentNachos Begin: Condition Variable
	boolean intStatus = Machine.interrupt().disable();
	KThread thread = waitQueue.nextThread();
	if (thread != null) {
		threadCount--;
	    thread.ready();
	}
	Machine.interrupt().restore(intStatus);
	//ExperimentNachos End.
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	//ExperimentNachos Begin: Condition Variable
	while(threadCount>0) wake();
	//ExperimentNachos End.
    }

    private Lock conditionLock;
	private int threadCount=0;
	private ThreadQueue waitQueue =	ThreadedKernel.scheduler.newThreadQueue(false); //ExperimentNachos
}
