import mu.KotlinLogging
import org.openqa.selenium.*
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import kotlin.NoSuchElementException


class FacebookSharedPosts {

    var driver: WebDriver
    val facebookProperties: FacebookProperties = FacebookProperties()

    private val logger = KotlinLogging.logger {}

    constructor() {
        // Firefox
        // https://www.browserstack.com/docs/automate/selenium/firefox-profile
        val firefoxProfile = FirefoxProfile()
//        firefoxProfile.setPreference("layout.css.devPixelsPerPx", "2.0")

        // https://stackoverflow.com/questions/15397483/how-do-i-set-browser-width-and-height-in-selenium-webdriver
        var firefoxOptions = FirefoxOptions()
        firefoxOptions.addArguments("--width=1000")
        firefoxOptions.addArguments("--height=3440")
        firefoxOptions.setProfile(firefoxProfile)

        driver = FirefoxDriver(firefoxOptions)
        driver.manage().window().position = Point(800, 0)

        driver["https://www.facebook.com"] // TODO debug
//        driver.findElement(By.cssSelector("body")).sendKeys(Keys.chord(Keys.COMMAND, "-"))

        // Chrome
        //driver = ChromeDriver()

        // Safari
        //driver = SafariDriver()
    }

    fun loginToFacebook() {

        val facebookProperties: FacebookProperties = FacebookProperties()

        driver["https://www.facebook.com"]
        driver.findElement(By.id("email")).sendKeys(facebookProperties.getProperty("username"))
        driver.findElement(By.id("pass")).sendKeys(facebookProperties.getProperty("password"))
        Thread.sleep(500)
        // cookie form
        driver.findElement(By.className("_9xo6")).click()
        // login button
        driver.findElement(By.name("login")).click()
        Thread.sleep(5000)
    }

    fun switchProfileToFanPage() {
        // TODO fix notification popup from chrome
        if (facebookProperties.getProperty("username").contains("kuba")) {
            // account icon
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[1]/span/div/div[1]"))
                .click()
            Thread.sleep(500)
            // switch profile to fan page
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div"))
                .click()
        } else {
            // account icon
            driver.findElement(By.xpath("/html/body/div[1]/div[1]/div[1]/div/div[2]/div[4]/div[1]/span/div/div[1]"))
                .click()
            Thread.sleep(500)
            // switch profile
            driver.findElement(By.xpath("/html/body/div[1]/div[1]/div[1]/div/div[2]/div[4]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[2]/div/div[1]/div"))
                .click()
            Thread.sleep(500)
            // switch profile to fan page
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[4]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[2]/div/div[2]/div[1]/div[2]/div"))
                .click()
        }
        Thread.sleep(2000)
    }

    fun openSharedPosts(postId: String) {

        val id = postId.substringAfter("_")
        // TODO get first words of post to log it alongside with id
        logger.info("lookig in shares of ${id} post")
        driver["https://www.facebook.com/shares/view?id=$id"]
        Thread.sleep(5000)

        val js = driver as JavascriptExecutor
        // https://github.com/SeleniumHQ/selenium/issues/4244#issuecomment-371533758
        js.executeScript("document.body.style.MozTransform = \"scale(0.80)\";")
        js.executeScript("document.body.style.MozTransformOrigin = \"0 0\";")

        // scroll down to bottom of page to load all posts (lazy loading)
        var scrollTimeout = 200
//        j = 12 // TODO debug
        var previousScrollHeight: Long = -1
        for (scrollNumber in 1..scrollTimeout) {
            //Scroll down till the bottom of the page
            js.executeScript("window.scrollBy(0,document.body.scrollHeight)")
            Thread.sleep(2000)
            var currnetScrollHeight: Long = js.executeScript("return document.body.scrollHeight") as Long
            if (currnetScrollHeight == previousScrollHeight) {
                logger.info("\treached bottom of the page after ${scrollNumber}th time out of ${scrollTimeout.toString()} tries")
                break
            }
            previousScrollHeight = currnetScrollHeight
            if (scrollNumber % 50 == 0 || scrollNumber == 0) {
                // should be debug but can't set netty to info then
                logger.info("\tscrolling for ${scrollNumber}th time out of ${scrollTimeout.toString()} tries")
            }
        }

        // scroll to the top of page (focus is still at the bottom)
        js.executeScript("window.scrollTo(0, -document.body.scrollHeight)")
        Thread.sleep(1000)

        val beginingOfNextPostLocation = driver.pageSource.indexOf("Shared with Public</title>")
        if (beginingOfNextPostLocation > -1) {
            if (facebookProperties.getProperty("username").contains("kuba")) {
                // send tab from like of first post should bring back focus to the top
                driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]"))
                    .sendKeys(Keys.TAB)
            } else {
                // send tab from like of first post should bring back focus to the top
                // TODO check if xpath is the same for this account
                driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]"))
                    .sendKeys(Keys.TAB)
            }

            var pageSource: String = driver.pageSource.substringAfter("Shared with Public</title>")

            var postNumber = 1
            while (true) {
                // TODO fix edge case for last comment
                val nextPostStartPosition: Int = pageSource.indexOf("Shared with Public</title>")
                if (nextPostStartPosition == -1) {
                    break
                }
                //pageSource.substringAfter("permalink.php?story_fbid=").substringBefore("&") // id of post, wont' use it since API permission is needed to acces post made by others
                var postSource: String = pageSource.substringBefore("Shared with Public</title>")
                // commentAuthor shows name of next post since we're spliting post by shared with public which occures after a post author
                val nextPostAuthor: String =
                    postSource.substringAfter("<a aria-label=\"").substringBefore("\"")
                var commentTextBoxPosition: Int = postSource.indexOf("Write a comment")
                if (commentTextBoxPosition > -1) {
                    // can be commented
                    logger.debug("\tpost ${postNumber}, next one is written by ${nextPostAuthor} can be commented")
                    val adminUsernamePosition = postSource.indexOf("Kuba Dobrowolski-Nowakowski")
                    if (adminUsernamePosition == -1) {
                        // no comment from admin of fanpage
                        logger.debug("\t\tpost doesn't contain admin response")
                        val replyMessage: String = FacebookReplies.Companion.randomizeThankYouReply()
                        logger.info("\t\ttrying replying with '${replyMessage.replace("\n", "")}'")

                        try {
                            if (facebookProperties.getProperty("username").contains("kuba")) {
                                // chrome
//                              driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
//                                    .sendKeys(replyMessage.replace("\n", Keys.chord(Keys.SHIFT, Keys.ENTER)))
                                // firefox
                                for (letter in replyMessage.replace("\n", " ")) {
                                    // TODO this breaks for some rare kind of post (maybe ones with commenting turned off text or smth other)
                                    // TODO check if that fails if post has already one commment
                                    driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                                        .sendKeys(letter.toString())
                                    Thread.sleep(50)
                                }
                                Thread.sleep(500)
                                driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                                    .sendKeys(Keys.RETURN)
                            } else {
                                // firefox
                                for (letter in replyMessage.replace("\n", " ")) {
                                    // TODO this breaks for some rare kind of post (maybe ones with commenting turned off text or smth other)
                                    // TODO check if that fails if post has already one commment
                                    driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                                        .sendKeys(letter.toString())
                                    Thread.sleep(50)
                                }
                                Thread.sleep(500)
                                driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                                    .sendKeys(Keys.RETURN)
                            }
                        } catch (e: NoSuchElementException) {
                            logger.error(e.message)
                            logger.error("NoSuchElementException exception has been thrown during processing of ${id} post on ${postNumber}th post, next one is written by ${nextPostAuthor}")
                        } catch (e: Exception) {
                            logger.error(e.message)
                            logger.error("Exception exception has been thrown during processing of ${id} post on ${postNumber}th post , next one is written by ${nextPostAuthor}")
                        }

                        val numberOfSeconds: Long = (10..120).random().toLong()
                        logger.info("\t\tsleeping for ${numberOfSeconds} seconds\n")
                        Thread.sleep(1000 * numberOfSeconds)
                    } else {
                        logger.debug("\t\tpost contains admin response")
                        // TODO this breaks if there's longer post (i.e. with comments)
                        // TODO example is birth post no. 85
                        for (i in 1..20) {
                            driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                            Thread.sleep(5)
                        }
                    }
                } else {
                    logger.debug("\tpost ${postNumber}, next one is written by ${nextPostAuthor} can't be commented")
                    // TODO this breaks if there's longer post (i.e. with comments)
                    // TODO example is birth post no. 85
                    for (i in 1..8) {
                        driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                        Thread.sleep(5)
                    }
                }

                postNumber++
                pageSource = pageSource.substringAfter("Shared with Public</title>")
            }
        } else {
            logger.info("\tpost ${id} doesn't have any shared posts")
        }
    }
}