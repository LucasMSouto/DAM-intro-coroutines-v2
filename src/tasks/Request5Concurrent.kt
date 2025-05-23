package tasks

import contributors.*
import kotlinx.coroutines.*

suspend fun loadContributorsConcurrent(
    service: GitHubService,
    req: RequestData
): List<User> = coroutineScope {
    val repos = service
        .getOrgRepos(req.org)
        // .execute() // Executes request and blocks the current thread
        .also { logRepos(req, it) }
        .bodyList()

    val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
        async {
//        async(Dispatchers.Default) {
            log("Loading contributors for ${repo.name}")
//            delay(3000) // Simulate a long-running task
            service
                .getRepoContributors(req.org, repo.name)
                // .execute() // Executes request and blocks the current thread
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }
    deferreds.awaitAll().flatten().aggregate()
}