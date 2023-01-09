import facebook4j.*
import mu.KotlinLogging
import java.net.URL
import java.util.*


fun main(args: Array<String>) {

    val facebook: Facebook = FacebookFactory().getInstance()
    val facebookReplies: FacebookReplies = FacebookReplies(facebook)
    val logger = KotlinLogging.logger {}

    val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba
    logger.info("got ${posts.size} posts")

    // comments
    for (post in posts) {
        facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(post)
    }

    logger.info("added comment to ${facebookReplies.commentedPosts.toString()} comments")

    // shared posts
//    for (post in posts) {
//        val sharedPosts = facebook.getSharedPosts(post.id)
//        println("")
//    }
}