import com.restfb.Connection
import com.restfb.FacebookClient
import mu.KLogger
import kotlin.math.min

class FpPostsProcessor (private val logger: KLogger,
                        private val facebookProperties: FacebookProperties,
                        private val facebook4jProperties: Facebook4jProperties,
                        private val facebookReplies: FacebookReplies,
                        private val restfbClient: FacebookClient,
                        private val facebookSharedPosts: FacebookSharedPosts?) {

    private fun fetchAllPostsFromFanpage(fanpageId: String): ArrayList<com.restfb.types.Post> {
        var postConnection: Connection<com.restfb.types.Post> = restfbClient.fetchConnection(
            "$fanpageId/feed", com.restfb.types.Post::class.java,
        )

        val allPosts: ArrayList<com.restfb.types.Post> = ArrayList()
        while (true) {
            for (postList in postConnection) {
                allPosts.addAll(postList)
            }
            if (postConnection.nextPageUrl != null) {
                postConnection = restfbClient.fetchConnectionPage(postConnection.nextPageUrl, com.restfb.types.Post::class.java)
            } else {
                break
            }
        }

        return allPosts
    }

    fun processFpPost() {
        /************************
         * Fanpage Posts
         ***********************/

        val posts: ArrayList<com.restfb.types.Post> = fetchAllPostsFromFanpage("105161449087504")
        logger.info("will be processing ${posts.size} fan page posts:")

        var fpPostsCounter = 1
        for (post in posts) {
//            // TODO debug remove afterwards
//            if (fpPostsCounter == 2 || fpPostsCounter > 3) {
//                fpPostsCounter++
//                continue
//            }
            logger.info("in ${fpPostsCounter}/${posts.size} post [${post.message?.substring(0, min(post.message.length, 30))}...], ${post.id}")

            // comments under posts via API
            // TODO this will fail with property absent in file
            if (facebook4jProperties.getProperty("enabled") == "true") {
                logger.info("\tlooking into comments under post")
                facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(post.id)
            }

            // shared posts via API
            //  val sharedPosts = facebook.getSharedPosts(post.id) // API

            // shared posts using workaround
            // TODO checking if value is not null is not elegant (it's done because when object is created browser window is spawned which is not necessary when only API is used)
            if (facebookProperties.getProperty("workaround-enabled") == "true" &&
                facebookSharedPosts !== null
            ) {
                logger.info("\tlooking into shared posts of fanpage posts using workaround")
                facebookSharedPosts.openSharedPosts(post.id)
            }
            fpPostsCounter++
        }
    }

    fun printCommentsSummary() {
        if (facebook4jProperties.getProperty("enabled") == "true") {
            logger.info("added comment to ${facebookReplies.commentedPosts} comments via API")
        }
        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
            facebookSharedPosts !== null
        ) {
            logger.info("added comment to ${facebookSharedPosts.commentedPosts} comments via shared posts workaround")
        }
        if ((facebook4jProperties.getProperty("enabled") == "true") || (facebookProperties.getProperty("workaround-enabled") == "true" &&
                    facebookSharedPosts !== null)
        ) {
            logger.info("fail to add comment to ${facebookReplies.commentsToBeReTried.size} post/comments")
        }
    }
}