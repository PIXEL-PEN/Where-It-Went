package com.pixelpen.whereitwent;

public class AccountTypeHeaderRow implements AccountsRow {

    public final String label;
    public final String total;

    public AccountTypeHeaderRow(String label, String total) {
        this.label = label;
        this.total = total;
    }

    @Override
    public int getType() {
        return TYPE_ACCOUNT_TYPE_HEADER;
    }
}
