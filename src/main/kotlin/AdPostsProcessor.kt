import facebook4j.Facebook
import facebook4j.FacebookException
import mu.KLogger
import pl.bnowakowski.facebook_commenter.FacebookPost

private const val emptyId = ""

class AdPostsProcessor {
    fun processAdPost(
        logger: KLogger,
        facebook: Facebook,
        facebook4jProperties: Facebook4jProperties,
        facebookReplies: FacebookReplies
    ) {
        /************************
         * Ad Posts
         ***********************/

        var adPostsCounter = 1
        // TODO figure out can we get id's of ad posts from API
        // For now manually taken from https://business.facebook.com/latest/inbox/facebook?asset_id=105161449087504&mailbox_id=105161449087504

        if (facebook4jProperties.getProperty("enabled") == "true") {

            // TODO below might be stupid
            val uniqueAdPostIds =
                this::class.java.getResourceAsStream("adPosts.txt").bufferedReader().useLines { it.toList() }
                    .map { longId -> getShortId(facebook, longId) }
                    .filter { shortId -> shortId != emptyId }
                    .toSet()

            logger.info("will be processing ${uniqueAdPostIds.size} ad posts:")

            for (adPostId in uniqueAdPostIds) {
                val post = facebook.getPost(adPostId)
                logger.info("in ${adPostsCounter}/${uniqueAdPostIds.size} ad post [${FacebookPost.previewMessage(post)}...], ${post.id}")

                // comments under ad posts via API

                logger.info("\tlooking into comments under post")
                facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(adPostId!!)


                // shared ad posts using workaround
                // TODO check how to get shared posts of ads
//        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
//            facebookSharedPosts !== null) {
//            facebookSharedPosts.openSharedPosts(adPost)
//        }
                adPostsCounter++
            }
        }
    }
    private fun getShortId(facebook: Facebook, longId: String): String? =
        try {
            facebook.getPost(longId).id
        } catch (e: FacebookException) {
            emptyId
        }
}