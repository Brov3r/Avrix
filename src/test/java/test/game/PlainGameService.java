package test.game;

/**
 * Simulated game service that is never targeted by a mixin.
 */
public class PlainGameService {

    /**
     * Returns an unmodified greeting.
     *
     * @param input player name
     * @return greeting string
     */
    public String getGreeting(String input) {
        return "Hello, " + input;
    }
}
