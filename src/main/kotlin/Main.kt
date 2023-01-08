import facebook4j.*
import java.util.*


fun main(args: Array<String>) {

    val facebook: Facebook = FacebookFactory().getInstance()
    val facebookReplies: FacebookReplies = FacebookReplies(facebook)

    val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba

    for (post in posts) {
        val comments: ResponseList<Comment> = facebook.getPostComments(post.id)

        for (comment in comments) {

            if (!facebookReplies.isCommentWrittenByOneOfAdmins(comment)) {
                if (!facebookReplies.isCommentRepliedByOneOfAdmins(comment)) {
                    println("comment ${comment} under post ${post.id}");

                    facebook.likePost(comment.id)
                    val replyMessage: String = facebookReplies.randomizeThankYouReply()
                    println("\ttrying replying with '${replyMessage}'")
                    facebook.commentPost(comment.id, replyMessage)
                }
            }
        }
    }
}