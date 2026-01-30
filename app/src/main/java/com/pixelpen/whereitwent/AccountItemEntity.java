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
