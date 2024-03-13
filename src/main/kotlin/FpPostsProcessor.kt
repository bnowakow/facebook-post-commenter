import com.restfb.Connection
import com.restfb.FacebookClient
import mu.KLogger
import kotlin.math.min

class FpPostsProcessor (private val logger: KLogger,
                        private val facebookProperties: FacebookProperties,
                        private val facebook4jProperties: Facebook4jProperties,
                        private val facebookReplies: FacebookReplies,
                        private val restfbClient: FacebookClient,
                        public var facebookSharedPosts: FacebookSharedPosts?) {

    var postIds: MutableList<String> = ArrayList()

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

        val posts: ArrayList<com.restfb.types.Post> = fetchAllPostsFromFanpage(facebook4jProperties.getProperty("fanpage.id"))
        logger.info("will be processing ${posts.size} fan page posts:")

        if (facebookProperties.getProperty("developer-mode-enabled") == "true") {
            logger.info("developer-mode is enabled will process subset of all posts")
        }
        var fpPostsCounter = 1
        for (post in posts) {

            if (facebookProperties.getProperty("developer-mode-enabled") == "true") {
                if (fpPostsCounter > 4) {
                    fpPostsCounter++
                    continue
                }
            }
            logger.info("in ${fpPostsCounter}/${posts.size} post [${post.message?.substring(0, min(post.message.length, 30))}...], ${post.id}")

            postIds.add(post.id)

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
                facebookSharedPosts!!.openSharedPosts(post)
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
            logger.info("added comment to ${facebookSharedPosts!!.commentedPosts} comments via shared posts workaround")
        }
        if ((facebook4jProperties.getProperty("enabled") == "true") || (facebookProperties.getProperty("workaround-enabled") == "true" &&
                    facebookSharedPosts !== null)
        ) {
            logger.info("fail to add comment to ${facebookReplies.commentsToBeReTried.size} post/comments")
        }
    }
}