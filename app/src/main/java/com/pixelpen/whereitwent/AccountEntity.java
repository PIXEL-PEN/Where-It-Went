package com.pixelpen.whereitwent;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "accounts",
        indices = {
                @Index(value = {"name"}, unique = true)
        }
)
public class AccountEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    public String type;

    public boolean archived;

    // ---- Foreign currency (v3.3, database v6; Travel accounts only) ----

    /** Trip currency code, e.g. "VND". Null = foreign currency off. */
    public String foreignCurrency;

    /** Home currency code, e.g. "USD". Amounts are stored in this currency. */
    public String homeCurrency;

    /** Current rate: foreign units per 1 home unit (1 USD = 25,450 VND). */
    @ColumnInfo(defaultValue = "0")
    public double rate;

    /**
     * Journey a Travel account belongs to (v3.3, database v7), e.g.
     * "SE Asia 2026" for its Vietnam and Cambodia trips. Null = none.
     */
    public String journey;

    public boolean hasForeignCurrency() {
        return foreignCurrency != null && !foreignCurrency.isEmpty()
                && homeCurrency != null && !homeCurrency.isEmpty() && rate > 0;
    }

    public AccountEntity(String name, String type, boolean archived) {
        this.name = name;
        this.type = type;
        this.archived = archived;
    }
}
