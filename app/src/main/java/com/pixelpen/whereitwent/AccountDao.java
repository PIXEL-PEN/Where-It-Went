package com.pixelpen.whereitwent;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AccountDao {

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    List<AccountEntity> getActiveAccounts();

    @Query("SELECT * FROM accounts ORDER BY archived, name COLLATE NOCASE")
    List<AccountEntity> getAllAccounts();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(AccountEntity account);

    @Update
    void update(AccountEntity account);

    @Delete
    void delete(AccountEntity account);

    @Query("UPDATE accounts SET archived = :archived WHERE id = :id")
    void setArchived(long id, boolean archived);

    @Query("SELECT COUNT(*) FROM accounts")
    int countAccounts();

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    AccountEntity getAccountByName(String name);


    @Query("SELECT * FROM accounts ORDER BY id ASC")
    List<AccountEntity> getAllAccountsById();


}
