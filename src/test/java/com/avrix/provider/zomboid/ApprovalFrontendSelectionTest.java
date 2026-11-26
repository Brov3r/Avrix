package com.avrix.provider.zomboid;

import com.avrix.core.Environment;
import com.avrix.provider.zomboid.approval.ZomboidApprovalFrontend;
import com.avrix.provider.zomboid.approval.ZomboidConsoleApprovalFrontend;
import com.avrix.provider.zomboid.approval.ZomboidSwingApprovalFrontend;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies approval frontend selection without touching the real console or display.
 */
@DisplayName("Approval frontend selection tests")
class ApprovalFrontendSelectionTest {

    @Test
    @DisplayName("Should not fall back to a GUI on a headless server")
    void shouldNotUseGuiOnHeadlessServer() {
        ZomboidApprovalFrontend frontend = ZomboidGameProvider.selectApprovalFrontend(
                Environment.SERVER, "auto", false, true);

        assertThat(frontend).isNotInstanceOf(ZomboidSwingApprovalFrontend.class);
        assertThat(frontend).isNotInstanceOf(ZomboidConsoleApprovalFrontend.class);
        assertThat(frontend.supportsInteractivePrompt()).isFalse();
    }

    @Test
    @DisplayName("Should prefer the console on a server attached to a terminal")
    void shouldPreferConsoleOnInteractiveServer() {
        ZomboidApprovalFrontend frontend = ZomboidGameProvider.selectApprovalFrontend(
                Environment.SERVER, "auto", true, true);

        assertThat(frontend).isInstanceOf(ZomboidConsoleApprovalFrontend.class);
    }

    @Test
    @DisplayName("Should use Swing on a server when AWT is available and no terminal is attached")
    void shouldUseSwingOnServerWhenAwtIsAvailable() {
        ZomboidApprovalFrontend frontend = ZomboidGameProvider.selectApprovalFrontend(
                Environment.SERVER, "auto", false, false);

        assertThat(frontend).isInstanceOf(ZomboidSwingApprovalFrontend.class);
    }

    @Test
    @DisplayName("Should use Swing on a client")
    void shouldUseSwingOnClient() {
        ZomboidApprovalFrontend frontend = ZomboidGameProvider.selectApprovalFrontend(
                Environment.CLIENT, "auto", false, true);

        assertThat(frontend).isInstanceOf(ZomboidSwingApprovalFrontend.class);
    }

    @Test
    @DisplayName("Should honor explicit frontend modes")
    void shouldHonorExplicitModes() {
        assertThat(ZomboidGameProvider.selectApprovalFrontend(Environment.SERVER, "none", true, false)
                .supportsInteractivePrompt()).isFalse();
        assertThat(ZomboidGameProvider.selectApprovalFrontend(Environment.SERVER, "console", false, false))
                .isInstanceOf(ZomboidConsoleApprovalFrontend.class);
        assertThat(ZomboidGameProvider.selectApprovalFrontend(Environment.SERVER, "swing", false, false))
                .isInstanceOf(ZomboidSwingApprovalFrontend.class);
    }

    @Test
    @DisplayName("Should deny unknown frontend modes")
    void shouldDenyUnknownModes() {
        assertThat(ZomboidGameProvider.selectApprovalFrontend(Environment.SERVER, "bogus", true, false)
                .supportsInteractivePrompt()).isFalse();
    }
}
