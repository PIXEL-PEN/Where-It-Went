package com.pixelpen.whereitwent;

public class AccountHeaderRow implements AccountsRow {

    public final long accountId;
    public final String name;
    public final String total;
    public boolean expanded;

    public AccountHeaderRow(long accountId, String name, String total) {
        this.accountId = accountId;
        this.name = name;
        this.total = total;
        this.expanded = false;
    }

    @Override
    public int getType() {
        return TYPE_ACCOUNT_HEADER;
    }
}
