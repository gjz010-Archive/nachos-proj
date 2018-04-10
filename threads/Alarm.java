package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue; //ExperimentNachos
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
	private static boolean inited=false;
    public Alarm() {
		if(inited) throw new Error();
		inited=true;
		waitingList=new PriorityQueue<>();
		lock=new Lock();
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	//lock.acquire();
	//System.out.println("Time!");
	boolean intStatus = Machine.interrupt().disable();
	long curr = Machine.timer().getTime();
	//System.out.println(waitingList.peek());
	while(waitingList.peek()!=null && waitingList.peek().isTimeout(curr)){
		//System.out.println("Poll!");
		cntr--;
		waitingList.poll().thread.ready();
	}
	//lock.release();
	Machine.interrupt().restore(intStatus);
	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	long wakeTime = Machine.timer().getTime() + x;
	boolean intStatus = Machine.interrupt().disable();
	//lock.acquire();
	//System.out.println("Push!");
	waitingList.add(new WaitingThread(KThread.currentThread(),wakeTime));
	//System.out.println(this);
	cntr++;
	//System.out.println(cntr);
	//System.out.println(waitingList.peek());
	//System.out.println(waitingList.peek());
	Machine.interrupt().restore(intStatus);
	//lock.release();
	intStatus = Machine.interrupt().disable();
	KThread.sleep();
	Machine.interrupt().restore(intStatus);
	//while (wakeTime > Machine.timer().getTime())
	//   KThread.yield();
    }
	private class WaitingThread implements Comparable{
		public KThread thread;
		public long timeout;
		public WaitingThread(KThread th, long to){
			thread=th;
			timeout=to;
			
		}
		public int compareTo(Object obj){
			if(!(obj instanceof WaitingThread)) return 0;
			WaitingThread wt=(WaitingThread)obj;
			if(timeout<wt.timeout) return -1;
			if(timeout==wt.timeout) return 0;
			if(timeout>wt.timeout) return 1;
			return 0;
		}
		public boolean isTimeout(long curr){
			return curr>=timeout;
			
		}
		public long getTimeout(){
			return timeout;
			
		}
	}
	private PriorityQueue<WaitingThread> waitingList;
	private Lock lock;
	private int cntr=0;
	
}
