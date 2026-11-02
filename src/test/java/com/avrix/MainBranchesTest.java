package com.avrix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Branch-focused unit tests for {@link Main} reflection-safe behavior.
 * <p>
 * {@link Main#main(String[])} is intentionally never invoked because it starts the full game runtime.
 */
@DisplayName("Main Branch Tests")
class MainBranchesTest {

    @Test
    @DisplayName("Should prevent direct instantiation of the entrypoint class")
    void shouldPreventDirectInstantiation() throws Exception {
        Constructor<Main> constructor = Main.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should strip repeated loader flags while preserving game argument order")
    void shouldStripRepeatedLoaderFlagsAndPreserveOrder() throws Exception {
        Method filter = Main.class.getDeclaredMethod("filterLoaderArguments", String[].class);
        filter.setAccessible(true);

        String[] result = (String[]) filter.invoke(
                null,
                (Object) new String[]{
                        "-game",
                        "--strict",
                        "--debug",
                        "--no-redirect-log",
                        "--strict",
                        "-more"
                }
        );

        assertThat(result).containsExactly("-game", "--debug", "-more");
    }
}
