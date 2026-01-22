package com.pixelpen.whereitwent;

public class AccountItemRow implements AccountsRow {

    public final long itemId;
    public final long accountId;
    public final String name;
    public final String amount;
    public final String date;
    public final String category;
    public final String note;
    public final String foreign;

    public AccountItemRow(
            long itemId,
            long accountId,
            String name,
            String amount,
            String date,
            String category,
            String note,
            String foreign
    ) {
        this.itemId = itemId;
        this.accountId = accountId;
        this.name = name;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.note = note;
        this.foreign = foreign;
    }

    @Override
    public int getType() {
        return TYPE_ACCOUNT_ITEM;
    }
}
