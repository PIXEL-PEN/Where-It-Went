package com.pixelpen.whereitwent;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert
    void insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    List<Expense> getAll();

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    Expense getById(int id);

    @Query("DELETE FROM expenses WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM expenses")
    void clearAll();

    @Query("SELECT * FROM expenses WHERE date = :date ORDER BY id ASC")
    List<Expense> getByDate(String date);

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    List<Expense> getExpensesBetween(String startDate, String endDate);

    @Query("SELECT * FROM expenses WHERE " +
            "((substr(date, 9, 4)) || '-' || " +
            "CASE substr(date, 4, 3) " +
            " WHEN 'Jan' THEN '01' WHEN 'Feb' THEN '02' WHEN 'Mar' THEN '03' " +
            " WHEN 'Apr' THEN '04' WHEN 'May' THEN '05' WHEN 'Jun' THEN '06' " +
            " WHEN 'Jul' THEN '07' WHEN 'Aug' THEN '08' WHEN 'Sep' THEN '09' " +
            " WHEN 'Oct' THEN '10' WHEN 'Nov' THEN '11' WHEN 'Dec' THEN '12' END " +
            " || '-' || substr(date, 1, 2)) " +
            "BETWEEN :startIso AND :endIso " +
            "ORDER BY " +
            "((substr(date, 9, 4)) || '-' || " +
            "CASE substr(date, 4, 3) " +
            " WHEN 'Jan' THEN '01' WHEN 'Feb' THEN '02' WHEN 'Mar' THEN '03' " +
            " WHEN 'Apr' THEN '04' WHEN 'May' THEN '05' WHEN 'Jun' THEN '06' " +
            " WHEN 'Jul' THEN '07' WHEN 'Aug' THEN '08' WHEN 'Sep' THEN '09' " +
            " WHEN 'Oct' THEN '10' WHEN 'Nov' THEN '11' WHEN 'Dec' THEN '12' END " +
            " || '-' || substr(date, 1, 2)) ASC")
    List<Expense> getExpensesBetweenIso(String startIso, String endIso);




}
