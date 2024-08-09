package com.avrix.ui.widgets;

import com.avrix.ui.NanoColor;
import com.avrix.ui.NanoDrawer;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Represents a {@link Widget} that displays tabular data in a scrollable table format.
 * <p>
 * The {@link TableWidget} provides features such as sorting by column, row selection, and rendering of the table with headers and rows.
 * </p>
 */
public class TableWidget extends ScrollPanelWidget {
    /**
     * The font name used for the table's text.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * The font name used for icons in the table.
     */
    protected String iconFontName = "FontAwesome";

    /**
     * The icon used to indicate sorting in descending order.
     */
    protected String iconDown = "\uf0d7";

    /**
     * The icon used to indicate sorting in ascending order.
     */
    protected String iconUp = "\uf0d8";

    /**
     * The name of the last sorted column.
     */
    protected String lastSortedColumn = null;

    /**
     * The font size used for the table's text.
     */
    protected int fontSize = 12;

    /**
     * The height of the header row.
     */
    protected int headerHeight = 32;

    /**
     * The offset of the border from the edge of the table.
     */
    protected int borderOffset = 2;

    /**
     * The thickness of the lines used to draw borders and lines in the table.
     */
    protected int lineThickness = 1;

    /**
     * The height of each row in the table.
     */
    protected int rowHeight = 24;

    /**
     * The index of the currently selected row.
     */
    protected int selectedIndex = -1;

    /**
     * The index of the currently hovered row.
     */
    protected int hoveredIndex = -1;

    /**
     * The Y position of the vertical scrollbar when the {@link TableWidget} was initialized.
     */
    protected int originalVerticalScrollBarY;

    /**
     * The positions of each column in the table.
     */
    protected int[] columnPositions;

    /**
     * The sorting order of the table (ascending or descending).
     */
    protected boolean ascendingOrder = true;

    /**
     * The widths of each column in the table.
     */
    protected List<Integer> columnWidths;

    /**
     * The list of column names in the table.
     */
    protected List<String> column;

    /**
     * The rows of the table, where each row is represented by a map of column names to cell values.
     */
    protected List<Map<String, String>> rows;

    /**
     * The color of the header background.
     */
    protected NanoColor headerColor = new NanoColor("#323232");

    /**
     * The color of the text in the header.
     */
    protected NanoColor headerTextColor = NanoColor.WHITE;

    /**
     * The color used for the accent of the selected row.
     */
    protected NanoColor accentColor = new NanoColor("#454545");

    /**
     * The color of the text in the rows.
     */
    protected NanoColor rowTextColor = NanoColor.WHITE;

    /**
     * The color of the row when hovered.
     */
    protected NanoColor rowHoverColor = new NanoColor("#202020");

    /**
     * The color of the lines used for borders and dividers in the table.
     */
    protected NanoColor lineColor = NanoColor.WHITE;

    /**
     * The action to be performed when a row is right-clicked.
     */
    protected Consumer<Map<String, String>> onRightClickAction = null;

    /**
     * The action to be performed when a row is double-clicked.
     */
    protected Consumer<Map<String, String>> onDoubleSelectAction = null;

    /**
     * The action to be performed when a row is selected.
     */
    protected Consumer<Map<String, String>> onSelectAction = null;

    /**
     * The timestamp of the last mouse click.
     */
    private long lastClickTime = 0;

    /**
     * The threshold time for detecting a double click in milliseconds.
     */
    private final long DOUBLE_CLICK_THRESHOLD = 300;

    /**
     * Constructs a new {@link TableWidget} with the specified position and size.
     *
     * @param x      The X position of the table.
     * @param y      The Y position of the table.
     * @param width  The width of the table.
     * @param height The height of the table.
     */
    public TableWidget(int x, int y, int width, int height) {
        super(x, y, width, height);

        this.column = new CopyOnWriteArrayList<>();
        this.rows = new CopyOnWriteArrayList<>();

        this.columnWidths = new ArrayList<>();
        this.columnPositions = new int[0];

        this.backgroundColor = new NanoColor("#181818");
        this.originalVerticalScrollBarY = this.verticalScrollbar.getY();
    }

    /**
     * Handles mouse movement events to update the hovered row index.
     *
     * @param x The relative X coordinate of the mouse.
     * @param y The relative Y coordinate of the mouse.
     */
    @Override
    public void onMouseMove(int x, int y) {
        super.onMouseMove(x, y);

        int relativeY = y - headerHeight + scrollY;
        if (relativeY >= 0) {
            int index = relativeY / (rowHeight + borderOffset);
            if (index >= 0 && index < rows.size()) {
                hoveredIndex = index;
            } else {
                hoveredIndex = -1;
            }
        } else {
            hoveredIndex = -1;
        }
    }

    /**
     * Determines if a click is within the header or scrollbars.
     *
     * @param x The X coordinate of the mouse click.
     * @param y The Y coordinate of the mouse click.
     * @return {@code true} if the click is not within the header or scrollbars; {@code false} otherwise.
     */
    protected boolean canClick(int x, int y) {
        boolean isInHeader = x > 0 && x < width && y > 0 && y < headerHeight;

        boolean isInVerticalScrollbar = verticalScrollbar.isVisible() &&
                x > verticalScrollbar.getX() - verticalScrollbar.getBorderOffset() &&
                x < verticalScrollbar.getX() + verticalScrollbar.getWidth() + verticalScrollbar.getBorderOffset() &&
                y > verticalScrollbar.getY() &&
                y < verticalScrollbar.getY() + verticalScrollbar.getHeight();

        boolean isInHorizontalScrollbar = horizontalScrollbar.isVisible() &&
                x > horizontalScrollbar.getX() &&
                x < horizontalScrollbar.getX() + horizontalScrollbar.getWidth() &&
                y > horizontalScrollbar.getY() - horizontalScrollbar.getBorderOffset() &&
                y < horizontalScrollbar.getY() + horizontalScrollbar.getHeight() + horizontalScrollbar.getBorderOffset();

        return !isInHeader && !isInVerticalScrollbar && !isInHorizontalScrollbar;
    }

    /**
     * Handles left mouse button down events to perform column sorting or row selection.
     *
     * @param x The relative X coordinate of the mouse click.
     * @param y The relative Y coordinate of the mouse click.
     */
    @Override
    public void onLeftMouseDown(int x, int y) {
        super.onLeftMouseDown(x, y);

        if (y <= headerHeight) {
            int columnIndex = getColumnIndexAt(x + scrollX);
            if (columnIndex >= 0 && columnIndex < column.size()) {
                String columnName = column.get(columnIndex);
                if (columnName.equals(lastSortedColumn)) {
                    ascendingOrder = !ascendingOrder;
                } else {
                    ascendingOrder = true;
                    lastSortedColumn = columnName;
                }

                sortByColumn(columnName, ascendingOrder);
                return;
            }
        }

        if (!canClick(x, y)) return;

        int relativeY = y - headerHeight + scrollY;
        if (relativeY >= 0) {
            int index = relativeY / (rowHeight + borderOffset);
            if (index >= 0 && index < rows.size()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < DOUBLE_CLICK_THRESHOLD) {
                    if (onDoubleSelectAction != null) {
                        onDoubleSelectAction.accept(rows.get(index));
                    }
                } else {
                    selectedIndex = index;
                    if (onSelectAction != null) {
                        onSelectAction.accept(rows.get(index));
                    }
                }
                lastClickTime = currentTime;
            }
        }
    }

    /**
     * Handles right mouse button down events to perform a right-click action on a row.
     *
     * @param x The relative X coordinate of the mouse click.
     * @param y The relative Y coordinate of the mouse click.
     */
    @Override
    public void onRightMouseDown(int x, int y) {
        super.onRightMouseDown(x, y);

        if (!canClick(x, y)) return;

        int relativeY = y - headerHeight + scrollY;
        if (relativeY >= 0) {
            int index = relativeY / (rowHeight + borderOffset);
            if (index >= 0 && index < rows.size()) {
                selectedIndex = index;
                if (onRightClickAction != null) {
                    onRightClickAction.accept(rows.get(index));
                }
            }
        }
    }

    /**
     * Retrieves the index of the column at the specified X coordinate.
     *
     * @param x The relative X coordinate to check.
     * @return The index of the column at the given X coordinate, or {@code -1} if no column is found.
     */
    protected int getColumnIndexAt(int x) {
        for (int i = 0; i < column.size(); i++) {
            int columnX = columnPositions[i];
            int columnWidth = columnWidths.get(i);

            if (x >= columnX && x < columnX + columnWidth) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sorts the table rows by the specified column in the given order.
     *
     * @param columnName The name of the column to sort by.
     * @param ascending  {@code true} to sort in ascending order, {@code false} for descending order.
     */
    public void sortByColumn(String columnName, boolean ascending) {
        selectedIndex = -1;

        rows.sort((row1, row2) -> {
            String value1 = row1.get(columnName);
            String value2 = row2.get(columnName);

            if (value1 == null && value2 == null) return 0;
            if (value1 == null) return ascendingOrder ? -1 : 1;
            if (value2 == null) return ascendingOrder ? 1 : -1;

            int comparison = value1.compareTo(value2);
            return ascendingOrder ? comparison : -comparison;
        });
    }

    /**
     * Adds a new row to the table with the specified values.
     *
     * @param values The values for the new row. Each value corresponds to a column in the table.
     */
    public void addRow(String... values) {
        Map<String, String> newRow = new HashMap<>();
        int numColumns = column.size();
        int numValues = (values != null) ? values.length : 0;

        for (int i = 0; i < numColumns; i++) {
            String value = (i < numValues && values[i] != null) ? values[i] : "-";
            newRow.put(column.get(i), value);
        }
        rows.add(newRow);
    }

    /**
     * Updates an existing row at the specified index with the new values.
     *
     * @param index  The index of the row to update.
     * @param values The new values for the row. Each value corresponds to a column in the table.
     */
    public void updateRow(int index, String... values) {
        if (index >= 0 && index < rows.size()) {
            Map<String, String> updatedRow = new HashMap<>();
            int numColumns = column.size();
            int numValues = (values != null) ? values.length : 0;

            for (int i = 0; i < numColumns; i++) {
                String value = (i < numValues && values[i] != null) ? values[i] : "-";
                updatedRow.put(column.get(i), value);
            }
            rows.set(index, updatedRow);
        }
    }

    /**
     * Sets the action to be performed when a row is selected.
     *
     * @param onSelectAction The action to be performed when a row is selected.
     */
    public void setOnSelectAction(Consumer<Map<String, String>> onSelectAction) {
        this.onSelectAction = onSelectAction;
    }

    /**
     * Sets the action to be performed when a row is double-clicked.
     *
     * @param onDoubleSelectAction The action to be performed when a row is double-clicked.
     */
    public void setOnDoubleSelectAction(Consumer<Map<String, String>> onDoubleSelectAction) {
        this.onDoubleSelectAction = onDoubleSelectAction;
    }

    /**
     * Sets the action to be performed when a row is right-clicked.
     *
     * @param onRightClickAction The action to be performed when a row is right-clicked.
     */
    public void setOnRightClickAction(Consumer<Map<String, String>> onRightClickAction) {
        this.onRightClickAction = onRightClickAction;
    }

    /**
     * Adds a new column to the table with the specified name and width.
     *
     * @param name  The name of the new column.
     * @param width The width of the new column.
     */
    public void addColumn(String name, int width) {
        column.add(name);

        columnWidths.add(width);
        columnPositions = new int[column.size()];
    }

    /**
     * Adds a new column to the table with the specified name and an undefined width.
     *
     * @param name The name of the new column.
     */
    public void addColumn(String name) {
        column.add(name);

        columnWidths.add(null);
        columnPositions = new int[column.size()];
    }

    /**
     * Clears all rows from the table.
     */
    public void clearRows() {
        rows.clear();
    }

    /**
     * Retrieves the currently selected row.
     *
     * @return The selected row as a map of column names to values, or {@code null} if no row is selected.
     */
    public Map<String, String> getSelectedRow() {
        if (selectedIndex >= 0 && selectedIndex < rows.size()) {
            return rows.get(selectedIndex);
        }
        return null;
    }

    /**
     * Retrieves a row by its index.
     *
     * @param index The index of the row to retrieve.
     * @return The row at the specified index as a map of column names to values, or {@code null} if the index is out of bounds.
     */
    public Map<String, String> getRowByIndex(int index) {
        if (index >= 0 && index < rows.size()) {
            return rows.get(index);
        }
        return null;
    }

    /**
     * Calculates and adjusts the widths of columns based on their content and available space.
     */
    protected void calculateColumnWidths() {
        int availableWidth = width - borderOffset * 2;
        int totalRequiredWidth = 0;
        int totalRequiredHeight = headerHeight + (rows.size() + 1) * rowHeight;

        for (int i = 0; i < column.size(); i++) {
            Integer columnWidth = columnWidths.get(i);
            if (columnWidth != null) {
                totalRequiredWidth += columnWidth;
            }
        }

        for (int i = 0; i < column.size(); i++) {
            if (columnWidths.get(i) == null) {
                String columnName = column.get(i);
                Vector2f columnTextSize = NanoDrawer.getTextSize(columnName, fontName, fontSize);
                Vector2f iconSize = NanoDrawer.getTextSize(iconDown, iconFontName, fontSize);

                int maxContentWidth = (int) (columnTextSize.x + iconSize.x * 4 + borderOffset * 3);

                columnWidths.set(i, maxContentWidth);
                totalRequiredWidth += maxContentWidth;
            }
        }

        if (totalRequiredWidth > availableWidth) {
            maxScrollX = totalRequiredWidth - availableWidth;
        } else {
            maxScrollX = 0;
            int extraSpace = availableWidth - totalRequiredWidth;

            int additionalWidthPerColumn = extraSpace / column.size();
            for (int i = 0; i < column.size(); i++) {
                columnWidths.set(i, columnWidths.get(i) + additionalWidthPerColumn);
            }
        }

        columnPositions = new int[column.size()];
        columnPositions[0] = borderOffset;
        for (int i = 1; i < column.size(); i++) {
            columnPositions[i] = columnPositions[i - 1] + columnWidths.get(i - 1);
        }

        if (totalRequiredHeight > height) {
            maxScrollY = totalRequiredHeight - height + (horizontalScrollbar.isVisible() ? horizontalScrollbar.height + horizontalScrollbar.getHeight() * 2 : 0);
        }
    }

    /**
     * Gets the font name used for rendering text in the {@link Widget}.
     *
     * @return the font name as a {@link String}.
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * Sets the font name used for rendering text in the {@link Widget}.
     *
     * @param fontName the font name to set.
     */
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Gets the icon font name used for rendering icons in the {@link Widget}.
     *
     * @return the icon font name as a {@link String}.
     */
    public String getIconFontName() {
        return iconFontName;
    }

    /**
     * Sets the icon font name used for rendering icons in the {@link Widget}.
     *
     * @param iconFontName the icon font name to set.
     */
    public void setIconFontName(String iconFontName) {
        this.iconFontName = iconFontName;
    }

    /**
     * Gets the icon used for indicating a downward sort direction in the {@link Widget}.
     *
     * @return the downward sort icon as a {@link String}.
     */
    public String getIconDown() {
        return iconDown;
    }

    /**
     * Sets the icon used for indicating a downward sort direction in the {@link Widget}.
     *
     * @param iconDown the downward sort icon to set.
     */
    public void setIconDown(String iconDown) {
        this.iconDown = iconDown;
    }

    /**
     * Gets the icon used for indicating an upward sort direction in the {@link Widget}.
     *
     * @return the upward sort icon as a {@link String}.
     */
    public String getIconUp() {
        return iconUp;
    }

    /**
     * Sets the icon used for indicating an upward sort direction in the {@link Widget}.
     *
     * @param iconUp the upward sort icon to set.
     */
    public void setIconUp(String iconUp) {
        this.iconUp = iconUp;
    }

    /**
     * Gets the name of the last sorted column in the {@link Widget}.
     *
     * @return the name of the last sorted column as a {@link String}.
     */
    public String getLastSortedColumn() {
        return lastSortedColumn;
    }

    /**
     * Sets the name of the last sorted column in the {@link Widget}.
     *
     * @param lastSortedColumn the name of the last sorted column to set.
     */
    public void setLastSortedColumn(String lastSortedColumn) {
        this.lastSortedColumn = lastSortedColumn;
    }

    /**
     * Gets the height of the header in the {@link Widget}.
     *
     * @return the header height as an {@link int}.
     */
    public int getHeaderHeight() {
        return headerHeight;
    }

    /**
     * Sets the height of the header in the {@link Widget}.
     *
     * @param headerHeight the header height to set.
     */
    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }

    /**
     * Gets the border offset in the {@link Widget}.
     *
     * @return the border offset as an {@link int}.
     */
    public int getBorderOffset() {
        return borderOffset;
    }

    /**
     * Sets the border offset in the {@link Widget}.
     *
     * @param borderOffset the border offset to set.
     */
    public void setBorderOffset(int borderOffset) {
        this.borderOffset = borderOffset;
    }

    /**
     * Gets the thickness of the lines in the {@link Widget}.
     *
     * @return the line thickness as an {@link int}.
     */
    public int getLineThickness() {
        return lineThickness;
    }

    /**
     * Sets the thickness of the lines in the {@link Widget}.
     *
     * @param lineThickness the line thickness to set.
     */
    public void setLineThickness(int lineThickness) {
        this.lineThickness = lineThickness;
    }

    /**
     * Gets the height of each row in the {@link Widget}.
     *
     * @return the row height as an {@link int}.
     */
    public int getRowHeight() {
        return rowHeight;
    }

    /**
     * Sets the height of each row in the {@link Widget}.
     *
     * @param rowHeight the row height to set.
     */
    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
    }

    /**
     * Gets the list of column names in the {@link Widget}.
     *
     * @return the list of column names as a {@link List} of {@link String}.
     */
    public List<String> getColumn() {
        return column;
    }

    /**
     * Sets the list of column names in the {@link Widget}.
     *
     * @param column the list of column names to set.
     */
    public void setColumn(List<String> column) {
        this.column = column;
    }

    /**
     * Gets the list of rows in the {@link Widget}.
     *
     * @return the list of rows as a {@link List} of {@link Map} objects, where each map represents a row.
     */
    public List<Map<String, String>> getRows() {
        return rows;
    }

    /**
     * Sets the list of rows in the {@link Widget}.
     *
     * @param rows the list of rows to set.
     */
    public void setRows(List<Map<String, String>> rows) {
        this.rows = rows;
    }

    /**
     * Gets the color of the header in the {@link Widget}.
     *
     * @return the header color as a {@link NanoColor}.
     */
    public NanoColor getHeaderColor() {
        return headerColor;
    }

    /**
     * Sets the color of the header in the {@link Widget}.
     *
     * @param headerColor the header color to set.
     */
    public void setHeaderColor(NanoColor headerColor) {
        this.headerColor = headerColor;
    }

    /**
     * Gets the color of the header text in the {@link Widget}.
     *
     * @return the header text color as a {@link NanoColor}.
     */
    public NanoColor getHeaderTextColor() {
        return headerTextColor;
    }

    /**
     * Sets the color of the header text in the {@link Widget}.
     *
     * @param headerTextColor the header text color to set.
     */
    public void setHeaderTextColor(NanoColor headerTextColor) {
        this.headerTextColor = headerTextColor;
    }

    /**
     * Gets the accent color used in the {@link Widget}.
     *
     * @return the accent color as a {@link NanoColor}.
     */
    public NanoColor getAccentColor() {
        return accentColor;
    }

    /**
     * Sets the accent color used in the {@link Widget}.
     *
     * @param accentColor the accent color to set.
     */
    public void setAccentColor(NanoColor accentColor) {
        this.accentColor = accentColor;
    }

    /**
     * Gets the color of the row text in the {@link Widget}.
     *
     * @return the row text color as a {@link NanoColor}.
     */
    public NanoColor getRowTextColor() {
        return rowTextColor;
    }

    /**
     * Sets the color of the row text in the {@link Widget}.
     *
     * @param rowTextColor the row text color to set.
     */
    public void setRowTextColor(NanoColor rowTextColor) {
        this.rowTextColor = rowTextColor;
    }

    /**
     * Gets the color of the row hover background in the {@link Widget}.
     *
     * @return the row hover color as a {@link NanoColor}.
     */
    public NanoColor getRowHoverColor() {
        return rowHoverColor;
    }

    /**
     * Sets the color of the row hover background in the {@link Widget}.
     *
     * @param rowHoverColor the row hover color to set.
     */
    public void setRowHoverColor(NanoColor rowHoverColor) {
        this.rowHoverColor = rowHoverColor;
    }

    /**
     * Gets the color of the line used for borders and separators in the {@link Widget}.
     *
     * @return the line color as a {@link NanoColor}.
     */
    public NanoColor getLineColor() {
        return lineColor;
    }

    /**
     * Sets the color of the line used for borders and separators in the {@link Widget}.
     *
     * @param lineColor the line color to set.
     */
    public void setLineColor(NanoColor lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * Updates the {@link Widget} state, including recalculating column widths and scrollbar positions.
     */
    @Override
    public void update() {
        super.update();

        calculateColumnWidths();

        if (verticalScrollbar.isVisible() && horizontalScrollbar.isVisible()) {
            horizontalScrollbar.width = horizontalScrollbar.compressedWidth;
        }

        verticalScrollbar.setY(originalVerticalScrollBarY + headerHeight);
        verticalScrollbar.setHeight(height - verticalScrollbar.borderOffset * 2 - headerHeight);

    }

    /**
     * Renders the {@link Widget}, including drawing the table headers, rows, and borders.
     */
    @Override
    public void render() {
        super.render();

        // Background
        drawRect(0, 0, width, headerHeight, headerColor);

        drawLine(0, headerHeight + lineThickness, width, headerHeight + lineThickness, lineThickness, lineColor);

        int visibleColumnStart = -1;
        int visibleColumnEnd = -1;

        for (int i = 0; i < column.size(); i++) {
            int columnX = columnPositions[i];
            int columnWidth = columnWidths.get(i);

            if (columnX + columnWidth > scrollX && visibleColumnStart == -1) {
                visibleColumnStart = i;
            }

            if (columnX < scrollX + width) {
                visibleColumnEnd = i;
            }
        }

        if (visibleColumnStart == -1 || visibleColumnEnd == -1) {
            visibleColumnStart = 0;
            visibleColumnEnd = column.size() - 1;
        }


        // Rows
        intersectScissor(0, headerHeight,
                width - (verticalScrollbar.isVisible() ? verticalScrollbar.width + verticalScrollbar.borderOffset * 2 : 0),
                height - headerHeight - (horizontalScrollbar.isVisible() ? horizontalScrollbar.height + horizontalScrollbar.borderOffset * 2 : 0));

        int rowY = headerHeight + lineThickness - scrollY;

        int visibleRowStart = Math.max(0, (scrollY) / (rowHeight + borderOffset));
        int visibleRowEnd = Math.min(rows.size() - 1, (scrollY + height - headerHeight) / (rowHeight + borderOffset));

        for (int rowIndex = visibleRowStart; rowIndex <= visibleRowEnd; rowIndex++) {
            Map<String, String> row = rows.get(rowIndex);
            int rowTop = rowY + rowIndex * (rowHeight + borderOffset);

            if (rowIndex == selectedIndex) {
                drawRect(0, rowTop, width, rowHeight, accentColor);
            } else if (rowIndex == hoveredIndex) {
                drawRect(0, rowTop, width, rowHeight, rowHoverColor);
            }

            for (int i = visibleColumnStart; i <= visibleColumnEnd; i++) {
                String columnName = column.get(i);
                String cellValue = row.get(columnName);
                int columnX = columnPositions[i];
                int columnWidth = columnWidths.get(i);

                intersectScissor(columnX + borderOffset * 2 - scrollX, rowTop, columnWidth - borderOffset * 4, rowHeight);

                Vector2f cellTextSize = NanoDrawer.getTextSize(cellValue, fontName, fontSize);

                drawText(cellValue, fontName,
                        (int) (columnX + (columnWidth - cellTextSize.x) / 2) - scrollX,
                        (int) (rowTop + borderOffset + (rowHeight - cellTextSize.y) / 2 - fontSize / 4),
                        fontSize, rowTextColor);

                NanoDrawer.restoreRenderState();

                if (rowIndex < rows.size() - 1) {
                    drawLine(columnX - scrollX, rowTop + rowHeight + lineThickness,
                            columnX + columnWidth - scrollX, rowTop + rowHeight + lineThickness,
                            lineThickness, lineColor);
                }
            }
        }

        NanoDrawer.restoreRenderState();

        // Header
        for (int i = visibleColumnStart; i <= visibleColumnEnd; i++) {
            String columnName = column.get(i);
            int columnX = columnPositions[i];
            int columnWidth = columnWidths.get(i);
            Vector2f columnTextSize = NanoDrawer.getTextSize(columnName, fontName, fontSize);

            int textX = (int) (columnX + (columnWidth - columnTextSize.x) / 2) - scrollX;
            int textY = (int) ((headerHeight - columnTextSize.y) / 2 - fontSize / 4);

            intersectScissor(columnX - scrollX, 0, columnWidth, headerHeight);

            drawText(columnName, fontName,
                    textX,
                    textY,
                    fontSize, headerTextColor);


            if (columnName.equals(lastSortedColumn)) {
                String currentIcon = ascendingOrder ? iconDown : iconUp;
                Vector2f iconSize = NanoDrawer.getTextSize(currentIcon, iconFontName, fontSize);

                drawText(currentIcon, iconFontName, (int) (textX + columnTextSize.x + borderOffset), (int) ((headerHeight - iconSize.y) / 2 - fontSize / 4), fontSize, headerTextColor);
            }

            NanoDrawer.restoreRenderState();

            if (i > 0) {
                drawLine(columnX - scrollX, borderOffset * (drawBorder ? 2 : 1), columnX - scrollX,
                        headerHeight - borderOffset * 2, lineThickness, lineColor);
                drawLine(columnX - scrollX, headerHeight + lineThickness * 2 + borderOffset,
                        columnX - scrollX,
                        height - borderOffset * 2 - (horizontalScrollbar.isVisible() ? horizontalScrollbar.height + horizontalScrollbar.borderOffset * 2 : 0),
                        lineThickness, lineColor);
            }
        }
    }
}