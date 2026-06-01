package com.oq.barnote.ui.productlist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.productListDataStore by preferencesDataStore(name = "product_list_prefs")

/**
 * ProductList 화면의 뷰 모드 영속화.
 * iOS `@Shared(.appStorage("productListViewMode")) var viewMode: ViewMode = .large` 대응.
 */
@Singleton
class ProductListPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val viewModeKey = stringPreferencesKey("productListViewMode")

    val viewMode: Flow<ProductListViewMode> =
        context.productListDataStore.data.map { prefs ->
            when (prefs[viewModeKey]) {
                "small" -> ProductListViewMode.Small
                else -> ProductListViewMode.Large
            }
        }

    suspend fun readViewMode(): ProductListViewMode = viewMode.first()

    suspend fun setViewMode(value: ProductListViewMode) {
        context.productListDataStore.edit {
            it[viewModeKey] = if (value == ProductListViewMode.Small) "small" else "large"
        }
    }
}
