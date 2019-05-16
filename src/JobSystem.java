import java.util.*;
import java.util.concurrent.*;

public class JobSystem {

    private static final int HOUR_TO_MILLI_FACTOR = 3600000;

    private long timeOutMillisec; // Maximum time allowed for a job to execute
    private final Map<String, Future> jobMap; // a job map holding <Id,Thread> for State lookup
    private Set<String> scheduledJobs; // a Set containing the all the scheduled jobs
    private Timer timer; // a Timer object that helps scheduling jobs
    private Set<String> runningThreads;

    private ScheduledExecutorService executorService; // core Pool to run concurrent threads
    private CompletionService<String> jobTracker; // an abstraction over executorService core pool
    private ScheduledExecutorService JobsTerminator; // a fixed size pool for timeouting exceeding jobs

    // Total usage of 4 + N Threads
    // N threads defined by the user upon object construction
    // 1 Daemon thread that fetches finished jobs for further processing / cleanup
    // 2 Additional Threads for Killing time exceeding jobs
    // 1 Additional thread which is actually allocated by Timer Object, that handles scheduled jobs and pushes them to Pool

    private static class MyCallableTask implements Callable<String> {

        private String uniqueID;
        private Runnable r;

        public MyCallableTask(String uniqueID, Runnable r){
            this.uniqueID = uniqueID;
            this.r = r;
        }
        @Override
        public String call() {
            r.run();
            return this.uniqueID;
        }
    }

    public JobSystem(int threadsThreshold, long timeOutMillisec) {

        if(threadsThreshold <=0  || timeOutMillisec <=0)
            throw new IllegalArgumentException();

        this.runningThreads = new HashSet<>();
        this.jobMap = new HashMap<>();
        this.scheduledJobs = new HashSet<>();
        this.timeOutMillisec = timeOutMillisec;
        this.timer = new Timer();
        this.executorService = Executors.newScheduledThreadPool(threadsThreshold);
        this.jobTracker = new ExecutorCompletionService<>(executorService);
        this.JobsTerminator = Executors.newScheduledThreadPool(2);



        // Background daemon thread, will constantly monitor finished jobs
        // and Clean from runningThreads map.
        // This can be used in order to post-process jobs, ie:
        // handle any return value of jobs for additional processing / aggregation
        new Thread(() -> {
            while(true){
                try {
                    final Future<String> f = jobTracker.take();
                    if(!f.isCancelled()) {
                        runningThreads.remove(f.get());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * This function takes a job, registers it Threadpool
     * and returns the id of the job (to be tracked)
     *
     * @param job
     * @return the id of the job.
     */

    public String execute(Runnable job){

        if(job == null)
            return null;

        String uniqueID = UUID.randomUUID().toString();

        execute(job, uniqueID);

        return uniqueID;
    }

    // private helper function

    private String execute(Runnable job, String uniqueID) {

        Future<String> f = jobTracker.submit(new MyCallableTask(uniqueID, job));
        JobsTerminator.schedule(() -> {
                runningThreads.remove(uniqueID);
                f.cancel(true);
        }, timeOutMillisec, TimeUnit.MILLISECONDS);

        jobMap.put(uniqueID, f);

        runningThreads.add(uniqueID);

        return uniqueID;

    }

    /**
     * @param job  a Runnable instance to be executed
     * @param time the delay time
     * @return
     */

    public String scheduledExecution(Runnable job, TimeFrame time) {

        if (job == null || time == null)
            return null;

        String uniqueID = UUID.randomUUID().toString();

        scheduledJobs.add(uniqueID);

        timer.schedule(new TimerTask() {
            @Override
            public void run () {
                scheduledJobs.remove(uniqueID);
                execute(job, uniqueID);
            }
        }, time.time * HOUR_TO_MILLI_FACTOR);

        return uniqueID;
    }


    /*
     * enum class defining constant values,
     * these values represent the possible scheduling options
     */

    public enum TimeFrame {
        ONE_HOUR(1),  //calls constructor with value 1
        TWO_HOURS(2),  //calls constructor with value 2
        SIX_HOURS(6),  //calls constructor with value 6
        TWELVE_HOURS(12); // calls constructor with value 12

        private final long time;

        TimeFrame(int time) {
            this.time = time;
        }
    }


    public boolean cancelJob(String id) {

        if(scheduledJobs.remove(id))
            return true;

        if(jobMap.get(id) != null) {
            return jobMap.get(id).cancel(true);
        }

        return false;
    }
    /**
     * @param timeOutMillisec sets a new value to job's execution timeout.
     */
    public void setTimeOutMillisec(long timeOutMillisec) {
        this.timeOutMillisec = timeOutMillisec;
    }

    /**
     * @param threadsThreshold sets a new value to max number of allowed concurrent jobs.
     */
    public void setThreadsThreshold(int threadsThreshold) {
        ((ThreadPoolExecutor) executorService).setCorePoolSize(threadsThreshold);
    }

    /*
    Following methods are part of the "Introspection"
     interface that JobSystem object offers.
     */

    /**
     *
     * @return max number of allowed threads to execute concurrently.
     */
    public long getThreadsThreshold() {
        return ((ThreadPoolExecutor)executorService).getCorePoolSize();
    }

    /**
     *
     * @return current TimeOutMilli sec for each Job.
     */
    public long getTimeOutMillisec() {
        return timeOutMillisec;
    }

    /**
     *
     * @return number of threads scheduled to be executed later.
     */
    public int getNumOfScheduledJobs() {
        return scheduledJobs.size();
    }

    /**
     *
     * @return number of running threads
     */
    public int getNumerOfRunningThreads() {
        return runningThreads.size();
    }

    /**
     * @param id of the job to lookup
     * @return the state of the job;
     */
    public String getJobState(String id) {

        // State lookup, Basically should treat these values as enums/constants rather than a String
        // That would be better, and especially important for Users to realize the interface
        if (scheduledJobs.contains(id)){
            return "Job is scheduled for later execution.";
        }
        if (jobMap.get(id) == null) {
            return "No such job exist.";
        }

        if (jobMap.get(id).isCancelled()){
            return "Job was interrupted!";
        }
        if (runningThreads.contains(id)){
            return "Job's running!";
        }

        return "Job's complete";
    }
}




