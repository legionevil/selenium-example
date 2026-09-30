package ru.legionevil.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.asserts.SoftAssert;
import ru.legionevil.core.ExceptionTranslatingDecorator;

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
    @DisplayName("Тест заголовка страницы")
    void checkPageTitle() {
        String title = driver.getTitle();
        softAssert.assertNotNull(title);
        String pageSource = driver.getPageSource();
        softAssert.assertNotNull(pageSource);
        softAssert.fail("просто 1 фэйл в софт ассерте");
        softAssert.fail("просто 2 фэйл в софт ассерте");
        driver.findElement(nonExistentLocator).click();
        driver.findElement(readOnlyLocator).clear();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
        if (softAssert != null) softAssert.assertAll("Упавшие проверки: ");
    }
}
