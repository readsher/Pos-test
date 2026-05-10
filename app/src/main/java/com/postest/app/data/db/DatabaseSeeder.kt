package com.postest.app.data.db

import com.postest.app.auth.PinHasher
import com.postest.app.data.entity.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val db: AppDatabase,
    private val pinHasher: PinHasher,
) {
    suspend fun seedIfEmpty() {
        if (db.userDao().count() == 0) {
            val (hash, salt) = pinHasher.hash("0000")
            db.userDao().insert(
                User(displayName = "Owner", pinHash = hash, pinSalt = salt, role = UserRole.OWNER)
            )
        }

        if (db.settingDao().get() == null) {
            db.settingDao().upsert(SettingRow())
        }

        if (db.categoryDao().count() == 0) {
            val drinks = db.categoryDao().insert(
                Category(nameEn = "Drinks", nameTh = "เครื่องดื่ม", sortOrder = 1, color = 0xFF3282B8.toInt())
            )
            val food = db.categoryDao().insert(
                Category(nameEn = "Food", nameTh = "อาหาร", sortOrder = 2, color = 0xFFE76F51.toInt())
            )
            val dessert = db.categoryDao().insert(
                Category(nameEn = "Dessert", nameTh = "ของหวาน", sortOrder = 3, color = 0xFFF4A261.toInt())
            )

            val productDao = db.productDao()
            productDao.insert(Product(categoryId = drinks, nameEn = "Iced Coffee",  nameTh = "กาแฟเย็น",       priceCents = 5500, kitchenPrint = false))
            productDao.insert(Product(categoryId = drinks, nameEn = "Iced Tea",     nameTh = "ชาเย็น",          priceCents = 4500, kitchenPrint = false))
            productDao.insert(Product(categoryId = drinks, nameEn = "Water",        nameTh = "น้ำเปล่า",         priceCents = 2000, kitchenPrint = false))
            productDao.insert(Product(categoryId = food,   nameEn = "Pad Thai",     nameTh = "ผัดไทย",          priceCents = 8000, kitchenPrint = true))
            productDao.insert(Product(categoryId = food,   nameEn = "Fried Rice",   nameTh = "ข้าวผัด",          priceCents = 7500, kitchenPrint = true))
            productDao.insert(Product(categoryId = food,   nameEn = "Tom Yum",      nameTh = "ต้มยำ",            priceCents = 12000, kitchenPrint = true))
            productDao.insert(Product(categoryId = food,   nameEn = "Green Curry",  nameTh = "แกงเขียวหวาน",     priceCents = 9000, kitchenPrint = true))
            productDao.insert(Product(categoryId = dessert,nameEn = "Mango Sticky Rice", nameTh = "ข้าวเหนียวมะม่วง", priceCents = 8500, kitchenPrint = true))
            productDao.insert(Product(categoryId = dessert,nameEn = "Coconut Ice Cream", nameTh = "ไอศกรีมกะทิ",   priceCents = 5500, kitchenPrint = false))
        }

        if (db.tableDao().count() == 0) {
            val tableDao = db.tableDao()
            for (i in 1..6) {
                tableDao.insert(OrderTable(label = "T$i"))
            }
        }
    }
}
