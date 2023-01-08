import facebook4j.*
import java.util.*


fun main(args: Array<String>) {

    val facebook: Facebook = FacebookFactory().getInstance()
    val facebookReplies: FacebookReplies = FacebookReplies(facebook)

    val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba
    println("got ${posts.size} posts")

    val commentedPosts: Int = 0

    // comments
    for (post in posts) {
        val comments: ResponseList<Comment> = facebook.getPostComments(post.id)
        println("in post [${post.message?.substring(0, 30)}...] got ${comments.size} comments")

        for (comment in comments) {

            if (!facebookReplies.isCommentWrittenByOneOfAdmins(comment)) {
                if (!facebookReplies.isCommentRepliedByOneOfAdmins(comment)) {
                    println("comment ${comment} under post ${post.id}");

                    facebook.likePost(comment.id)
                    val replyMessage: String = facebookReplies.randomizeThankYouReply()
                    println("\ttrying replying with '${replyMessage}'")
                    facebook.commentPost(comment.id, replyMessage)
                    commentedPosts.inc()

                    val numberOfSeconds: Long = (10..120).random().toLong()
                    println("sleeping for ${numberOfSeconds} seconds\n")
                    Thread.sleep(1000 * numberOfSeconds)
                }
            }
        }
    }

    println("added comment to ${commentedPosts.toString()} comments")

    // shared posts
//    for (post in posts) {
//        val sharedPosts = facebook.getSharedPosts(post.id)
//        println("")
//    }
}