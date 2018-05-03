package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	Lock lock;
	Condition listenReady;
	Condition speakReady;
	int waitingSpeakers=0;
	int waitingListeners=0;
	int activeListeners=0;
	boolean isWriting=false;
	int buffer;
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
		lock=new Lock();
		listenReady=new Condition(lock);
		speakReady=new Condition(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
		lock.acquire();
		while(waitingListeners==0 || isWriting){
			waitingSpeakers++;
			speakReady.sleep();
			waitingSpeakers--;
		}
		isWriting=true;
		System.out.println("Writing "+word+" to buffer.");
		buffer=word;
		if(waitingListeners>0){
			listenReady.wake();
		}
		lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
		int result;
		lock.acquire();
		while(!isWriting){
			waitingListeners++;
			if(waitingSpeakers>0) speakReady.wake();
			listenReady.sleep();
			waitingListeners--;
		}
		//activeListeners++;
		result=buffer;
		System.out.println("Reading "+result+" from buffer.");
		//activeListeners--;
		//if(activeListeners==0){ //The "last" leaving listener clean up the mess.
			isWriting=false;
			System.out.println("Cleanup");
			if(waitingSpeakers>0){
				speakReady.wake();
				
			}
		//}
		lock.release();
		System.out.println("Read Done.");
		return result;
    }
}
