package contributors

import contributors.Contributors.LoadingStatus.*
import contributors.Variant.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import tasks.*
import java.awt.event.ActionListener
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

enum class Variant {
    BLOCKING,         // Request1Blocking
    BACKGROUND,       // Request2Background
    CALLBACKS,        // Request3Callbacks
    SUSPEND,          // Request4Coroutine
    CONCURRENT,       // Request5Concurrent
    NOT_CANCELLABLE,  // Request6NotCancellable
    PROGRESS,         // Request6Progress
    CHANNELS          // Request7Channels
}

interface Contributors : CoroutineScope {

    val job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    fun init() {
        // Start a new loading on 'load' click
        addLoadListener {
            saveParams()
            loadContributors()
        }

        // Save preferences and exit on closing the window
        addOnWindowClosingListener {
            job.cancel()
            saveParams()
            exitProcess(0)
        }

        // Load stored params (user & password values)
        loadInitialParams()
    }

    fun loadContributors() {
        val (username, password, org, _) = getParams()
        val req = RequestData(username, password, org)

        clearResults()
        val service = createGitHubService(req.username, req.password)

        val startTime = System.currentTimeMillis()
        when (getSelectedVariant()) {
            BLOCKING -> { // Blocking UI thread
                val users = loadContributorsBlocking(service, req)
                updateResults(users, startTime)
            }

            BACKGROUND -> { // Blocking a background thread
                loadContributorsBackground(service, req) { users ->
                    SwingUtilities.invokeLater {
                        updateResults(users, startTime)
                    }
                }
            }

            CALLBACKS -> { // Using callbacks
                loadContributorsCallbacks(service, req) { users ->
                    SwingUtilities.invokeLater {
                        updateResults(users, startTime)
                    }
                }
            }

            SUSPEND -> { // Using coroutines
                launch {
                    val users = loadContributorsSuspend(service, req)
                    updateResults(users, startTime)
                }.setUpCancellation()
            }

            CONCURRENT -> { // Performing requests concurrently
//                launch(Dispatchers.Main) { // Runs the coroutine only on the main thread
                launch(Dispatchers.Default) { // Runs the coroutine on a background thread
                    val users = loadContributorsConcurrent(service, req)
                    withContext(Dispatchers.Main) {
                        updateResults(users, startTime)
                    }
                }.setUpCancellation()
            }

            NOT_CANCELLABLE -> { // Performing requests in a non-cancellable way
                launch {
                    val users = loadContributorsNotCancellable(service, req)
                    updateResults(users, startTime)
                }.setUpCancellation()
            }

            PROGRESS -> { // Showing progress
                launch(Dispatchers.Default) {
                    loadContributorsProgress(service, req) { users, completed ->
                        withContext(Dispatchers.Main) {
                            updateResults(users, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }

            CHANNELS -> {  // Performing requests concurrently and showing progress
//                launch(Dispatchers.Default) {
//                    loadContributorsChannels(service, req) { users, completed ->
//                        withContext(Dispatchers.Main) {
//                            updateResults(users, startTime, completed)
//                        }
//                    }
//                }.setUpCancellation()

                launch(Dispatchers.Default) {
                    val progressChannel = Channel<Pair<List<User>, Boolean>>() // Optional buffer size

                    // Producer coroutine
                    launch(Dispatchers.Default) {
                        loadContributorsChannels(service, req) { users, completed ->
                            progressChannel.send(users to completed)
                        }
                        progressChannel.close()
                    }

                    // Consumer loop
                    for ((users, completed) in progressChannel) {
                        withContext(Dispatchers.Main) {
                            updateResults(users, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }
        }
    }

    enum class LoadingStatus { INIT, COMPLETED, CANCELED, IN_PROGRESS }

    data class LoadingStateData(
        val status: LoadingStatus = INIT,
        val startTime: Long? = null,
        val elapsedTime: String = "",
    )

    val loadingState: StateFlow<LoadingStateData>

    private fun clearResults() {
        updateContributors(listOf())
//        updateLoadingStatus(IN_PROGRESS)
        updateLoadingStatus(LoadingStateData(IN_PROGRESS))
        setActionsStatus(newLoadingEnabled = false)
    }

    private fun calculateElapsedTime(startTime: Long): String {
        val time = System.currentTimeMillis() - startTime
        return "${(time / 1000)}.${time % 1000 / 100} sec"
    }

    fun updateLoadingStatus(newStatus: LoadingStateData)

    private fun updateResults(
        users: List<User>,
        startTime: Long,
        completed: Boolean = true
    ) {
        updateContributors(users)
        val status = if (completed) COMPLETED else IN_PROGRESS
        val elapsedTime = calculateElapsedTime(startTime)
        updateLoadingStatus(LoadingStateData(status, startTime, elapsedTime))
        if (completed) {
            setActionsStatus(newLoadingEnabled = true)
        }
    }

    private fun Job.setUpCancellation() {
        // make active the 'cancel' button
        setActionsStatus(newLoadingEnabled = false, cancellationEnabled = true)

        val loadingJob = this

        // cancel the loading job if the 'cancel' button was clicked
        val listener = ActionListener {
            loadingJob.cancel()
//            updateLoadingStatus(CANCELED)
            updateLoadingStatus(LoadingStateData(CANCELED))
        }
        addCancelListener(listener)

        // update the status and remove the listener after the loading job is completed
        launch {
            loadingJob.join()
            setActionsStatus(newLoadingEnabled = true)
            removeCancelListener(listener)
        }
    }

    fun loadInitialParams() {
        setParams(loadStoredParams())
    }

    fun saveParams() {
        val params = getParams()
        if (params.username.isEmpty() && params.password.isEmpty()) {
            removeStoredParams()
        } else {
            saveParams(params)
        }
    }

    fun getSelectedVariant(): Variant

    fun updateContributors(users: List<User>)

    fun setLoadingStatus(text: String, iconRunning: Boolean)

    fun setActionsStatus(newLoadingEnabled: Boolean, cancellationEnabled: Boolean = false)

    fun addCancelListener(listener: ActionListener)

    fun removeCancelListener(listener: ActionListener)

    fun addLoadListener(listener: () -> Unit)

    fun addOnWindowClosingListener(listener: () -> Unit)

    fun setParams(params: Params)

    fun getParams(): Params
}
