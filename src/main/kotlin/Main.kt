import facebook4j.*
import mu.KotlinLogging
import pl.bnowakowski.facebook_commenter.FacebookPost
import java.net.URL
import java.util.*


fun main(args: Array<String>) {

    val facebook: Facebook = FacebookFactory().getInstance()
    val facebookReplies: FacebookReplies = FacebookReplies(facebook)
    val logger = KotlinLogging.logger {}
    val facebookProperties: FacebookProperties = FacebookProperties()
    val facebook4jProperties: Facebook4jProperties = Facebook4jProperties()

    facebook.extendTokenExpiration()

    val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba
    logger.info("will be processing ${posts.size} fan page posts:")

    var facebookSharedPosts: FacebookSharedPosts? = null
    if (facebookProperties.getProperty("workaround-enabled") == "true") {
        facebookSharedPosts = FacebookSharedPosts()
        facebookSharedPosts.loginToFacebook()
        facebookSharedPosts.switchProfileToFanPage()
    }

    for (post in posts) {
        logger.info("in post [${FacebookPost.Companion.previewMessage(post)}...] ")

        // comments under posts via API
        if (facebook4jProperties.getProperty("enabled") == "true") {
            logger.info("\tlooking into comments under post")
            facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(post)
        }

        // shared posts via API
        //  val sharedPosts = facebook.getSharedPosts(post.id) // API

        // shared posts using workaround
        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
            facebookSharedPosts !== null) {
            logger.info("\tlooking into shared posts using workaround")
            facebookSharedPosts.openSharedPosts(post.id)
        }
    }

    // TODO figure out can we get id's of ad posts from API
    val adPosts: List<String> = listOf(
        "105161449087504_pfbid02Qnx3ctSvN2Z2JJDEzp25kcdsLgNVSNtHV1bF57psQTR5zWHY6NgEExRnSxMBw6A9l",
        "105161449087504_pfbid0gywSSeZKvCFomR5dELyr2ULFpk35SLHAaE5USdiMeyWw4H6bi5yLBVrHnnVN4tuEl"
    )

    logger.info("will be processing ${adPosts.size} ad posts:")

    for (adPost in adPosts) {
        val post = facebook.getPost(adPost)
        logger.info("in post [${FacebookPost.Companion.previewMessage(post)}...] ")

        // comments under ad posts via API
        if (facebook4jProperties.getProperty("enabled") == "true") {
            logger.info("\tlooking into comments under post")
            facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(
                facebook.getPost(adPost)
            )
        }

        // shared ad posts using workaround
        // TODO check how to get shared posts of ads
//        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
//            facebookSharedPosts !== null) {
//            facebookSharedPosts.openSharedPosts(adPost)
//        }
    }

    logger.info("added comment to ${facebookReplies.commentedPosts.toString()} comments")
}