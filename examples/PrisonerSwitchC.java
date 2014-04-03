/**
 * Demonstrates the Prisioner Switch problem solved using a thread pool.
 */
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PrisonerSwitchC {
    public static void main(String[] args) {
        int numPrisoners = 10;
        ThreadPoolExecutor prisonerThreadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(5);

        LightSwitchRoom prisonCommonRoom = new LightSwitchRoom();

        // Pick the leader.
        Prisoner leadPrisoner = new Prisoner(prisonCommonRoom, numPrisoners, true);
        int leaderThreadNumber = ThreadLocalRandom.current().nextInt(0, (numPrisoners - 1) );

        // Create and Start all the threads
        while(leadPrisoner.myPrisonerCount < numPrisoners) {
            for (int i = 0; i < numPrisoners; i++ ) {
                if(i == leaderThreadNumber) {
                    prisonerThreadPool.execute( leadPrisoner );
                } else {
                    prisonerThreadPool.execute( new Prisoner(prisonCommonRoom, numPrisoners, false) );
                }
            }
            System.out.println("Prisoner Thread Pool Approx. Completed Task Count: " + prisonerThreadPool.getCompletedTaskCount());
            System.out.println("Prisoner Thread Pool Approx. Queue Size: " + (prisonerThreadPool.getTaskCount() - prisonerThreadPool.getCompletedTaskCount()));
            // If we don't sleep for some time, the Thread Pool work queue gets filled up faster than the the while condition can be checked,
            // causing a memory leak because the work queue basically fills up all available memory, and the thread pool can't catch up.
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                return;
            }
        }
        // Immediately stop any jobs in the queue and don't let any more jobs in.
        prisonerThreadPool.shutdownNow();

        // Wait until we are done shutting down.
        try {
            // This is the equivalent of Thread.join(), but the Thread Pool Interface requires you to set a timeout.
            prisonerThreadPool.awaitTermination(10L, TimeUnit.MINUTES);
        } catch(InterruptedException e) {
            System.out.println("We were rudely interrupted");
            return;
        }

        // Go talk to the Warden.
        System.out.println("Lead Prisoner, \"I declare that all " + leadPrisoner.myPrisonerCount + " Prisoners have entered the room at least once.\"");

        if(leadPrisoner.myPrisonerCount == numPrisoners) {
            System.out.println("Warden, \"Congratulations, you can all go free!\"");
        } else {
            System.out.println("Warden, \"Sorry, you loose. I will feed you all to my Crocodiles!\"");
        }
    }
}

/**
 * We demonstrate using volatile and atomic updaters to by using a volatile integer to implement our own atomic boolean.
 */
class LightSwitchRoom {
    private volatile int lightSwitch = 0;
    private AtomicIntegerFieldUpdater<LightSwitchRoom> lightSwitchUpdater = AtomicIntegerFieldUpdater.newUpdater(LightSwitchRoom.class, "lightSwitch");

    public boolean switchStatus() {
        return ( this.lightSwitchUpdater.get(this) == 1 );
    }

    public boolean flipSwitch(boolean expectedValue) {
        return lightSwitchUpdater.compareAndSet(this, expectedValue ? 1 : 0, (!expectedValue) ? 1 : 0 );
    }
}

class Prisoner implements Runnable {

    private LightSwitchRoom prisonCommonRoom;
    private boolean iAmTheLeader = false;
    private int numPrisoners;

    // Volatile assures that the master thread with the main method always reads a valid value.
    // The main thread is still a thread, so all the same rules for thread safety apply!
    public volatile int myPrisonerCount = 1;

    public Prisoner(LightSwitchRoom roomReference, int prisonerCount, boolean leaderStatus) {
        this.prisonCommonRoom = roomReference;
        this.numPrisoners = prisonerCount;
        this.iAmTheLeader = leaderStatus;
    }

    public void run() {
            // If we were interrupted, something either went wrong, or the leader finished counting. Either way, just stop and return.
            if( Thread.interrupted() ) {
                return;
            }

            if( iAmTheLeader ) {
                // Lock, get the switch status, and flip the switch.
                if( myPrisonerCount < numPrisoners && prisonCommonRoom.flipSwitch(true) ) {
                    myPrisonerCount++;
                    System.out.println("Lead Prisoner : Incrementing the count to : " + myPrisonerCount);
                }
            } else {
                // If the switch is false then flip it.
                if( prisonCommonRoom.flipSwitch(false) ) {
                    System.out.println("Prisoner found the switch off and flips it on.");
                } else {
                    System.out.println("Prisoner found the switch on and left it alone.");
                }
            }

            // You were put back in your cell. Sleep some random amount of time.
            int sleepTime = ThreadLocalRandom.current().nextInt(500, 1000);

            try {
                Thread.sleep(sleepTime);
            } catch(InterruptedException e) {
                return;
            }

            return;
    }

}