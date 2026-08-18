package test.game;

/**
 * Simulated game service class to be transformed by mixins in unit tests.
 */
public class TargetGameService {

    private int executionCounter = 0;

    /**
     * Target method returning a greeting message.
     *
     * @param input player name
     * @return original greeting string
     */
    public String getGreeting(String input) {
        this.executionCounter++;
        return "Hello, " + input;
    }

    /**
     * @return current execution count
     */
    public int getExecutionCounter() {
        return executionCounter;
    }
}