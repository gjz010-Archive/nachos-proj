package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	// implement me
	return new PriorityQueue(transferPriority);
    }
	
    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getLotteryThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getLotteryThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getLotteryThreadState(thread).setPriority(priority);
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
    public static final int priorityMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = Integer.MAX_VALUE;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected LotteryThreadState getLotteryThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new LotteryThreadState(thread);

	return (LotteryThreadState) thread.schedulingState;
    }
	
    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
		
		public LinkedList<LotteryThreadState> lotteryPool;
		
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;

		this.lotteryPool=new LinkedList<>();
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getLotteryThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getLotteryThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		LotteryThreadState next=pickNextThread();
		if(next==null) return null;
		lotteryPool.remove(next);
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
	private Random lottery=new Random();
	protected LotteryThreadState pickNextThread() {
		int size=getSize();
		if(size<=0) return null;
		int number=lottery.nextInt(size);
		for(LotteryThreadState s:lotteryPool){
			number-=s.getEffectivePriority();
			if(number<0) return s;
		}
		return null;
	}
	
	public int getSize(){
		int total=0;
		for(LotteryThreadState s:lotteryPool) total+=s.getEffectivePriority();
		return total;
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
	public LotteryThreadState worker=null;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class LotteryThreadState {
	/**
	 * Allocate a new <tt>LotteryThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public LotteryThreadState(KThread thread) {
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
		waitQueue.lotteryPool.add(this);
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
			if(pq.lotteryPool.isEmpty()) continue;
			int tmp=pq.getSize();
			ep+=tmp;
		}
		if(ep!=getEffectivePriority()){
			if(waiting!=null) waiting.lotteryPool.remove(this);
			effectivePriority=ep;
			if(waiting!=null){
				waiting.lotteryPool.add(this);
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
			LotteryThreadState worker=waitQueue.worker;
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
