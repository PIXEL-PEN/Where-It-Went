package com.pixelpen.whereitwent;

public class AccountItem {

    public final long dateMillis;   // SINGLE SOURCE OF TRUTH
    public final String item;
    public final String category;
    public final String amount;
    public final String note;

    public AccountItem(
            long dateMillis,
            String item,
            String category,
            String amount,
            String note
    ) {
        this.dateMillis = dateMillis;
        this.item = item;
        this.category = category;
        this.amount = amount;
        this.note = note;
    }
}
