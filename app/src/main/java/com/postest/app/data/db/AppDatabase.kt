package com.postest.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.postest.app.data.entity.*

class Converters {
    @TypeConverter fun mode(v: PosMode): String = v.name
    @TypeConverter fun toMode(v: String): PosMode = PosMode.valueOf(v)
    @TypeConverter fun status(v: OrderStatus): String = v.name
    @TypeConverter fun toStatus(v: String): OrderStatus = OrderStatus.valueOf(v)
    @TypeConverter fun ts(v: TableStatus): String = v.name
    @TypeConverter fun toTs(v: String): TableStatus = TableStatus.valueOf(v)
    @TypeConverter fun pm(v: PaymentMethod): String = v.name
    @TypeConverter fun toPm(v: String): PaymentMethod = PaymentMethod.valueOf(v)
    @TypeConverter fun role(v: UserRole): String = v.name
    @TypeConverter fun toRole(v: String): UserRole = UserRole.valueOf(v)
}

@Database(
    entities = [
        Category::class,
        Product::class,
        OrderTable::class,
        PosOrder::class,
        OrderItem::class,
        Payment::class,
        Shift::class,
        User::class,
        AuditLog::class,
        SettingRow::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun tableDao(): TableDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun shiftDao(): ShiftDao
    abstract fun userDao(): UserDao
    abstract fun auditDao(): AuditDao
    abstract fun settingDao(): SettingDao
}
