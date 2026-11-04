package test.benchmark;

import java.util.List;

/**
 * Template target class transformed by generated benchmark mixins.
 */
public class MixinTargetTemplate {

    /**
     * Appends a base marker to the trace and returns a deterministic result.
     *
     * @param trace execution trace filled by the target and injected mixins
     * @param input payload to process
     * @return processed result
     */
    public String apply(List<String> trace, String input) {
        trace.add("BASE:" + input);
        return "RESULT:" + input;
    }
}
