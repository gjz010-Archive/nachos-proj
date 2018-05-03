package nachos.threads;

import java.util.HashSet;
import java.util.Random;

import nachos.machine.Lib;
import nachos.machine.Machine;

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
    
    @Override
    public ThreadQueue newThreadQueue(boolean transferPriority) {
    	return new LotteryQueue(transferPriority);
    }
    
    @Override
    public int getPriority(KThread thread) {
    	Lib.assertTrue(Machine.interrupt().disabled());
    	
    	return getThreadState(thread).getPriority();
    }
    
    @Override
    public int getEffectivePriority(KThread thread) {
    	Lib.assertTrue(Machine.interrupt().disabled());
    	
    	return getThreadState(thread).getEffectivePriority();
    }
    
    @Override
    public void setPriority(KThread thread, int priority) {
    	Lib.assertTrue(Machine.interrupt().disabled());
    	Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
    	
    	getThreadState(thread).setPriority(priority);
    }
    
    @Override
	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	@Override
	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}
	
	public static final int priorityDefault = 1;

	public static final int priorityMinimum = 1;

	public static final int priorityMaximum = Integer.MAX_VALUE;
	
	protected Random random = new Random(priorityMaximum);
	
	@Override
	protected LotteryThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		return (LotteryThreadState) thread.schedulingState;
	}

    
    protected class LotteryQueue extends PriorityScheduler.PriorityQueue {
    	LotteryQueue(boolean transferPriority) {
			super(transferPriority);
		}
    	
    	@Override
		protected LotteryThreadState pickNextThread() {
			if (waitQueue.isEmpty())
				return null;

			int totalLottery = 0; 			
			int[] acculLottery = new int[waitQueue.size()]; // acculmulated lottery for threads in waitQueue.
			
			int i = 0;
			for (KThread thread : waitQueue) {
				totalLottery += getThreadState(thread).getEffectivePriority();
				acculLottery[i++] = totalLottery;
			}
			
			int lottery = random.nextInt(totalLottery);

			i = 0;
			for (KThread thread : waitQueue)
				if (lottery < acculLottery[i++])
					return getThreadState(thread);

			Lib.assertNotReached(); // ensure a thread is returned;
			return null;
		}
    }
    
    protected class LotteryThreadState extends PriorityScheduler.ThreadState {
		public LotteryThreadState(KThread thread) {
			super(thread);
		}

		@Override
		public int getEffectivePriority() {
			if (effectivePriority != effectivePriorityDefault)
				return effectivePriority;
			else 
				return getEffectivePriority(new HashSet<LotteryThreadState>());
		}
		
		private int getEffectivePriority(HashSet<LotteryThreadState> set) {
			
			if (set.contains(this)) {
				//break possible loop;
				return priority;
			}

			effectivePriority = priority;

			for (PriorityQueue queue : inheritQueue)
				if (queue.transferPriority)
					for (KThread thread : queue.waitQueue) {
						set.add(this);
						effectivePriority += getThreadState(thread).getEffectivePriority(set);
						set.remove(this);
					}
			
			PriorityQueue queue = (PriorityQueue) thread.waitForJoin;
			if (queue != null && queue.transferPriority)
				for (KThread thread : queue.waitQueue) {
					set.add(this);
					effectivePriority += getThreadState(thread).getEffectivePriority(set);
					set.remove(this);
				}

			return effectivePriority;
		}
	}
    
}
