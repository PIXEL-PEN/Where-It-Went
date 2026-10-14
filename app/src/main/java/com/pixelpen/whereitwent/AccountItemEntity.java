package com.pixelpen.whereitwent;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "account_items",
        indices = {
                @Index("accountId"),
                @Index("date"),
                @Index("dateMillis")
        }
)
public class AccountItemEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long accountId;

    public String date;

    public long dateMillis;   // NOT NULL, DEFAULT 0

    public String item;

    public String category;

    public double amount;

    public String note;

    // ---- Foreign currency (v3.3, database v6) ----
    // When set, the entry was made in the account's trip currency:
    // foreignAmount is what was typed (e.g. 500000 dong), rate is the rate
    // used, and amount holds the home-currency value (foreignAmount / rate).

    public Double foreignAmount;

    public Double rate;

    public boolean hasForeignAmount() {
        return foreignAmount != null && rate != null && rate > 0;
    }

    public AccountItemEntity(
            long accountId,
            String date,
            long dateMillis,
            String item,
            String category,
            double amount,
            String note
    ) {
        this.accountId = accountId;
        this.date = date;
        this.dateMillis = dateMillis;
        this.item = item;
        this.category = category;
        this.amount = amount;
        this.note = note;
    }
}
