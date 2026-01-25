package com.pixelpen.whereitwent;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "account_items",
        indices = {
                @Index("accountId"),
                @Index("date")
        }
)
public class AccountItemEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    // Foreign key reference to AccountEntity.id
    public long accountId;

    // Display date, e.g. "Jan 22"
    public String date;

    // Item name, e.g. "Hammer"
    public String item;

    // Category label, e.g. "TOOLS"
    public String category;

    // Stored as numeric for totals / currency handling
    public double amount;

    // Optional note
    public String note;

    public AccountItemEntity(long accountId,
                             String date,
                             String item,
                             String category,
                             double amount,
                             String note) {
        this.accountId = accountId;
        this.date = date;
        this.item = item;
        this.category = category;
        this.amount = amount;
        this.note = note;
    }
}
