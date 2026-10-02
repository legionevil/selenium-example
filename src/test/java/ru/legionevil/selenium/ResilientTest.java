package ru.legionevil.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;
import ru.legionevil.core.ExceptionTranslatingDecorator;

import java.time.Duration;

/**
 * Использование selenium с модификациями, перевод и сборка ошибок, контроль исключений
 */
public class ResilientTest {
    private final By readOnlyLocator = By.name("my-readonly");
    private final By nonExistentLocator = By.name("non-existent");
    WebDriver driver;// декорированный драйвер с переводом и коллекционированием ошибок
    SoftAssert softAssert;// ассертер с коллектором

    @BeforeEach
    void setUp() {
        softAssert = new SoftAssert();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
//        options.addArguments("--force-color-profile=srgb");// Принудительно устанавливаем цветовой профиль sRGB
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        driver = new ExceptionTranslatingDecorator(softAssert).decorate(new ChromeDriver(options));
        driver.get("https://www.selenium.dev/selenium/web/web-form.html");
    }

    @Test
    @DisplayName("Тест отработки драйвера и софт ассерта с прерыванием")
    void checkDecoratedDriverSoftAndHardAssert() {
        String title = driver.getTitle();
        softAssert.assertNotNull(title);// проверяем штатную работу ассертера
        String pageSource = driver.getPageSource();
        softAssert.assertNotNull(pageSource);// проверяем штатную работу ассертера
        softAssert.fail("просто 1 фэйл в софт ассерте");// добавляем ошибку, показывающую отсутствие прерывания
        driver.findElement(nonExistentLocator).click();// получаем ошибку поиска и прерываемся на клике хард ассертом
        driver.findElement(readOnlyLocator).clear();// не доходим до этого шага
    }

    @Test
    @DisplayName("Тест отработки драйвера и софт ассерта с ожиданием и без прерывания")
    void checkDecoratedDriverWaitSoftAssert() {
        String pageSource = driver.getPageSource();
        softAssert.assertNotNull(pageSource);// проверяем штатную работу ассертера
        softAssert.fail("просто 1 фэйл в софт ассерте");// добавляем ошибку, показывающую отсутствие прерывания
        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(1L)).pollingEvery(Duration.ZERO);
        String text = wait.until(d -> driver.findElement(nonExistentLocator).getText());// получаем ошибку поиска
        softAssert.assertEquals(text, "текст");// проверяем пустой текст из-за ошибки поиска
        driver.findElement(readOnlyLocator).clear();// получаем ошибку выполнения неподходящего действия
    }

    @Test
    @DisplayName("Тест отработки драйвера и софт ассерта с ожиданием и прерыванием")
    void checkDecoratedDriverWaitHardAssert() {
        String pageSource = driver.getPageSource();
        softAssert.assertNotNull(pageSource);// проверяем штатную работу ассертера
        softAssert.fail("просто 1 фэйл в софт ассерте");// добавляем ошибку, показывающую отсутствие прерывания
        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ZERO).pollingEvery(Duration.ZERO);
        wait.until(d -> driver.findElement(nonExistentLocator).isDisplayed());// получаем ошибку поиска
        driver.findElement(readOnlyLocator).clear();// не доходим до этого шага
    }

    @Test
    @DisplayName("Тест отработки драйвера и софт ассерта с пустой сессией")
    void checkDecoratedDriverSoftAssertNoSession() {
        String pageSource = driver.getPageSource();
        softAssert.assertNotNull(pageSource);// проверяем штатную работу ассертера
        softAssert.fail("просто 1 фэйл в софт ассерте");// добавляем ошибку, показывающую отсутствие прерывания
        driver.quit();
        driver.findElement(readOnlyLocator).clear();// ошибка с разрывом сессии
        driver.findElement(nonExistentLocator).clear();// не доходим до этого шага
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
        if (softAssert != null) softAssert.assertAll("Упавшие проверки: ");
    }
}
