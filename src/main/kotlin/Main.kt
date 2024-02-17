import facebook4j.*
import mu.KotlinLogging
import kotlin.system.exitProcess


fun main() {

    val facebook: Facebook = FacebookFactory().instance
    val facebook4jProperties = Facebook4jProperties()
    val restfbClient = com.restfb.DefaultFacebookClient(facebook4jProperties.getProperty("oauth.accessToken"),
        facebook4jProperties.getProperty("oauth.appSecret"), com.restfb.Version.VERSION_19_0)
    val facebookReplies = FacebookReplies(facebook, restfbClient)
    val logger = KotlinLogging.logger {}
    val facebookProperties = FacebookProperties()
    val fpPostsProcessor = FpPostsProcessor()
    val adPostsProcessor = AdPostsProcessor()

    // TODO automate inviting people liking page to join grou
    // TODO find notification with people sharing fanpage (orange flag icon in notification) and comment it

    facebook.extendTokenExpiration()

    adPostsProcessor.processAdPost(logger, facebook, facebook4jProperties, facebookReplies)
    fpPostsProcessor.processFpPost(logger, facebookProperties, facebook4jProperties, facebookReplies, restfbClient)
    exitProcess(0)

}
