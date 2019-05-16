
public class Main {


    // Main class contains the trivial running context for the JobSystem object.
    // Upon Execution, Prints out the job Id
    // Looking carefully at the output of this Main , we can see the random order of the prints (ie: random job execution order)
    public static void main(String[] args) {

        JobSystem jobSystem = new JobSystem(100, 1000);

        for(int i = 0; i < 1000; i++){

            final int finalI = i;

            String a =  jobSystem.execute(() -> System.out.println("RUNNABLE " + finalI));

        }
    }
}
