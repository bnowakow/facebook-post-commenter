import facebook4j.*
import mu.KotlinLogging
import pl.bnowakowski.facebook_commenter.FacebookPost
import kotlin.system.exitProcess


fun main() {

    val facebook: Facebook = FacebookFactory().instance
    val facebookReplies = FacebookReplies(facebook)
    val logger = KotlinLogging.logger {}
    val facebookProperties = FacebookProperties()
    val facebook4jProperties = Facebook4jProperties()
    val fpPostsProcessor = FpPostsProcessor()
    val adPostsProcessor = AdPostsProcessor()

    // TODO automate inviting people liking page to join grou
    // TODO find notification with people sharing fanpage (orange flag icon in notification) and comment it

    facebook.extendTokenExpiration()

    adPostsProcessor.processAdPost(logger, facebook, facebook4jProperties, facebookReplies)
    fpPostsProcessor.processFpPost(logger, facebook, facebookProperties, facebook4jProperties, facebookReplies)
    exitProcess(0)

}
