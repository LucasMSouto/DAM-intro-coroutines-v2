package tasks

import contributors.MockGithubService
import contributors.expectedConcurrentResults
import contributors.testRequestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.currentTime
import org.junit.Assert
import org.junit.Test

//class Request5ConcurrentKtTest {
//    @Test
//    fun testConcurrent() = runBlocking {
//        val startTime = System.currentTimeMillis()
//        val result = loadContributorsConcurrent(MockGithubService, testRequestData)
//        Assert.assertEquals("Wrong result for 'loadContributorsConcurrent'", expectedConcurrentResults.users, result)
//        val totalTime = System.currentTimeMillis() - startTime
//        /*
//        // TODO: uncomment this assertion
//        Assert.assertEquals(
//            "The calls run concurrently, so the total virtual time should be 2200 ms: " +
//                    "1000 ms for repos request plus max(1000, 1200, 800) = 1200 ms for concurrent contributors requests)",
//            expectedConcurrentResults.timeFromStart, totalTime
//        )
//        */
//        Assert.assertTrue(
//            "The calls run concurrently, so the total virtual time should be 2200 ms: " +
//                    "1000 ms for repos request plus max(1000, 1200, 800) = 1200 ms for concurrent contributors requests)",
//            totalTime in expectedConcurrentResults.timeFromStart..(expectedConcurrentResults.timeFromStart + 500)
//        )
//    }
//}

@OptIn(ExperimentalCoroutinesApi::class)
class Request5ConcurrentKtTest {
    @Test
    fun testConcurrent() = runTest {
        val startTime = currentTime
        val result = loadContributorsConcurrent(MockGithubService, testRequestData)
        Assert.assertEquals("Wrong result for 'loadContributorsConcurrent'", expectedConcurrentResults.users, result)
        val totalTime = currentTime - startTime

        Assert.assertEquals(
            "The calls run concurrently, so the total virtual time should be 2200 ms: " +
                    "1000 for repos request plus max(1000, 1200, 800) = 1200 for concurrent contributors requests)",
            expectedConcurrentResults.timeFromStart, totalTime
        )
    }
}