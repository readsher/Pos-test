package com.postest.app.di

import android.content.Context
import androidx.room.Room
import com.postest.app.data.db.AppDatabase
import com.postest.app.data.db.AuditDao
import com.postest.app.data.db.CategoryDao
import com.postest.app.data.db.OrderDao
import com.postest.app.data.db.PaymentDao
import com.postest.app.data.db.ProductDao
import com.postest.app.data.db.SettingDao
import com.postest.app.data.db.ShiftDao
import com.postest.app.data.db.TableDao
import com.postest.app.data.db.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "postest.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
    @Provides fun provideTableDao(db: AppDatabase): TableDao = db.tableDao()
    @Provides fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()
    @Provides fun providePaymentDao(db: AppDatabase): PaymentDao = db.paymentDao()
    @Provides fun provideShiftDao(db: AppDatabase): ShiftDao = db.shiftDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideAuditDao(db: AppDatabase): AuditDao = db.auditDao()
    @Provides fun provideSettingDao(db: AppDatabase): SettingDao = db.settingDao()
}
