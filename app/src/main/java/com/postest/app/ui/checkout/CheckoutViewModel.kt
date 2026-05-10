package com.postest.app.ui.checkout

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postest.app.auth.SessionManager
import com.postest.app.data.entity.OrderItem
import com.postest.app.data.entity.PaymentMethod
import com.postest.app.data.entity.PosOrder
import com.postest.app.data.entity.SettingRow
import com.postest.app.data.repo.OrderRepo
import com.postest.app.data.repo.SettingsRepo
import com.postest.app.data.repo.ShiftRepo
import com.postest.app.print.PrinterService
import com.postest.app.print.PromptPayQr
import com.postest.app.print.ReceiptData
import com.postest.app.print.ReceiptRenderer
import com.postest.app.print.receiptPrinterTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutState(
    val order: PosOrder? = null,
    val items: List<OrderItem> = emptyList(),
    val settings: SettingRow = SettingRow(),
    val qrBitmap: Bitmap? = null,
    val paid: Boolean = false,
    val printError: String? = null,
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val orderRepo: OrderRepo,
    private val settingsRepo: SettingsRepo,
    private val shiftRepo: ShiftRepo,
    private val printer: PrinterService,
    private val renderer: ReceiptRenderer,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutState())
    val state: StateFlow<CheckoutState> = _state.asStateFlow()

    fun load(orderId: Long) {
        viewModelScope.launch {
            val s = settingsRepo.get()
            orderRepo.applyTax(orderId, s.taxPct)
            val o = orderRepo.byId(orderId)
            val items = orderRepo.listItems(orderId)
            _state.value = _state.value.copy(order = o, items = items, settings = s)
        }
    }

    fun showPromptPayQr() {
        viewModelScope.launch {
            val o = _state.value.order ?: return@launch
            val s = _state.value.settings
            if (s.promptPayId.isBlank()) return@launch
            val payload = PromptPayQr.buildPayload(s.promptPayId, o.totalCents / 100.0)
            val bmp = PromptPayQr.renderBitmap(payload, 512)
            _state.value = _state.value.copy(qrBitmap = bmp)
        }
    }

    fun payCash(tenderedCents: Long, andPrint: Boolean) {
        viewModelScope.launch {
            val o = _state.value.order ?: return@launch
            val shift = shiftRepo.current()
            orderRepo.pay(o.id, PaymentMethod.CASH, o.totalCents, tenderedCents, null, shift?.id)
            _state.value = _state.value.copy(paid = true, order = orderRepo.byId(o.id))
            if (andPrint) doPrintReceipt()
        }
    }

    fun payPromptPay(andPrint: Boolean) {
        viewModelScope.launch {
            val o = _state.value.order ?: return@launch
            val shift = shiftRepo.current()
            orderRepo.pay(o.id, PaymentMethod.PROMPTPAY, o.totalCents, null, null, shift?.id)
            _state.value = _state.value.copy(paid = true, order = orderRepo.byId(o.id))
            if (andPrint) doPrintReceipt()
        }
    }

    fun reprintReceipt() = viewModelScope.launch { doPrintReceipt() }

    private suspend fun doPrintReceipt() {
        val o = _state.value.order ?: return
        val s = _state.value.settings
        val items = orderRepo.listItems(o.id)
        val payments = orderRepo.paymentsForOrder(o.id)
        val cashier = session.current.value?.displayName ?: "—"
        val bmp = renderer.renderReceipt(
            ReceiptData(o, items, payments, cashier),
            settings = s,
            lang = s.language,
            qr = null,
        )
        val target = s.receiptPrinterTarget()
        val r = printer.print(bmp, target)
        if (r.isFailure) {
            _state.value = _state.value.copy(printError = r.exceptionOrNull()?.message)
        }
    }
}
