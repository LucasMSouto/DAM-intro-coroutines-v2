package tasks

import contributors.MockGithubService
import contributors.progressResults
import contributors.testRequestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.currentTime
import org.junit.Assert
import org.junit.Test

//class Request6ProgressKtTest {
//    @Test
//    fun testProgress() = runBlocking {
//        val startTime = System.currentTimeMillis()
//        var index = 0
//        loadContributorsProgress(MockGithubService, testRequestData) {
//            users, _ ->
//            val expected = progressResults[index++]
//            val time = System.currentTimeMillis() - startTime
//            /*
//            // TODO: uncomment this assertion
//            Assert.assertEquals("Expected intermediate result after virtual ${expected.timeFromStart} ms:",
//                expected.timeFromStart, time)
//            */
//            Assert.assertEquals("Wrong intermediate result after $time:", expected.users, users)
//        }
//    }
//}

@OptIn(ExperimentalCoroutinesApi::class)
class Request6ProgressKtTest {
    @Test
    fun testProgress() = runTest {
        val startTime = currentTime
        var index = 0
        loadContributorsProgress(MockGithubService, testRequestData) { users, _ ->
            val expected = progressResults[index++]
            val time = currentTime - startTime
            Assert.assertEquals("Expected intermediate result after virtual ${expected.timeFromStart} ms:", expected.timeFromStart, time)
            Assert.assertEquals("Wrong intermediate result after $time:", expected.users, users)
        }
    }
}