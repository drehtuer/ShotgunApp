package de.drehtuer.shotgun.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * A DataStore that belongs to one test and nothing else.
 *
 * Two things make this necessary, and both were found the hard way.
 *
 * `preferencesDataStore` caches one instance **per process**, in the delegate
 * rather than per context, so every test in the JVM otherwise shares one store
 * and one internal scope. Robolectric hands each test a fresh filesystem, but
 * the cached store keeps serving what the previous test put in it: a probe that
 * wrote `haptics = false` in one test read it back in the next, which is not a
 * thing a fresh install could ever do.
 *
 * The scope is the second half. The process-wide store runs on
 * `Dispatchers.IO`, so its work lands on real threads while `runTest` is
 * driving a virtual clock - a resumption crossing between the two is what a
 * test that hung for a full minute in CI looked like. Here the store runs on
 * the test's own dispatcher, so the writes and the reads are on the same clock
 * as the assertions waiting for them.
 *
 * Call [close] from `@After`. The file is unique per store, so two stores can
 * never meet even if one outlives its test.
 */
class IsolatedSettingsStore(dispatcher: CoroutineDispatcher, directory: File) {

    private val scope = CoroutineScope(dispatcher + Job())

    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        // Not created here: DataStore wants to create the file itself, and
        // hands back a corruption error if it finds an empty one waiting.
        produceFile = { File(directory, "settings-${counter.incrementAndGet()}.preferences_pb") },
    )

    /** The repository under test, wired to this store. */
    fun repository() = SettingsRepository(dataStore)

    fun close() = scope.cancel()

    private companion object {
        /** Unique per store, so no two share a path within a JVM. */
        val counter = AtomicLong()
    }
}
