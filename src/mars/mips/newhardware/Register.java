/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.mips.newhardware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Observable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import mars.mips.hardware.AccessNotice;

/**
 * Abstraction to represent a register of a MIPS Assembler.
 *
 * @author Jason Bumgarner, Jason Shrewsbury, Ben Sherman
 * @version June 2003
 */
// FIXME DEADLOCKS AHOY!
public class Register extends Observable {

    // PENDING - Don't synchronized blocks imply volatility?
    // Probably implying shared memory, thus not needed?
    String name;
    volatile int value;

    /////////
    //////////
    //////////
    ///////////
    ///////////
    static class Mutex implements Lock, java.io.Serializable {

        // Our internal helper class
        private static class Sync extends AbstractQueuedSynchronizer {
            // Reports whether in locked state

            protected boolean isHeldExclusively() {
                return getState() == 1;
            }

            // Acquires the lock if state is zero
            public boolean tryAcquire(int acquires) {
                assert acquires == 1; // Otherwise unused
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }

            // Releases the lock by setting state to zero
            protected boolean tryRelease(int releases) {
                assert releases == 1; // Otherwise unused
                if (getState() == 0) throw new IllegalMonitorStateException();
                setExclusiveOwnerThread(null);
                setState(0);
                return true;
            }

            // Provides a Condition
            Condition newCondition() {
                return new ConditionObject();
            }

            // Deserializes properly
            private void readObject(ObjectInputStream s)
                    throws IOException, ClassNotFoundException {
                s.defaultReadObject();
                setState(0); // reset to unlocked state
            }
        }

        // The sync object does all the hard work. We just forward to it.
        private final Sync sync = new Sync();

        public void lock() {
            sync.acquire(1);
        }

        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        public void unlock() {
            sync.release(1);
        }

        public Condition newCondition() {
            return sync.newCondition();
        }

        public boolean isLocked() {
            return sync.isHeldExclusively();
        }

        public boolean hasQueuedThreads() {
            return sync.hasQueuedThreads();
        }

        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }
    }

//////////////////
/////////////
///////////
///////////
///////////
    Register(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Sets the value of the register. Observers are notified of the WRITE
     * operation.
     *
     * @param value Value to set the Register to.
     * @return previous value of register
     */
    public synchronized int set(int value) {
        int old = this.value;
        this.value = value;
        if (countObservers() > 0) {
            setChanged();
            notifyObservers(new RegisterAccessNotice(AccessNotice.WRITE, name));
        }
        return old;
    }

    /**
     * Returns the value of the Register. Observers are notified of the READ
     * operation.
     *
     * @return the value of the Register.
     */
    public synchronized int get() {
        int out = value;
        if (countObservers() > 0) {
            setChanged();
            notifyObservers(new RegisterAccessNotice(AccessNotice.READ, name));
        }
        return out;
    }
}
