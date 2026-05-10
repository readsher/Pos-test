package com.postest.app.data.repo

import com.postest.app.data.db.ShiftDao
import com.postest.app.data.entity.PaymentMethod
import com.postest.app.data.entity.Shift
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class ShiftSummary(
    val shift: Shift,
    val totalSalesCents: Long,
    val txCount: Int,
    val byMethod: Map<PaymentMethod, Long>,
    val byCategory: Map<String, Long>,
    val voidCount: Int,
    val expectedCashCents: Long,
    val overShortCents: Long?, // null until closed
)

@Singleton
class ShiftRepo @Inject constructor(
    private val dao: ShiftDao,
    private val orderRepo: OrderRepo,
    private val catalogRepo: CatalogRepo,
) {
    fun observeCurrent(): Flow<Shift?> = dao.observeCurrent()
    suspend fun current(): Shift? = dao.current()
    fun observeAll(): Flow<List<Shift>> = dao.observeAll()
    suspend fun byId(id: Long): Shift? = dao.byId(id)

    suspend fun open(openingCashCents: Long, userId: Long): Shift {
        check(dao.current() == null) { "A shift is already open" }
        val id = dao.insert(Shift(
            openedAt = System.currentTimeMillis(),
            openingCashCents = openingCashCents,
            openedByUserId = userId,
        ))
        return dao.byId(id)!!
    }

    suspend fun close(countedCashCents: Long, userId: Long, note: String? = null): Shift {
        val cur = dao.current() ?: error("No open shift")
        val updated = cur.copy(
            closedAt = System.currentTimeMillis(),
            closingCashCountedCents = countedCashCents,
            closedByUserId = userId,
            note = note,
        )
        dao.update(updated)
        return updated
    }

    suspend fun summarize(shift: Shift): ShiftSummary {
        val orders = orderRepo.ordersForShift(shift.id)
        val payments = orderRepo.paymentsForShift(shift.id)
        val totalSales = payments.sumOf { it.amountCents }
        val byMethod = PaymentMethod.entries.associateWith { m ->
            payments.filter { it.method == m }.sumOf { it.amountCents }
        }
        // by category
        val cats = catalogRepo.let { repo ->
            // we don't expose category list lookup directly here for simplicity; build from items
            val itemsAll = orders.flatMap { orderRepo.listItems(it.id) }
            val productIds = itemsAll.map { it.productId }.distinct()
            val products = productIds.mapNotNull { repo.productById(it) }.associateBy { it.id }
            itemsAll.groupBy { products[it.productId]?.categoryId ?: -1L }
                .mapValues { (_, items) -> items.sumOf { it.unitPriceCents * it.qty } }
                .mapKeys { (catId, _) -> "cat#$catId" }
        }
        val voidCount = orders.count { it.status.name == "VOID" }
        val cashSales = byMethod[PaymentMethod.CASH] ?: 0L
        val expectedCash = shift.openingCashCents + cashSales
        val overShort = shift.closingCashCountedCents?.let { it - expectedCash }
        return ShiftSummary(
            shift = shift,
            totalSalesCents = totalSales,
            txCount = payments.size,
            byMethod = byMethod,
            byCategory = cats,
            voidCount = voidCount,
            expectedCashCents = expectedCash,
            overShortCents = overShort,
        )
    }
}
