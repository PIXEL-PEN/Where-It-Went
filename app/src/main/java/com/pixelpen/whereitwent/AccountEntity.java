package com.pixelpen.whereitwent;

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

    public AccountEntity(String name, String type, boolean archived) {
        this.name = name;
        this.type = type;
        this.archived = archived;
    }
}
