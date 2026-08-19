package test.game;

import java.util.List;

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
     * Target pipeline method to test chained mixin injections.
     *
     * @param traceLog list recording execution stages
     * @param payload  sample payload string
     * @return processed result
     */
    public String executePipeline(List<String> traceLog, String payload) {
        this.executionCounter++;
        traceLog.add("ORIGINAL_BODY: " + payload);
        return "PROCESSED: " + payload;
    }

    /**
     * @return current execution count
     */
    public int getExecutionCounter() {
        return executionCounter;
    }
}