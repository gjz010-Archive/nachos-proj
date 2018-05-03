package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;
/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
        boolean ret = true;
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    ret = false;
        else
            setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return ret;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
        boolean ret = true;
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
            ret = false;
        else
            setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return ret;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
		
		public java.util.PriorityQueue<ThreadState> effectiveQueue;
		
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
		Comparator<ThreadState> cmp=(ThreadState s1, ThreadState s2)->{
			int d1=s2.getEffectivePriority()-s1.getEffectivePriority();
			if(d1!=0) return d1;
			else {
				long d2=s1.startTime-s2.startTime;
				if(d2!=0) return d2<0?-1:1;
				else return s1.index-s2.index;
				
			}
		};
		this.effectiveQueue=new java.util.PriorityQueue(cmp);
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		ThreadState next=pickNextThread();
		if(next==null) return null;
		effectiveQueue.remove(next);
		next.acquire(this);
	    return next.thread;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
		if(effectiveQueue.isEmpty()) return null;
	    return effectiveQueue.peek();
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	public ThreadState worker=null;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    index=maxIndex++;
		
	    setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
	    // implement me
	    return effectivePriority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;
	    this.updatePriority(new HashSet<Integer>());
	    // implement me
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
	    // implement me
		this.waiting=waitQueue;
		this.startTime=Machine.timer().getTime();
		waitQueue.effectiveQueue.add(this);
		if(waitQueue.worker!=null){
			waitQueue.worker.updatePriority(new HashSet<>());
			
		}
	}
	public void updatePriority(HashSet<Integer> mask){
		if(mask.contains(this.index)){
			return;
		}
		int ep=priority;
		for(PriorityQueue pq: working){
			if(!pq.transferPriority) continue;
			if(pq.effectiveQueue.isEmpty()) continue;
			int tmp=pq.effectiveQueue.peek().getEffectivePriority();
			if(tmp>ep) ep=tmp;
		}
		if(ep!=getEffectivePriority()){
			if(waiting!=null) waiting.effectiveQueue.remove(this);
			effectivePriority=ep;
			if(waiting!=null){
				waiting.effectiveQueue.add(this);
				HashSet<Integer> newmask=new HashSet<>();
				newmask.addAll(mask);
				newmask.add(index);
				waiting.worker.updatePriority(newmask);
			}
		}
		
		
	}
	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
		if(this.waiting==waitQueue) this.waiting=null;
		if(waitQueue.worker!=null){
			ThreadState worker=waitQueue.worker;
			waitQueue.worker=null;
			worker.working.remove(waitQueue);
			worker.updatePriority(new HashSet<Integer>());
			
		}
		waitQueue.worker=this;
		this.working.add(waitQueue);
		updatePriority(new HashSet<Integer>());
	    // implement me
	}	

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	protected int effectivePriority;
	public long startTime;
	public int index;
	
	protected ArrayList<PriorityQueue> working=new ArrayList<>();
	protected PriorityQueue waiting=null;
    }
	public static int maxIndex=0;
	
}
