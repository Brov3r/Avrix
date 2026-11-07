package com.avrix.provider.zomboid.approval.ui;

import com.avrix.provider.zomboid.approval.ApprovalMessage;
import com.avrix.provider.zomboid.approval.ApprovalMessages;
import com.avrix.provider.zomboid.approval.MixinTargetInspector;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;
import com.avrix.provider.zomboid.approval.ui.detail.ApprovalChipElement;
import com.avrix.provider.zomboid.approval.ui.detail.ApprovalDetailElement;
import com.avrix.provider.zomboid.approval.ui.detail.ApprovalDetailGlyph;
import com.avrix.provider.zomboid.approval.ui.detail.ApprovalDividerElement;
import com.avrix.provider.zomboid.approval.ui.detail.ApprovalHeaderElement;
import com.avrix.provider.zomboid.approval.ui.detail.ApprovalSectionElement;
import com.avrix.provider.zomboid.approval.ui.detail.ApprovalTextElement;

import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Rich detail block shown below an expanded row.
 */
public final class ApprovalDetailPanel extends JComponent {

    private static final int PADDING_X = 16;
    private static final int PADDING_Y = 12;
    private static final int ELEMENT_GAP = 7;

    /**
     * Entry rendered by this panel.
     */
    private final ZomboidApprovalEntry entry;

    /**
     * Mixin inspection result for the entry.
     */
    private final MixinTargetInspector.Result inspection;

    /**
     * Localized messages.
     */
    private final ApprovalMessages messages;

    /**
     * Laid-out detail elements.
     */
    private List<ApprovalDetailElement> elements = List.of();

    /**
     * Width used for the current layout, or {@code -1} when layout is stale.
     */
    private int laidOutWidth = -1;

    /**
     * Creates a detail block for one entry.
     *
     * @param entry      approval entry
     * @param inspection mixin inspection result
     * @param messages   localized messages
     */
    public ApprovalDetailPanel(
            ZomboidApprovalEntry entry,
            MixinTargetInspector.Result inspection,
            ApprovalMessages messages
    ) {
        this.entry = entry;
        this.inspection = inspection;
        this.messages = messages;
        setOpaque(true);
        setBackground(ZomboidApprovalTheme.DETAIL_BG);
        setFont(ZomboidApprovalTheme.SMALL_FONT);
    }

    /**
     * Returns the preferred height for the given width.
     *
     * @param width available width
     * @return preferred height in pixels
     */
    public int preferredHeight(int width) {
        ensureLayout(width);
        int height = PADDING_Y * 2;
        for (ApprovalDetailElement element : elements) {
            height += element.height() + ELEMENT_GAP;
        }
        return Math.max(height, 48);
    }

    /**
     * Paints the expanded detail block.
     *
     * @param graphics target graphics
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(ZomboidApprovalTheme.DETAIL_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(ZomboidApprovalTheme.BORDER);
            g2.fillRect(0, 0, getWidth(), 1);
            g2.setColor(entry.previousSha256().isBlank()
                    ? ZomboidApprovalTheme.BADGE_NEW_FG
                    : ZomboidApprovalTheme.BADGE_CHANGED_FG);
            g2.fillRect(0, 0, 3, getHeight());

            ensureLayout(Math.max(160, getWidth()));
            int y = PADDING_Y;
            int width = getWidth() - PADDING_X * 2;
            for (ApprovalDetailElement element : elements) {
                element.paint(g2, PADDING_X, y, width);
                y += element.height() + ELEMENT_GAP;
            }
        } finally {
            g2.dispose();
        }
    }

    private void ensureLayout(int width) {
        int normalized = Math.max(160, width);
        if (normalized == laidOutWidth) {
            return;
        }
        elements = buildElements(normalized);
        laidOutWidth = normalized;
    }

    private List<ApprovalDetailElement> buildElements(int width) {
        int contentWidth = Math.max(120, width - PADDING_X * 2);
        List<ApprovalDetailElement> result = new ArrayList<>();
        result.add(new ApprovalHeaderElement(entry, messages));
        result.add(new ApprovalDividerElement());

        result.add(new ApprovalSectionElement(
                messages.get(ApprovalMessage.DETAIL_SECTION_METADATA),
                ApprovalDetailGlyph.INFO,
                ZomboidApprovalTheme.ACCENT
        ));
        result.add(new ApprovalTextElement(List.of(
                messages.format(ApprovalMessage.DETAIL_MOD_ID, ApprovalFormatting.blankToDash(entry.modId())),
                messages.format(ApprovalMessage.DETAIL_PLUGIN_ID, ApprovalFormatting.blankToDash(entry.pluginId())),
                messages.format(ApprovalMessage.DETAIL_AUTHOR, ApprovalFormatting.blankToDash(entry.modAuthor())),
                messages.format(ApprovalMessage.DETAIL_SOURCE, source(entry)),
                messages.format(ApprovalMessage.DETAIL_SIZE, ApprovalFormatting.formatSize(entry.size())),
                messages.format(
                        ApprovalMessage.DETAIL_MODIFIED,
                        ApprovalFormatting.formatDate(entry.modifiedEpochMillis(), messages)
                )
        ), ZomboidApprovalTheme.SMALL_FONT, ZomboidApprovalTheme.TEXT, 8));

        result.add(new ApprovalSectionElement(
                messages.get(ApprovalMessage.DETAIL_SECTION_ARCHIVE),
                ApprovalDetailGlyph.ARCHIVE,
                ZomboidApprovalTheme.ACCENT
        ));
        result.add(new ApprovalTextElement(
                ApprovalFormatting.wrapLines(
                        entry.jarPath().toString(),
                        getFontMetrics(ZomboidApprovalTheme.SMALL_FONT),
                        contentWidth
                ),
                ZomboidApprovalTheme.SMALL_FONT,
                ZomboidApprovalTheme.TEXT,
                0
        ));

        result.add(new ApprovalSectionElement(
                messages.get(ApprovalMessage.DETAIL_SECTION_INTEGRITY),
                ApprovalDetailGlyph.HASH,
                ZomboidApprovalTheme.ACCENT
        ));
        result.add(new ApprovalTextElement(
                ApprovalFormatting.wrapLines(
                        entry.sha256(),
                        getFontMetrics(ZomboidApprovalTheme.MONO_FONT),
                        contentWidth
                ),
                ZomboidApprovalTheme.MONO_FONT,
                ZomboidApprovalTheme.TEXT,
                0
        ));
        if (!entry.previousSha256().isBlank()) {
            result.add(new ApprovalSectionElement(
                    messages.get(ApprovalMessage.DETAIL_SECTION_PREVIOUS),
                    ApprovalDetailGlyph.HISTORY,
                    ZomboidApprovalTheme.WARN
            ));
            result.add(new ApprovalTextElement(
                    ApprovalFormatting.wrapLines(
                            entry.previousSha256(),
                            getFontMetrics(ZomboidApprovalTheme.MONO_FONT),
                            contentWidth
                    ),
                    ZomboidApprovalTheme.MONO_FONT,
                    ZomboidApprovalTheme.WARN,
                    0
            ));
        }

        result.add(new ApprovalSectionElement(
                messages.format(
                        ApprovalMessage.DETAIL_SECTION_MIXINS,
                        inspection.mixinClasses().size()
                ),
                ApprovalDetailGlyph.MIXIN,
                ZomboidApprovalTheme.ACCENT
        ));
        if (inspection.mixinClasses().isEmpty()) {
            result.add(new ApprovalTextElement(
                    List.of(messages.get(ApprovalMessage.DETAIL_MIXINS_EMPTY)),
                    ZomboidApprovalTheme.SMALL_FONT,
                    ZomboidApprovalTheme.MUTED,
                    0
            ));
        } else {
            result.add(new ApprovalChipElement(
                    inspection.mixinClasses(),
                    ZomboidApprovalTheme.BADGE_NEW_BG,
                    ZomboidApprovalTheme.ACCENT,
                    contentWidth,
                    getFontMetrics(ZomboidApprovalTheme.SMALL_FONT)
            ));
        }

        result.add(new ApprovalSectionElement(
                messages.format(
                        ApprovalMessage.DETAIL_SECTION_TARGETS,
                        inspection.targetClasses().size()
                ),
                ApprovalDetailGlyph.TARGET,
                ZomboidApprovalTheme.SECONDARY
        ));
        if (inspection.targetClasses().isEmpty()) {
            result.add(new ApprovalTextElement(
                    List.of(messages.get(ApprovalMessage.DETAIL_TARGETS_EMPTY)),
                    ZomboidApprovalTheme.SMALL_FONT,
                    ZomboidApprovalTheme.MUTED,
                    0
            ));
        } else {
            result.add(new ApprovalChipElement(
                    inspection.targetClasses(),
                    ZomboidApprovalTheme.BADGE_CHANGED_BG,
                    ZomboidApprovalTheme.SECONDARY,
                    contentWidth,
                    getFontMetrics(ZomboidApprovalTheme.SMALL_FONT)
            ));
        }

        if (!inspection.issues().isEmpty()) {
            result.add(new ApprovalSectionElement(
                    messages.get(ApprovalMessage.DETAIL_SECTION_NOTES),
                    ApprovalDetailGlyph.WARNING,
                    ZomboidApprovalTheme.DENY
            ));
            result.add(new ApprovalTextElement(
                    ApprovalFormatting.wrapLines(
                            String.join(" • ", inspection.issues()),
                            getFontMetrics(ZomboidApprovalTheme.SMALL_FONT),
                            contentWidth
                    ),
                    ZomboidApprovalTheme.SMALL_FONT,
                    ZomboidApprovalTheme.DENY,
                    0
            ));
        }
        return result;
    }

    private String source(ZomboidApprovalEntry entry) {
        if (!entry.workshopId().isBlank()) {
            return messages.format(ApprovalMessage.SOURCE_WORKSHOP, entry.workshopId());
        }
        if (!entry.sourceName().isBlank()) {
            return entry.sourceName();
        }
        return entry.sourceType().isBlank()
                ? messages.get(ApprovalMessage.SOURCE_LOCAL)
                : entry.sourceType();
    }
}
