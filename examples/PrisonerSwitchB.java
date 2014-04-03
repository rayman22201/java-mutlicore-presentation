/**
 * A slightly more advanced version of the Prisoner Problem tha uses the ligthSwitchRoom class to demonstrate
 * the use of volatile by implementing a custom version of an atomic boolean value.
 */
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class PrisonerSwitchB {
    public static void main(String[] args) {
        int numPrisoners = 10;
        Thread prisonerThreads[] = new Thread[numPrisoners];

        LightSwitchRoom prisonCommonRoom = new LightSwitchRoom();

        // Pick the leader.
        Prisoner leadPrisoner = new Prisoner(prisonCommonRoom, numPrisoners, true);
        int leaderThreadNumber = ThreadLocalRandom.current().nextInt(0, (numPrisoners - 1) );

        // Create and Start all the threads
        for (int i = 0; i < numPrisoners; i++ ) {
            if(i == leaderThreadNumber) {
                prisonerThreads[i] = new Thread( leadPrisoner );
            } else {
                prisonerThreads[i] = new Thread( new Prisoner(prisonCommonRoom, numPrisoners, false) );
            }
            prisonerThreads[i].start();
        }

        // When the Leader returns, then we know we are done.
        try {
            prisonerThreads[leaderThreadNumber].join();
        } catch(InterruptedException e) {
            System.out.println("We were rudely interrupted");
            return;
        }

        // Stop all the other prisoners.
        for (int i = 0; i < numPrisoners; i++ ) {
            prisonerThreads[i].interrupt();
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
    public int myPrisonerCount = 1;

    public Prisoner(LightSwitchRoom roomReference, int prisonerCount, boolean leaderStatus) {
        this.prisonCommonRoom = roomReference;
        this.numPrisoners = prisonerCount;
        this.iAmTheLeader = leaderStatus;
    }

    public void run() {
        while( myPrisonerCount < numPrisoners ) {

            // If we were interrupted, something either went wrong, or the leader finished counting. Either way, just stop and return.
            if( Thread.interrupted() ) {
                return;
            }

            if( iAmTheLeader ) {
                // Lock, get the switch status, and flip the switch.
                if( prisonCommonRoom.flipSwitch(true) ) {
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
            int sleepTime = ThreadLocalRandom.current().nextInt(500, 5000);

            try {
                Thread.sleep(sleepTime);
            } catch(InterruptedException e) {
                return;
            }
        }
        return;
    }

}