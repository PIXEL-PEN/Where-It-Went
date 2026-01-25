package com.pixelpen.whereitwent;

import java.util.ArrayList;
import java.util.List;

public class AccountProject {

    public final String name;
    public final String total;
    public final List<AccountItem> items = new ArrayList<>();

    public AccountProject(String name, String total) {
        this.name = name;
        this.total = total;
    }

    public void addItem(AccountItem item) {
        items.add(item);
    }
}
