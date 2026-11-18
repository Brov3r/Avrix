package test.game;

/**
 * Simulated game service used to verify MixinExtras injectors.
 */
public class ExtrasGameService {

    /**
     * Concatenates a prefix and the supplied value.
     *
     * @param value input value
     * @return prefixed value
     */
    public String format(String value) {
        return "base:".concat(value);
    }
}
