package test.game;

import java.util.List;

/**
 * Simulated game service used to verify chained mixin injections.
 */
public class PipelineGameService {

    /**
     * Target pipeline method recording execution stages.
     *
     * @param traceLog list recording execution stages
     * @param payload  sample payload string
     * @return processed result
     */
    public String executePipeline(List<String> traceLog, String payload) {
        traceLog.add("ORIGINAL_BODY: " + payload);
        return "PROCESSED: " + payload;
    }
}
