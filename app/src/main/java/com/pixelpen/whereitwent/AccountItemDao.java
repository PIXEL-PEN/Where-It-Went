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
    // TOTALS
    // ----------------------------------------------------
    @Query(
            "SELECT SUM(amount) FROM account_items " +
                    "WHERE accountId = :accountId"
    )
    Double getTotalForAccount(long accountId);

    // ----------------------------------------------------
    // COUNTS
    // ----------------------------------------------------
    @Query("SELECT COUNT(*) FROM account_items")
    int countItems();

    // ----------------------------------------------------
    // LAST USED ACCOUNT
    // ----------------------------------------------------
    @Query(
            "SELECT accountId FROM account_items " +
                    "ORDER BY id DESC LIMIT 1"
    )
    Long getLastUsedAccountId();

    // ----------------------------------------------------
    // ITEMS — AUTHORITATIVE ORDER
    // ----------------------------------------------------
    @Query(
            "SELECT * FROM account_items " +
                    "WHERE accountId = :accountId " +
                    "ORDER BY dateMillis DESC, id DESC"
    )
    List<AccountItemEntity> getItemsForAccount(long accountId);

    @Query(
            "SELECT * FROM account_items " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    AccountItemEntity getItemById(long id);

    // ----------------------------------------------------
// CATEGORIES (PER ACCOUNT)
// ----------------------------------------------------
    @Query(
            "SELECT DISTINCT category " +
                    "FROM account_items " +
                    "WHERE accountId = :accountId " +
                    "AND category IS NOT NULL " +
                    "AND category != '' " +
                    "ORDER BY category COLLATE NOCASE"
    )
    List<String> getCategoriesForAccount(long accountId);


    @Query("SELECT DISTINCT category FROM account_items WHERE accountId = :accountId ORDER BY category")
    List<String> getDistinctCategoriesForAccount(long accountId);



}
