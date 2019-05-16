import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JobSystemTest {


    private final static String SCHEDULED =  "Job is scheduled for later execution.";
    private final static String RUNNING = "Job's running!";
    private final static String DOESNT_EXIST =  "No such job exist.";
    private final static String COMPLETE =  "Job's complete";
    private final static String INTERRUPTED = "Job was interrupted!";

    private static JobSystem jobSystem = null;

    @BeforeClass
    public static void setUpBeforeClass() {
        jobSystem = new JobSystem(10, 1000);
    }

    private String createNewJobExecution(long milliseconds) {

        return jobSystem.execute(() -> {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testAllPossibleStates() throws InterruptedException {

        // INTERRUPT A THREAD - TIMEOUT
        String id = createNewJobExecution(1500);

        Thread.sleep(1700);
        Assert.assertEquals("OK", INTERRUPTED, jobSystem.getJobState(id));

        // LET A THREAD COMPLETE IT'S JOB
        id = createNewJobExecution(30);
        Thread.sleep(100);
        Assert.assertEquals("OK", COMPLETE, jobSystem.getJobState(id));

        // LOOKUP FOR A THREAD THREAD THAT DOES NOT EXIST
        id = createNewJobExecution(30);;
        Thread.sleep(100);
        Assert.assertEquals("OK", DOESNT_EXIST, jobSystem.getJobState(id +1 ));

        // CHECK IF THREAD IS RUNNING
        id = createNewJobExecution(500);;
        Assert.assertEquals("OK", RUNNING, jobSystem.getJobState(id));

        // REPORT IF THREAD IS SCHEDULED
        id = jobSystem.scheduledExecution(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, JobSystem.TimeFrame.ONE_HOUR);
        Assert.assertEquals("OK", SCHEDULED, jobSystem.getJobState(id));

    }

    @Test
    public void Test_Number_Of_Threads() {

        // CHECK NUMBER OF RUNNING THREADS
        for(int i = 0 ; i < 10; i ++){
            createNewJobExecution(700);
        }
        Assert.assertEquals("OK", 10, jobSystem.getNumerOfRunningThreads());

        // CHECK NUMBER OF SCHEDULED THREADS
        for(int i = 0 ; i < 10; i ++) {

            jobSystem.scheduledExecution(() -> {
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, JobSystem.TimeFrame.ONE_HOUR);
        }
        Assert.assertEquals("OK", 10, jobSystem.getNumOfScheduledJobs());

        // ETC... Generally testing sizes of all possible states
    }

    @Test
    public void badInputTest() {

        // TEST BAD INPUT FOR scheduledExecution METHOD
        String s = jobSystem.scheduledExecution(null, JobSystem.TimeFrame.ONE_HOUR);
        Assert.assertNull("OK", s);

        // TEST BAD INPUT FOR execute METHOD
        s = jobSystem.execute(null);
        Assert.assertNull("OK", s);

        jobSystem.scheduledExecution(() -> {
            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, null);
        Assert.assertNull("OK", s);

        // ETC.. Generally cover all cases of bad inputs in the interface exposed to the user.
    }
    @Test
    public void cancelAnExistingJob() {

        // AS NAMED, INIT & CANCEL A JOB, THEN CHECK TO SEE THAT JOB HAS BEEN CHANGED
        String id =  createNewJobExecution(700);

        if(jobSystem.cancelJob(id)){
            Assert.assertEquals("OK", INTERRUPTED, jobSystem.getJobState(id));
        }
        else {
            Assert.assertEquals("OK", COMPLETE, jobSystem.getJobState(id));

        }
    }
    @Test
    public void checkPoolSize() {

        // CHECK THE INITIALIZATION VALUE MATCHES THE POOL CORE SIZE
        Assert.assertEquals("OK", 10, jobSystem.getThreadsThreshold());

        // AFTER CHANGING, CHECK THAT EQUALITY PERSISTS
        jobSystem.setThreadsThreshold(20);
        Assert.assertEquals("OK", 20, jobSystem.getThreadsThreshold());

    }
}