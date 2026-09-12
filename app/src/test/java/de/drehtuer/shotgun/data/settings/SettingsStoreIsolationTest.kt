package de.drehtuer.shotgun.data.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner

/**
 * That one test's settings cannot be seen by the next one.
 *
 * This is a test about the tests, and it earns its place: the app's store is a
 * process-wide singleton held by the `preferencesDataStore` delegate, so before
 * [IsolatedSettingsStore] existed, every test in the JVM shared one. A probe in
 * exactly this shape wrote `haptics = false` in the first method and read it
 * back in the second - across a Robolectric filesystem reset, which no fresh
 * install could ever do. Tests then had to write every value they intended to
 * read, and a test that forgot passed or failed on what its neighbours left
 * behind.
 *
 * Method order is pinned because the point is the *sequence*: `a` writes, `b`
 * must not see it.
 */
@RunWith(RobolectricTestRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SettingsStoreIsolationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: IsolatedSettingsStore

    @Before
    fun setUp() {
        store = IsolatedSettingsStore(UnconfinedTestDispatcher(), tempFolder.newFolder())
    }

    @After
    fun tearDown() = store.close()

    @Test
    fun `a a test writing a setting changes it for itself`() = runTest {
        store.repository().setHaptics(false)
        assertEquals(false, store.repository().settings.first().haptics)
    }

    @Test
    fun `b the next test starts from the defaults, not from what a wrote`() = runTest {
        assertEquals(true, store.repository().settings.first().haptics)
    }
}
