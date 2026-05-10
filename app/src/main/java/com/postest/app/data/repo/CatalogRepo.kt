package com.postest.app.data.repo

import com.postest.app.data.db.CategoryDao
import com.postest.app.data.db.ProductDao
import com.postest.app.data.db.TableDao
import com.postest.app.data.entity.Category
import com.postest.app.data.entity.OrderTable
import com.postest.app.data.entity.Product
import com.postest.app.data.entity.TableStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepo @Inject constructor(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val tableDao: TableDao,
) {
    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()
    fun observeProductsIn(catId: Long): Flow<List<Product>> = productDao.observeByCategory(catId)
    fun observeAllProducts(): Flow<List<Product>> = productDao.observeAll()
    fun observeTables(): Flow<List<OrderTable>> = tableDao.observeAll()

    suspend fun addCategory(c: Category): Long = categoryDao.insert(c)
    suspend fun updateCategory(c: Category) = categoryDao.update(c)
    suspend fun deleteCategory(c: Category) = categoryDao.delete(c)

    suspend fun addProduct(p: Product): Long = productDao.insert(p)
    suspend fun updateProduct(p: Product) = productDao.update(p)
    suspend fun deleteProduct(p: Product) = productDao.delete(p)
    suspend fun productById(id: Long): Product? = productDao.byId(id)

    suspend fun addTable(t: OrderTable): Long = tableDao.insert(t)
    suspend fun updateTable(t: OrderTable) = tableDao.update(t)
    suspend fun deleteTable(t: OrderTable) = tableDao.delete(t)
    suspend fun tableById(id: Long): OrderTable? = tableDao.byId(id)

    suspend fun setTableOrder(tableId: Long, orderId: Long?) {
        val t = tableDao.byId(tableId) ?: return
        tableDao.update(t.copy(
            currentOrderId = orderId,
            status = if (orderId == null) TableStatus.FREE else TableStatus.OCCUPIED,
        ))
    }
}
