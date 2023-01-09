import facebook4j.*
import java.net.URL
import java.util.*


fun main(args: Array<String>) {

    val facebook: Facebook = FacebookFactory().getInstance()
    val facebookReplies: FacebookReplies = FacebookReplies(facebook)

    val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba
    println("got ${posts.size} posts")

    // comments
    for (post in posts) {
        facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(post)
    }

    println("added comment to ${facebookReplies.commentedPosts.toString()} comments")

    // shared posts
//    for (post in posts) {
//        val sharedPosts = facebook.getSharedPosts(post.id)
//        println("")
//    }
}