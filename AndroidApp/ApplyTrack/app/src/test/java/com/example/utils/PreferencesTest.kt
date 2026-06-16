package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesTest {

    private lateinit var context: Context
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var preferencesHelper: PreferencesHelper

    // --- In-Memory Fake SharedPreferences Implementation ---
    class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()
        private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = map[key] as? Set<String> ?: defValues
        override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map, listeners)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
            listeners.add(listener)
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
            listeners.remove(listener)
        }

        class FakeEditor(
            private val backingMap: MutableMap<String, Any?>,
            private val listeners: Set<SharedPreferences.OnSharedPreferenceChangeListener>
        ) : SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any?>()
            private val keysToRemove = mutableSetOf<String>()
            private var clearCalled = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
                tempMap[key] = values
                return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                keysToRemove.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearCalled = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearCalled) {
                    backingMap.clear()
                }
                keysToRemove.forEach { backingMap.remove(it) }
                backingMap.putAll(tempMap)
            }
        }
    }

    @Before
    fun setUp() {
        context = mock()
        fakePrefs = FakeSharedPreferences()
        whenever(context.getSharedPreferences(eq("applytrack_preferences"), any())).thenReturn(fakePrefs)

        preferencesHelper = PreferencesHelper(context)
    }

    @Test
    fun testDefaultSettings() = runTest {
        assertEquals(AppTheme.SYSTEM, preferencesHelper.getSavedTheme())
        assertFalse(preferencesHelper.isOfflineGuestEnabled())
        assertEquals(AppTheme.SYSTEM, preferencesHelper.themeFlow.value)
        assertFalse(preferencesHelper.isOfflineGuest.value)
        assertTrue(preferencesHelper.isAutoSyncEnabled())
    }

    @Test
    fun testSetAndGetTheme() = runTest {
        preferencesHelper.setTheme(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, preferencesHelper.getSavedTheme())
        assertEquals(AppTheme.LIGHT, preferencesHelper.themeFlow.value)

        preferencesHelper.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, preferencesHelper.getSavedTheme())
        assertEquals(AppTheme.DARK, preferencesHelper.themeFlow.value)
    }

    @Test
    fun testSetAndGetOfflineGuest() = runTest {
        preferencesHelper.setOfflineGuest(true)
        assertTrue(preferencesHelper.isOfflineGuestEnabled())
        assertTrue(preferencesHelper.isOfflineGuest.value)

        preferencesHelper.setOfflineGuest(false)
        assertFalse(preferencesHelper.isOfflineGuestEnabled())
        assertFalse(preferencesHelper.isOfflineGuest.value)
    }

    @Test
    fun testClearAll() = runTest {
        preferencesHelper.setTheme(AppTheme.DARK)
        preferencesHelper.setOfflineGuest(true)

        preferencesHelper.clearAll()

        assertEquals(AppTheme.SYSTEM, preferencesHelper.getSavedTheme())
        assertFalse(preferencesHelper.isOfflineGuestEnabled())
        assertEquals(AppTheme.SYSTEM, preferencesHelper.themeFlow.value)
        assertFalse(preferencesHelper.isOfflineGuest.value)
    }
}
