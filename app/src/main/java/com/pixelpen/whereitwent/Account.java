package com.pixelpen.whereitwent.model;

public class Account {

    // Stable identifier (Room later)
    public long id;

    // Display name
    public String name; // e.g. "Kitchen Renovation"

    // Type grouping
    public AccountType type;

    // UI-only state (NOT persisted)
    public boolean expanded = true;

    public Account(long id, String name, AccountType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
