package test.loader;

/**
 * Test provider whose construction fails with a configurable fatal JVM error.
 * <p>
 * {@link java.util.ServiceLoader} wraps constructor failures in
 * {@link java.util.ServiceConfigurationError}, which lets tests verify that fatal causes are rethrown instead of
 * being converted into recoverable discovery errors.
 */
public final class FatalGameProvider extends TestGameProvider {

    /**
     * Fatal error thrown by the next constructor invocation, or {@code null} to construct normally.
     */
    public static volatile VirtualMachineError failure;

    /**
     * Constructs a provider instance or throws the configured fatal error.
     */
    public FatalGameProvider() {
        VirtualMachineError configuredFailure = failure;
        if (configuredFailure != null) {
            throw configuredFailure;
        }
    }
}
