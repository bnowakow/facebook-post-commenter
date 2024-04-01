import com.restfb.FacebookClient
import facebook4j.Facebook
import facebook4j.FacebookException
import mu.KLogger
import pl.bnowakowski.facebook_commenter.FacebookPost

private const val emptyId = ""

class AdPostsProcessor (private val logger: KLogger,
                        private val facebook: Facebook,
                        private val facebookProperties: FacebookProperties,
                        private val facebook4jProperties: Facebook4jProperties,
                        private val facebookReplies: FacebookReplies,
                        private val restfbClient: FacebookClient,
                        public var facebookSharedPosts: FacebookSharedPosts?) {
    fun processAdPost() {
        /************************
         * Ad Posts
         ***********************/

        var adPostsCounter = 1

        if (facebook4jProperties.getProperty("enabled") == "true") {

            // TODO below might be stupid
            val uniqueAdPostIds =
                this::class.java.getResourceAsStream("adPosts.txt").bufferedReader().useLines { it.toList() }
                    .map { longId -> getShortId(longId) }
                    .filter { shortId -> shortId != emptyId }
                    .toSet()

            logger.info("will be processing ${uniqueAdPostIds.size} ad posts:")

            if (facebookProperties.getProperty("developer-mode-enabled") == "true") {
                logger.info("developer-mode is enabled will process subset of all ad posts")
            }
            for (adPostId in uniqueAdPostIds) {
                if (facebookProperties.getProperty("developer-mode-enabled") == "true") {
                    if (adPostsCounter != 37) {
                        adPostsCounter++
                        continue
                    }
                }

                val post = facebook.getPost(adPostId)
                logger.info("in ${adPostsCounter}/${uniqueAdPostIds.size} ad post [${FacebookPost.previewMessage(post)}...], ${post.id}")

                // comments under ad posts via API

                logger.info("\tlooking into comments under post")
                if (facebook4jProperties.getProperty("api-commenting-enabled") == "true") {
                    facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(adPostId!!)
                }


                // shared ad posts using workaround
                // TODO check how to get shared posts of ads via api
                logger.info("\tlooking into shared posts of ad using workaround")
                if (facebookProperties.getProperty("workaround-enabled") == "true") {
                    if (facebook4jProperties.getProperty("api-commenting-enabled") == "false") {
                        facebookSharedPosts!!.openPostComments(getPost(adPostId!!))
                    }
                    facebookSharedPosts!!.openSharedPosts(getPost(adPostId!!))
                }

                adPostsCounter++
            }
        }
    }

    //  {
// TODO throws, The connection JSON does not contain a data field, maybe it is no connection
    /*
    private fun getPost(postId: String) : com.restfb.types.Post {
        var postConnection: Connection<Post> = restfbClient.fetchConnection(
            "$postId", com.restfb.types.Post::class.java,
            Parameter.with("fields", "id,created_time,message")
        )
        logger.info("break")
//        return allPosts
    }
*/
    fun getPost(id: String): facebook4j.Post {
        return facebook.getPost(id)

    }

    fun getShortId(longId: String): String? =
        try {
            facebook.getPost(longId).id
        } catch (e: FacebookException) {
            emptyId
        }
}