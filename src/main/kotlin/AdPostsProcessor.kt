import facebook4j.Facebook
import mu.KLogger
import pl.bnowakowski.facebook_commenter.FacebookPost
import java.io.File

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

        val uri = Thread.currentThread().getContextClassLoader().getResource("adPosts.txt")?.toURI()
        val adPostIds = File (uri).useLines { it.toList() }

        logger.info("will be processing ${adPostIds.size} ad posts:")

        for (adPostId in adPostIds) {
            val post = facebook.getPost(adPostId)
            logger.info("in ${adPostsCounter}/${adPostIds.size} ad post [${FacebookPost.previewMessage(post)}...], ${post.id}")

            // comments under ad posts via API
            if (facebook4jProperties.getProperty("enabled") == "true") {
                logger.info("\tlooking into comments under post")
                facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(
                    facebook.getPost(adPostId)
                )
            }

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