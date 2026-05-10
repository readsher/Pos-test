package com.postest.app.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.postest.app.data.entity.Category
import com.postest.app.data.entity.Product
import com.postest.app.data.repo.CatalogRepo
import com.postest.app.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuAdminViewModel @Inject constructor(
    private val repo: CatalogRepo,
) : ViewModel() {
    val categories: StateFlow<List<Category>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val products: StateFlow<List<Product>> = repo.observeAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveCategory(c: Category) = viewModelScope.launch { if (c.id == 0L) repo.addCategory(c) else repo.updateCategory(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCategory(c) }
    fun saveProduct(p: Product) = viewModelScope.launch { if (p.id == 0L) repo.addProduct(p) else repo.updateProduct(p) }
    fun deleteProduct(p: Product) = viewModelScope.launch { repo.deleteProduct(p) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuAdminScreen(nav: NavController, vm: MenuAdminViewModel = hiltViewModel()) {
    val cats by vm.categories.collectAsState()
    val prods by vm.products.collectAsState()
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Menu") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Row(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Categories", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { editingCategory = Category(nameEn = "", nameTh = "") }) { Text("New") }
                }
                LazyColumn {
                    items(cats) { c ->
                        ListItem(
                            headlineContent = { Text(c.nameEn) },
                            supportingContent = { Text(c.nameTh) },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = { editingCategory = c }) { Text("Edit") }
                                    TextButton(onClick = { vm.deleteCategory(c) }) { Text("Delete") }
                                }
                            },
                        )
                    }
                }
            }
            VerticalDivider()
            Column(Modifier.weight(2f).padding(start = 12.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Products", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Button(
                        enabled = cats.isNotEmpty(),
                        onClick = { editingProduct = Product(categoryId = cats.first().id, nameEn = "", nameTh = "", priceCents = 0) },
                    ) { Text("New") }
                }
                LazyColumn {
                    items(prods) { p ->
                        ListItem(
                            headlineContent = { Text(p.nameEn) },
                            supportingContent = { Text("${p.nameTh}  ·  ${Money.fmt(p.priceCents)}${if (p.kitchenPrint) "  ·  kitchen" else ""}") },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = { editingProduct = p }) { Text("Edit") }
                                    TextButton(onClick = { vm.deleteProduct(p) }) { Text("Delete") }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (editingCategory != null) {
        CategoryEditDialog(editingCategory!!, onSave = { vm.saveCategory(it); editingCategory = null }, onDismiss = { editingCategory = null })
    }
    if (editingProduct != null) {
        ProductEditDialog(editingProduct!!, cats, onSave = { vm.saveProduct(it); editingProduct = null }, onDismiss = { editingProduct = null })
    }
}

@Composable
private fun CategoryEditDialog(c: Category, onSave: (Category) -> Unit, onDismiss: () -> Unit) {
    var en by remember { mutableStateOf(c.nameEn) }
    var th by remember { mutableStateOf(c.nameTh) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(c.copy(nameEn = en, nameTh = th)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (c.id == 0L) "New category" else "Edit category") },
        text = {
            Column {
                OutlinedTextField(value = en, onValueChange = { en = it }, label = { Text("Name (English)") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = th, onValueChange = { th = it }, label = { Text("Name (ไทย)") })
            }
        },
    )
}

@Composable
private fun ProductEditDialog(p: Product, cats: List<Category>, onSave: (Product) -> Unit, onDismiss: () -> Unit) {
    var en by remember { mutableStateOf(p.nameEn) }
    var th by remember { mutableStateOf(p.nameTh) }
    var priceText by remember { mutableStateOf((p.priceCents / 100.0).toString()) }
    var catId by remember { mutableStateOf(p.categoryId) }
    var kitchen by remember { mutableStateOf(p.kitchenPrint) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val cents = ((priceText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                onSave(p.copy(nameEn = en, nameTh = th, priceCents = cents, categoryId = catId, kitchenPrint = kitchen))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (p.id == 0L) "New product" else "Edit product") },
        text = {
            Column {
                OutlinedTextField(value = en, onValueChange = { en = it }, label = { Text("Name (English)") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = th, onValueChange = { th = it }, label = { Text("Name (ไทย)") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText, onValueChange = { priceText = it },
                    label = { Text("Price (THB)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(8.dp))
                Text("Category:")
                cats.forEach { c ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = catId == c.id, onClick = { catId = c.id })
                        Text(c.nameEn)
                    }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = kitchen, onCheckedChange = { kitchen = it })
                    Text("Print to kitchen")
                }
            }
        },
    )
}
