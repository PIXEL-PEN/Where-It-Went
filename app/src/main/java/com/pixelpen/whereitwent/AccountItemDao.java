package com.pixelpen.whereitwent;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AccountItemDao {

    // ----------------------------------------------------
    // INSERT
    // ----------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AccountItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AccountItemEntity> items);

    // ----------------------------------------------------
    // UPDATE
    // ----------------------------------------------------
    @Update
    void update(AccountItemEntity item);

    // ----------------------------------------------------
    // DELETE
    // ----------------------------------------------------
    @Delete
    void delete(AccountItemEntity item);

    @Query("DELETE FROM account_items WHERE accountId = :accountId")
    void deleteAllForAccount(long accountId);

    // ----------------------------------------------------
    // QUERY
    // ----------------------------------------------------
    @Query(
            "SELECT * FROM account_items " +
                    "WHERE accountId = :accountId " +
                    "ORDER BY date ASC, id ASC"
    )
    List<AccountItemEntity> getItemsForAccount(long accountId);

    @Query(
            "SELECT SUM(amount) FROM account_items " +
                    "WHERE accountId = :accountId"
    )
    Double getTotalForAccount(long accountId);

    @Query("SELECT COUNT(*) FROM account_items")
    int countItems();




}
