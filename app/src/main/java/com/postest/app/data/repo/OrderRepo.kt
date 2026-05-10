package com.postest.app.data.repo

import com.postest.app.data.db.OrderDao
import com.postest.app.data.db.PaymentDao
import com.postest.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepo @Inject constructor(
    private val orderDao: OrderDao,
    private val paymentDao: PaymentDao,
    private val catalogRepo: CatalogRepo,
) {
    suspend fun byId(id: Long): PosOrder? = orderDao.byId(id)
    fun observe(id: Long): Flow<PosOrder?> = orderDao.observe(id)
    fun observeItems(id: Long): Flow<List<OrderItem>> = orderDao.observeItems(id)
    fun observeOpen(): Flow<List<PosOrder>> = orderDao.observeOpen()

    suspend fun startOrder(
        mode: PosMode,
        userId: Long,
        shiftId: Long?,
        tableId: Long? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val id = orderDao.insertOrder(
            PosOrder(mode = mode, tableId = tableId, createdAt = now, userId = userId, shiftId = shiftId)
        )
        if (tableId != null) catalogRepo.setTableOrder(tableId, id)
        return id
    }

    suspend fun addItem(orderId: Long, product: Product, qty: Int = 1, note: String? = null) {
        orderDao.insertItem(OrderItem(
            orderId = orderId,
            productId = product.id,
            nameEn = product.nameEn,
            nameTh = product.nameTh,
            qty = qty,
            unitPriceCents = product.priceCents,
            note = note,
            kitchenPrint = product.kitchenPrint,
        ))
        recomputeTotals(orderId)
    }

    suspend fun updateItem(item: OrderItem) {
        orderDao.updateItem(item)
        recomputeTotals(item.orderId)
    }

    suspend fun removeItem(item: OrderItem) {
        orderDao.deleteItem(item)
        recomputeTotals(item.orderId)
    }

    suspend fun listItems(orderId: Long): List<OrderItem> = orderDao.listItems(orderId)
    suspend fun listUnsentItems(orderId: Long): List<OrderItem> = orderDao.listUnsentItems(orderId)
    suspend fun markAllSentToKitchen(orderId: Long) = orderDao.markAllSent(orderId)

    suspend fun voidOrder(orderId: Long, reason: String) {
        val o = orderDao.byId(orderId) ?: return
        orderDao.updateOrder(o.copy(status = OrderStatus.VOID, voidReason = reason, closedAt = System.currentTimeMillis()))
        if (o.tableId != null) catalogRepo.setTableOrder(o.tableId, null)
    }

    suspend fun pay(orderId: Long, method: PaymentMethod, amountCents: Long, tenderedCents: Long?, ref: String?, shiftId: Long?) {
        val o = orderDao.byId(orderId) ?: return
        val now = System.currentTimeMillis()
        paymentDao.insert(Payment(
            orderId = orderId, method = method, amountCents = amountCents,
            tenderedCents = tenderedCents, paidAt = now, shiftId = shiftId, ref = ref,
        ))
        orderDao.updateOrder(o.copy(status = OrderStatus.PAID, closedAt = now, shiftId = shiftId ?: o.shiftId))
        if (o.tableId != null) catalogRepo.setTableOrder(o.tableId, null)
    }

    private suspend fun recomputeTotals(orderId: Long) {
        val o = orderDao.byId(orderId) ?: return
        val items = orderDao.listItems(orderId)
        val subtotal = items.sumOf { it.unitPriceCents * it.qty }
        // Tax taken from settings is applied at checkout-time; store subtotal here, total = subtotal until checkout
        orderDao.updateOrder(o.copy(subtotalCents = subtotal, totalCents = subtotal))
    }

    suspend fun applyTax(orderId: Long, taxPct: Double) {
        val o = orderDao.byId(orderId) ?: return
        val tax = (o.subtotalCents * taxPct / 100.0).toLong()
        orderDao.updateOrder(o.copy(taxCents = tax, totalCents = o.subtotalCents + tax))
    }

    suspend fun paymentsForOrder(orderId: Long) = paymentDao.listByOrder(orderId)
    suspend fun paymentsForShift(shiftId: Long) = paymentDao.listByShift(shiftId)
    suspend fun ordersForShift(shiftId: Long) = orderDao.listByShift(shiftId)
    suspend fun paidOrdersBetween(from: Long, to: Long) = orderDao.listPaidBetween(from, to)
    suspend fun paymentsBetween(from: Long, to: Long) = paymentDao.listBetween(from, to)
}
