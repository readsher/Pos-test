package com.postest.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postest.app.auth.SessionManager
import com.postest.app.data.entity.*
import com.postest.app.data.repo.CatalogRepo
import com.postest.app.data.repo.OrderRepo
import com.postest.app.data.repo.SettingsRepo
import com.postest.app.data.repo.ShiftRepo
import com.postest.app.print.KitchenTicket
import com.postest.app.print.PrinterService
import com.postest.app.print.ReceiptRenderer
import com.postest.app.print.kitchenPrinterTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosUiState(
    val mode: PosMode = PosMode.RETAIL,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val products: List<Product> = emptyList(),
    val cart: List<OrderItem> = emptyList(),
    val orderId: Long? = null,
    val openShift: Shift? = null,
    val tableId: Long? = null,
    val subtotalCents: Long = 0,
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val catalogRepo: CatalogRepo,
    private val orderRepo: OrderRepo,
    private val shiftRepo: ShiftRepo,
    private val settingsRepo: SettingsRepo,
    private val session: SessionManager,
    private val printer: PrinterService,
    private val renderer: ReceiptRenderer,
) : ViewModel() {

    private val selectedCat = MutableStateFlow<Long?>(null)
    private val orderIdFlow = MutableStateFlow<Long?>(null)
    private val tableIdFlow = MutableStateFlow<Long?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val productsFlow = selectedCat.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else catalogRepo.observeProductsIn(id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val cartFlow = orderIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else orderRepo.observeItems(id)
    }

    val state: StateFlow<PosUiState> = combine(
        settingsRepo.observe(),
        catalogRepo.observeCategories(),
        productsFlow,
        cartFlow,
        shiftRepo.observeCurrent(),
    ) { settings, cats, prods, items, shift ->
        val firstCat = cats.firstOrNull()?.id
        if (selectedCat.value == null && firstCat != null) selectedCat.value = firstCat
        PosUiState(
            mode = settings.mode,
            categories = cats,
            selectedCategoryId = selectedCat.value,
            products = prods,
            cart = items,
            orderId = orderIdFlow.value,
            tableId = tableIdFlow.value,
            openShift = shift,
            subtotalCents = items.sumOf { it.unitPriceCents * it.qty },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PosUiState())

    fun selectCategory(id: Long) { selectedCat.value = id }
    fun setTable(id: Long?) { tableIdFlow.value = id }

    fun resumeOrder(id: Long) { orderIdFlow.value = id }

    private suspend fun ensureOrder(): Long {
        orderIdFlow.value?.let { return it }
        val user = session.current.value ?: error("Not signed in")
        val shift = shiftRepo.current()
        val mode = settingsRepo.get().mode
        val newId = orderRepo.startOrder(mode, user.id, shift?.id, tableIdFlow.value)
        orderIdFlow.value = newId
        return newId
    }

    fun addProduct(p: Product) {
        viewModelScope.launch {
            val orderId = ensureOrder()
            orderRepo.addItem(orderId, p)
        }
    }

    fun changeQty(item: OrderItem, delta: Int) {
        viewModelScope.launch {
            val newQty = (item.qty + delta).coerceAtLeast(0)
            if (newQty == 0) orderRepo.removeItem(item)
            else orderRepo.updateItem(item.copy(qty = newQty))
        }
    }

    fun clearOrder() {
        viewModelScope.launch {
            val id = orderIdFlow.value ?: return@launch
            val items = orderRepo.listItems(id)
            items.forEach { orderRepo.removeItem(it) }
        }
    }

    fun sendToKitchen() {
        viewModelScope.launch {
            val id = orderIdFlow.value ?: return@launch
            val unsent = orderRepo.listUnsentItems(id).filter { it.kitchenPrint }
            if (unsent.isEmpty()) return@launch
            val s = settingsRepo.get()
            val cashier = session.current.value?.displayName ?: "—"
            val table = state.value.tableId?.let { tid -> "T$tid" }
            val bmp = renderer.renderKitchenTicket(
                KitchenTicket(orderId = id, tableLabel = table, items = unsent, cashier = cashier),
                settings = s,
                lang = s.language,
            )
            printer.print(bmp, s.kitchenPrinterTarget())
            orderRepo.markAllSentToKitchen(id)
        }
    }

    fun resetForNewSale() {
        orderIdFlow.value = null
        tableIdFlow.value = null
    }
}
