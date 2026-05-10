package com.postest.app.data.repo

import com.postest.app.data.entity.PaymentMethod
import javax.inject.Inject
import javax.inject.Singleton

data class SalesReport(
    val totalCents: Long,
    val txCount: Int,
    val byHour: Map<Int, Long>,
    val byMethod: Map<PaymentMethod, Long>,
    val byCategory: Map<Long, Long>,
    val byUser: Map<Long, Long>,
)

@Singleton
class ReportRepo @Inject constructor(
    private val orderRepo: OrderRepo,
    private val catalogRepo: CatalogRepo,
) {
    suspend fun build(from: Long, to: Long): SalesReport {
        val payments = orderRepo.paymentsBetween(from, to)
        val orders = orderRepo.paidOrdersBetween(from, to).associateBy { it.id }

        val total = payments.sumOf { it.amountCents }
        val byMethod = PaymentMethod.entries.associateWith { m ->
            payments.filter { it.method == m }.sumOf { it.amountCents }
        }
        val byHour = payments.groupBy {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.paidAt }
            cal.get(java.util.Calendar.HOUR_OF_DAY)
        }.mapValues { (_, list) -> list.sumOf { it.amountCents } }

        val byUser = orders.values.groupBy { it.userId }
            .mapValues { (_, list) -> list.sumOf { it.totalCents } }

        // by category — pull items per order
        val byCategory = mutableMapOf<Long, Long>()
        for (o in orders.values) {
            val items = orderRepo.listItems(o.id)
            for (i in items) {
                val p = catalogRepo.productById(i.productId) ?: continue
                byCategory.merge(p.categoryId, i.unitPriceCents * i.qty) { a, b -> a + b }
            }
        }

        return SalesReport(
            totalCents = total,
            txCount = payments.size,
            byHour = byHour,
            byMethod = byMethod,
            byCategory = byCategory,
            byUser = byUser,
        )
    }
}
