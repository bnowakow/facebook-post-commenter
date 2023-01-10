import facebook4j.*
import mu.KotlinLogging
import java.net.URL
import java.util.*


fun main(args: Array<String>) {

    val facebook: Facebook = FacebookFactory().getInstance()
    val facebookReplies: FacebookReplies = FacebookReplies(facebook)
    val logger = KotlinLogging.logger {}

    facebook.extendTokenExpiration()

    //debug
//    val facebookAuthorization: FacebookAuthorization = FacebookAuthorization()
//    facebookAuthorization.convertShortLivingTokenToLongLivingOne()
    // \debug


    val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba
    logger.info("got ${posts.size} posts")

    // comments
    for (post in posts) {
        facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(post)
    }

    // TODO figure out can we get id's of ad posts from API
    val adPosts: List<String> = listOf(
        "pfbid02Qnx3ctSvN2Z2JJDEzp25kcdsLgNVSNtHV1bF57psQTR5zWHY6NgEExRnSxMBw6A9l"
    )
    for (adPost in adPosts) {
        facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(facebook.getPost("105161449087504_" + adPost))
    }

    logger.info("added comment to ${facebookReplies.commentedPosts.toString()} comments")

    // shared posts
//    for (post in posts) {
//        val sharedPosts = facebook.getSharedPosts(post.id)
//        println("")
//    }
}