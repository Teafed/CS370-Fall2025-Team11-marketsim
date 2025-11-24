package com.gui.navigation;

public class SectionHeader implements SymbolListEntry {
    private final String title;
    private boolean collapsed;

    public SectionHeader(String title) {
        this.title = title;
        this.collapsed = false;
    }

    public String getTitle() { return title;}
    public boolean isCollapsed() { return collapsed; }
    public void toggleCollapsed() { collapsed = !collapsed; }
}