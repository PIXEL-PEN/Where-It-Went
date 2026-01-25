package com.pixelpen.whereitwent;

public class AccountItem {

    public final String date;      // "Jan 22"
    public final String item;      // "Hammer"
    public final String category;  // "TOOLS"
    public final String amount;    // "฿18.50"
    public final String note;      // optional

    public AccountItem(String date,
                       String item,
                       String category,
                       String amount,
                       String note) {
        this.date = date;
        this.item = item;
        this.category = category;
        this.amount = amount;
        this.note = note;
    }
}
