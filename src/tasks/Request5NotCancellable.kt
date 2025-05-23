package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.async

suspend fun loadContributorsNotCancellable(
    service: GitHubService,
    req: RequestData
): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
        // GlobalScope launches coroutines in a scope that cannot be
        // cancelled from the outside. This is not a good practice!
        GlobalScope.async(Dispatchers.Default) {
            log("Loading contributors for ${repo.name}")
//            delay(3000) // Simulate a long-running task
            service
                .getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }
    return deferreds.awaitAll().flatten().aggregate()
}