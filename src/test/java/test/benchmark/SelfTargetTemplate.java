package test.benchmark;

import java.util.List;

/**
 * Template class owned by a benchmark plugin and transformed by that plugin's own mixin.
 */
public class SelfTargetTemplate {

    /**
     * Appends a base marker to the trace and returns a deterministic result.
     *
     * @param trace execution trace filled by the target and the plugin's own mixin
     * @param input payload to process
     * @return processed result
     */
    public String process(List<String> trace, String input) {
        trace.add("SELF_BASE:" + input);
        return "SELF_RESULT:" + input;
    }
}
