package test.plugin;

/**
 * Test service used as a bytecode transformation target by cross-plugin mixin tests.
 */
public class BasePluginService {
    /**
     * Returns the original value used before a mixin transformation is applied.
     *
     * @param input input value
     * @return original prefixed value
     */
    public String calculateValue(String input) {
        return "ORIGINAL_A: " + input;
    }
}