import mu.KotlinLogging
import org.openqa.selenium.*
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import kotlin.NoSuchElementException


class FacebookSharedPosts {

    private var driver: WebDriver
    private val facebookProperties: FacebookProperties = FacebookProperties()
    var commentedPosts = 0

    private val logger = KotlinLogging.logger {}

    init {
        // Firefox
        // https://www.browserstack.com/docs/automate/selenium/firefox-profile
        val firefoxProfile = FirefoxProfile()
//        firefoxProfile.setPreference("layout.css.devPixelsPerPx", "2.0")

        // https://stackoverflow.com/questions/15397483/how-do-i-set-browser-width-and-height-in-selenium-webdriver
        val firefoxOptions = FirefoxOptions()
        firefoxOptions.addArguments("--width=1000")
        firefoxOptions.addArguments("--height=3440")
        firefoxOptions.profile = firefoxProfile

        driver = FirefoxDriver(firefoxOptions)
        driver.manage().window().position = Point(800, 0)

        driver["https://www.facebook.com"]
//        driver.findElement(By.cssSelector("body")).sendKeys(Keys.chord(Keys.COMMAND, "-"))

        // Chrome
        //driver = ChromeDriver()

        // Safari
        //driver = SafariDriver()
    }

    fun loginToFacebook() {

        val facebookProperties = FacebookProperties()

        driver["https://www.facebook.com"]
        driver.findElement(By.id("email")).sendKeys(facebookProperties.getProperty("username"))
        driver.findElement(By.id("pass")).sendKeys(facebookProperties.getProperty("password"))
        Thread.sleep(500)
        // cookie form
        driver.findElement(By.className("_9xo6")).click()
        // login button
        driver.findElement(By.name("login")).click()
        Thread.sleep(6000)
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

    private fun canElementBeReachedAndPressTabOnIt(xpath: String): Boolean {
        logger.trace("\t\t\tcanElementBeReachedAndPressTabOnIt xpath=$xpath")
        return try {
            driver.findElement(By.xpath(xpath)).sendKeys(Keys.TAB)
    //            driver.findElement(By.xpath(xpath)).sendKeys(Keys.chord(Keys.SHIFT, Keys.TAB))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSharedPosts(postId: String) {

        val id = postId.substringAfter("_")
        // TODO get first words of post to log it alongside with id
        driver["https://www.facebook.com/shares/view?id=$id"]
        Thread.sleep(5000)

        val js = driver as JavascriptExecutor
        // https://github.com/SeleniumHQ/selenium/issues/4244#issuecomment-371533758
        js.executeScript("document.body.style.MozTransform = \"scale(0.55)\";")
        js.executeScript("document.body.style.MozTransformOrigin = \"0 0\";")

        // scroll down to bottom of page to load all posts (lazy loading)
        val scrollTimeout = 250
        var previousScrollHeight: Long = -1
        for (scrollNumber in 1..scrollTimeout) {
            driver.findElement(By.cssSelector("body")).sendKeys(Keys.PAGE_DOWN)
            Thread.sleep(1500)
            val currentScrollHeight: Long = js.executeScript("return document.body.scrollHeight") as Long
            // TODO break also when detecting response from admin
            if (currentScrollHeight <= previousScrollHeight) {
                logger.info("\t\treached bottom of the page after ${scrollNumber}th time out of $scrollTimeout tries")
                break
            }
            previousScrollHeight = currentScrollHeight
            if (scrollNumber % 50 == 0) {
                // should be debugged but can't set netty to info then
                logger.info("\t\tscrolling for ${scrollNumber}th time out of $scrollTimeout tries")
            }
        }

        // scroll to the top of page (focus is still at the bottom)
        js.executeScript("window.scrollTo(0, -document.body.scrollHeight)")
        Thread.sleep(2000)

        var pageSource: String
        if (driver.pageSource.indexOf("Shared with Public</title>") > -1) {
            if (facebookProperties.getProperty("username").contains("kuba")) {
                // send tab from like of first post should bring back focus to the top
                if (!this.canElementBeReachedAndPressTabOnIt("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]")) {
                    if (!this.canElementBeReachedAndPressTabOnIt("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]")) {
                        throw Exception("couldn't press Tab on like in first post")
                    }
                }
                // TODO check if locale of accounts are different and this causes below
                pageSource = driver.pageSource.substringAfter("People who shared this")
                    .substringAfter("Enlarge")  // for video
                    .substringAfter("<a aria-label=\"")
            } else {
                // send tab from like of first post should bring back focus to the top
                if (!this.canElementBeReachedAndPressTabOnIt("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]")) {
                    if (!this.canElementBeReachedAndPressTabOnIt("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]")) {
                        throw Exception("couldn't press Tab on like in first post")
                    }
                }

                // TODO check if locale of accounts are different and this causes below
                pageSource = driver.pageSource.substringAfter("People Who Shared This")
                    .substringAfter("Enlarge")  // for video
                    .substringAfter("<a aria-label=\"")
            }

            pageSource = pageSource.replace("<a aria-label=\"Click to view attachment\"", "")

            val totalPostNumber = pageSource.split("<a aria-label=\"").size
            var postNumber = 1
            while (true) {
                // TODO fix edge case for last comment
                val nextPostStartPosition: Int = pageSource.indexOf("<a aria-label=\"")
                if (nextPostStartPosition == -1) {
                    break
                }
                //pageSource.substringAfter("permalink.php?story_fbid=").substringBefore("&") // id of post, wont' use it since API permission is needed to accessed post made by others
                val postSource: String = pageSource.substringBefore("<a aria-label=\"")
                val postAuthor: String = postSource.substringBefore("\"")
                val commentTextBoxPosition: Int = postSource.indexOf("Write a comment")
                if (commentTextBoxPosition > -1) {
                    // can be commented
                    logger.debug("\t\tpost $postNumber/$totalPostNumber written by $postAuthor can be commented")
                    val adminUsernamePosition = postSource.indexOf("Kuba Dobrowolski-Nowakowski")
                    if (adminUsernamePosition == -1) {
                        // no comment from admin of fan page
                        logger.debug("\t\t\tpost doesn't contain admin response")
                        val replyMessage: String = FacebookReplies.randomizeThankYouReply()
                        logger.info("\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")

                        val commentTextFieldPossibleXpaths : List<String> = listOf(
                            "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]",
                            "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div/div[2]/div[1]/form/div/div[1]/div[1]/div/div[1]",
                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]",
                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"
                        )
                        var xpath = ""
                        val iter: Iterator<String> = commentTextFieldPossibleXpaths.iterator()
                        while (iter.hasNext()) {
                            xpath = iter.next()
                            if (this.canElementBeReachedAndPressTabOnIt(xpath)) {
                                break
                            } else {
                                if (!iter.hasNext()) {
                                    // last item
                                    throw Exception("couldn't press Tab comment text box")
                                }
                            }
                        }

                        try {
                            if (facebookProperties.getProperty("username").contains("kuba")) {
                                // chrome
//                              driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
//                                    .sendKeys(replyMessage.replace("\n", Keys.chord(Keys.SHIFT, Keys.ENTER)))
                                // firefox
                                for (letter in replyMessage.replace("\n", " ")) {
                                    driver.findElement(By.xpath(xpath))
                                        .sendKeys(letter.toString())
                                    Thread.sleep(50)
                                }
                                Thread.sleep(500)
                                driver.findElement(By.xpath(xpath))
                                    .sendKeys(Keys.RETURN)

                                commentedPosts++
                            } else {
                                // firefox
                                for (letter in replyMessage.replace("\n", " ")) {
                                    driver.findElement(By.xpath(xpath))
                                        .sendKeys(letter.toString())
                                    Thread.sleep(50)
                                }
                                Thread.sleep(500)
                                driver.findElement(By.xpath(xpath))
                                    .sendKeys(Keys.RETURN)

                                commentedPosts++
                            }
                        } catch (e: NoSuchElementException) {
                            logger.error(e.message)
                            logger.error("NoSuchElementException exception has been thrown during processing of $id post on ${postNumber} post written by $postAuthor")
                        } catch (e: Exception) {
                            logger.error(e.message)
                            logger.error("Exception exception has been thrown during processing of $id post on ${postNumber} post written by $postAuthor")
                        }

                        val numberOfSeconds: Long = (10..120).random().toLong()
                        logger.info("\t\t\tsleeping for $numberOfSeconds seconds\n")
                        Thread.sleep(1000 * numberOfSeconds)
                    } else {
                        logger.debug("\t\t\tpost contains admin response")
                        for (i in 1..20) {
                            driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                            Thread.sleep(5)
                        }
                    }
                } else {
                    logger.debug("\t\tpost $postNumber/$totalPostNumber written by $postAuthor can't be commented")
                    for (i in 1..8) {
                        driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                        Thread.sleep(5)
                    }
                }

                postNumber++
                pageSource = pageSource.substringAfter("<a aria-label=\"")
            }
        } else {
            logger.info("\t\tpost doesn't have any shared posts")
        }
    }
}