package com.postest.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PosMode { RETAIL, RESTAURANT }
enum class OrderStatus { OPEN, PAID, VOID }
enum class TableStatus { FREE, OCCUPIED }
enum class PaymentMethod { CASH, PROMPTPAY }
enum class UserRole { OWNER, MANAGER, CASHIER }

@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameTh: String,
    val sortOrder: Int = 0,
    val color: Int = 0xFF3282B8.toInt(),
    val active: Boolean = true,
)

@Entity(
    tableName = "product",
    foreignKeys = [ForeignKey(
        entity = Category::class, parentColumns = ["id"],
        childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("categoryId")],
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val nameEn: String,
    val nameTh: String,
    val priceCents: Long,
    val sku: String? = null,
    val imageUri: String? = null,
    val kitchenPrint: Boolean = false,
    val active: Boolean = true,
)

@Entity(tableName = "order_table")
data class OrderTable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val area: String? = null,
    val status: TableStatus = TableStatus.FREE,
    val currentOrderId: Long? = null,
)

@Entity(
    tableName = "pos_order",
    indices = [Index("shiftId"), Index("userId"), Index("tableId")],
)
data class PosOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: PosMode,
    val tableId: Long? = null,
    val status: OrderStatus = OrderStatus.OPEN,
    val createdAt: Long,
    val closedAt: Long? = null,
    val subtotalCents: Long = 0,
    val taxCents: Long = 0,
    val totalCents: Long = 0,
    val shiftId: Long? = null,
    val userId: Long,
    val voidReason: String? = null,
)

@Entity(
    tableName = "order_item",
    foreignKeys = [ForeignKey(
        entity = PosOrder::class, parentColumns = ["id"],
        childColumns = ["orderId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("orderId"), Index("productId")],
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val nameEn: String,
    val nameTh: String,
    val qty: Int,
    val unitPriceCents: Long,
    val note: String? = null,
    val sentToKitchen: Boolean = false,
    val kitchenPrint: Boolean = false,
)

@Entity(
    tableName = "payment",
    foreignKeys = [ForeignKey(
        entity = PosOrder::class, parentColumns = ["id"],
        childColumns = ["orderId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("orderId"), Index("shiftId")],
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val method: PaymentMethod,
    val amountCents: Long,
    val tenderedCents: Long? = null,
    val paidAt: Long,
    val shiftId: Long? = null,
    val ref: String? = null,
)

@Entity(tableName = "shift")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openedAt: Long,
    val closedAt: Long? = null,
    val openingCashCents: Long,
    val closingCashCountedCents: Long? = null,
    val openedByUserId: Long,
    val closedByUserId: Long? = null,
    val note: String? = null,
)

@Entity(tableName = "user", indices = [Index(value = ["displayName"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val pinHash: String,
    val pinSalt: String,
    val role: UserRole,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "audit_log", indices = [Index("userId"), Index("at")])
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val action: String,
    val targetType: String? = null,
    val targetId: Long? = null,
    val payloadJson: String? = null,
    val approvedByUserId: Long? = null,
    val at: Long = System.currentTimeMillis(),
)

@Entity(tableName = "setting")
data class SettingRow(
    @PrimaryKey val id: Int = 1,
    val mode: PosMode = PosMode.RETAIL,
    val language: String = "en",
    val taxPct: Double = 0.0,
    val promptPayId: String = "",
    val shopName: String = "PosTest Shop",
    val shopAddress: String = "",
    val starPrinterIp: String = "",
    val btPrinterMac: String = "",
    val receiptPrinterTarget: String = "STAR", // STAR | BT | NONE
    val kitchenPrinterTarget: String = "NONE",
    val idleLockMinutes: Int = 5,
    val paperWidthMm: Int = 80,
)
