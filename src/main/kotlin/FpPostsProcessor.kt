import facebook4j.Facebook
import facebook4j.Post
import facebook4j.ResponseList
import mu.KLogger
import pl.bnowakowski.facebook_commenter.FacebookPost

class FpPostsProcessor {

    fun processFpPost(
        logger: KLogger,
        facebook: Facebook,
        facebookProperties: FacebookProperties,
        facebook4jProperties: Facebook4jProperties,
        facebookReplies: FacebookReplies
    ) {
        /************************
         * Fanpage Posts
         ***********************/

        val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba
        logger.info("will be processing ${posts.size} fan page posts:")

        var facebookSharedPosts: FacebookSharedPosts? = null
        if (facebookProperties.getProperty("workaround-enabled") == "true") {
            facebookSharedPosts = FacebookSharedPosts()
            facebookSharedPosts.loginToFacebook()
            facebookSharedPosts.inviteToLikeFanpagePeopleWhoInteractedWithPosts()
            facebookSharedPosts.switchProfileToFanPage()
        }

        var fpPostsCounter = 1
        for (post in posts) {
            logger.info("in ${fpPostsCounter}/${posts.size} post [${FacebookPost.previewMessage(post)}...], ${post.id}")

            // comments under posts via API
            // TODO this will fail with property absent in file
            if (facebook4jProperties.getProperty("enabled") == "true") {
                logger.info("\tlooking into comments under post")
                facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(post)
            }

            // shared posts via API
            //  val sharedPosts = facebook.getSharedPosts(post.id) // API

            // shared posts using workaround
            // TODO checking if value is not null is not elegant (it's done because when object is created browser window is spawned which is not necessary when only API is used)
            if (facebookProperties.getProperty("workaround-enabled") == "true" &&
                facebookSharedPosts !== null
            ) {
                logger.info("\tlooking into shared posts using workaround")
                facebookSharedPosts.openSharedPosts(post.id)
            }
            fpPostsCounter++
        }

        if (facebook4jProperties.getProperty("enabled") == "true") {
            logger.info("added comment to ${facebookReplies.commentedPosts} comments via API")
        }
        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
            facebookSharedPosts !== null
        ) {
            logger.info("added comment to ${facebookSharedPosts.commentedPosts} comments via shared posts workaround")
        }
    }
}