package com.postest.app.data.db

import androidx.room.*
import com.postest.app.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category WHERE active = 1 ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM category ORDER BY sortOrder, id")
    suspend fun listAll(): List<Category>

    @Insert suspend fun insert(c: Category): Long
    @Update suspend fun update(c: Category)
    @Delete suspend fun delete(c: Category)
    @Query("SELECT COUNT(*) FROM category") suspend fun count(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM product WHERE active = 1 AND categoryId = :catId ORDER BY id")
    fun observeByCategory(catId: Long): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE active = 1 ORDER BY id")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun byId(id: Long): Product?

    @Insert suspend fun insert(p: Product): Long
    @Update suspend fun update(p: Product)
    @Delete suspend fun delete(p: Product)
}

@Dao
interface TableDao {
    @Query("SELECT * FROM order_table ORDER BY id")
    fun observeAll(): Flow<List<OrderTable>>

    @Query("SELECT * FROM order_table WHERE id = :id")
    suspend fun byId(id: Long): OrderTable?

    @Insert suspend fun insert(t: OrderTable): Long
    @Update suspend fun update(t: OrderTable)
    @Delete suspend fun delete(t: OrderTable)
    @Query("SELECT COUNT(*) FROM order_table") suspend fun count(): Int
}

@Dao
interface OrderDao {
    @Insert suspend fun insertOrder(o: PosOrder): Long
    @Update suspend fun updateOrder(o: PosOrder)

    @Query("SELECT * FROM pos_order WHERE id = :id")
    suspend fun byId(id: Long): PosOrder?

    @Query("SELECT * FROM pos_order WHERE id = :id")
    fun observe(id: Long): Flow<PosOrder?>

    @Query("SELECT * FROM pos_order WHERE status = 'OPEN' ORDER BY createdAt DESC")
    fun observeOpen(): Flow<List<PosOrder>>

    @Query("SELECT * FROM pos_order WHERE shiftId = :shiftId")
    suspend fun listByShift(shiftId: Long): List<PosOrder>

    @Query("SELECT * FROM pos_order WHERE createdAt BETWEEN :from AND :to AND status = 'PAID'")
    suspend fun listPaidBetween(from: Long, to: Long): List<PosOrder>

    @Insert suspend fun insertItem(i: OrderItem): Long
    @Update suspend fun updateItem(i: OrderItem)
    @Delete suspend fun deleteItem(i: OrderItem)

    @Query("SELECT * FROM order_item WHERE orderId = :orderId ORDER BY id")
    fun observeItems(orderId: Long): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_item WHERE orderId = :orderId")
    suspend fun listItems(orderId: Long): List<OrderItem>

    @Query("SELECT * FROM order_item WHERE orderId = :orderId AND sentToKitchen = 0")
    suspend fun listUnsentItems(orderId: Long): List<OrderItem>

    @Query("UPDATE order_item SET sentToKitchen = 1 WHERE orderId = :orderId AND sentToKitchen = 0")
    suspend fun markAllSent(orderId: Long)
}

@Dao
interface PaymentDao {
    @Insert suspend fun insert(p: Payment): Long
    @Query("SELECT * FROM payment WHERE orderId = :orderId")
    suspend fun listByOrder(orderId: Long): List<Payment>

    @Query("SELECT * FROM payment WHERE shiftId = :shiftId")
    suspend fun listByShift(shiftId: Long): List<Payment>

    @Query("SELECT * FROM payment WHERE paidAt BETWEEN :from AND :to")
    suspend fun listBetween(from: Long, to: Long): List<Payment>
}

@Dao
interface ShiftDao {
    @Insert suspend fun insert(s: Shift): Long
    @Update suspend fun update(s: Shift)

    @Query("SELECT * FROM shift WHERE closedAt IS NULL LIMIT 1")
    suspend fun current(): Shift?

    @Query("SELECT * FROM shift WHERE closedAt IS NULL LIMIT 1")
    fun observeCurrent(): Flow<Shift?>

    @Query("SELECT * FROM shift WHERE id = :id")
    suspend fun byId(id: Long): Shift?

    @Query("SELECT * FROM shift ORDER BY openedAt DESC")
    fun observeAll(): Flow<List<Shift>>
}

@Dao
interface UserDao {
    @Insert suspend fun insert(u: User): Long
    @Update suspend fun update(u: User)

    @Query("SELECT * FROM user WHERE active = 1 ORDER BY role, displayName")
    fun observeActive(): Flow<List<User>>

    @Query("SELECT * FROM user ORDER BY role, displayName")
    fun observeAll(): Flow<List<User>>

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun byId(id: Long): User?

    @Query("SELECT COUNT(*) FROM user") suspend fun count(): Int
}

@Dao
interface AuditDao {
    @Insert suspend fun insert(log: AuditLog): Long

    @Query("SELECT * FROM audit_log ORDER BY at DESC LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<AuditLog>

    @Query("SELECT * FROM audit_log ORDER BY at DESC LIMIT 200")
    fun observeRecent(): Flow<List<AuditLog>>
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM setting WHERE id = 1")
    suspend fun get(): SettingRow?

    @Query("SELECT * FROM setting WHERE id = 1")
    fun observe(): Flow<SettingRow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: SettingRow)
}
