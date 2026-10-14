package com.pixelpen.whereitwent;

public class AccountItem {

    public final long dateMillis;   // SINGLE SOURCE OF TRUTH
    public final String item;
    public final String category;
    public final CharSequence amount;   // v3.3: may hold two lines (trip + home)
    public final String note;

    public AccountItem(
            long dateMillis,
            String item,
            String category,
            CharSequence amount,
            String note
    ) {
        this.dateMillis = dateMillis;
        this.item = item;
        this.category = category;
        this.amount = amount;
        this.note = note;
    }
}
