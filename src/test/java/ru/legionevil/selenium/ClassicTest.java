package ru.legionevil.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Классический пример использования selenium, без модификаций
 */
public class ClassicTest {
    RemoteWebDriver driver;

    private final By readOnlyLocator = By.name("my-readonly");
    private final By hiddenLocator = By.name("my-hidden");
    private final By nonExistentLocator = By.name("non-existent");

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
//        options.addArguments("--force-color-profile=srgb");// Принудительно устанавливаем цветовой профиль sRGB
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        driver = new ChromeDriver(options);
        driver.get("https://www.selenium.dev/selenium/web/web-form.html");
    }

    @Test
    @DisplayName("Тест отработки InvalidElementStateException")
    void checkClassicDriverStateException() {
        // получаем оригинальный InvalidElementStateException
        Assertions.assertThrows(InvalidElementStateException.class, () -> driver.findElement(readOnlyLocator).clear());
    }

    @Test
    @DisplayName("Тест отработки ElementNotInteractableException")
    void checkClassicDriverInteractException() {
        // получаем оригинальный ElementNotInteractableException
        Assertions.assertThrowsExactly(ElementNotInteractableException.class, () -> driver.findElement(hiddenLocator).click());
    }

    @Test
    @DisplayName("Тест отработки NoSuchElementException")
    void checkClassicDriverNoElementException() {
        // получаем оригинальный NoSuchElementException
        Assertions.assertThrows(NoSuchElementException.class, () -> driver.findElement(nonExistentLocator).click());
    }

    @Test
    @DisplayName("Тест отработки NoSuchSessionException")
    void checkClassicDriverNoSessionException() {
        driver.quit();// закрываем драйвер и прерываем сессию
        // получаем оригинальный NoSuchSessionException
        Assertions.assertThrows(NoSuchSessionException.class, () -> driver.findElement(readOnlyLocator).click());
    }

    @Test
    @DisplayName("Тест отработки StaleElementReferenceException")
    void checkClassicDriverStaleException() {
        WebElement element = driver.findElement(readOnlyLocator);// заранее находим элемент
        driver.navigate().refresh();// обновляем страницу
        // получаем оригинальный StaleElementReferenceException
        Assertions.assertThrows(StaleElementReferenceException.class, element::click);
    }

    @Test
    @DisplayName("Тест отработки драйвера и софт ассерта с ожиданием и без прерывания")
    void checkDecoratedDriverWaitSoftAssert() {
        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ZERO).pollingEvery(Duration.ZERO);
        Executable executable = () -> wait.until(d -> driver.findElement(nonExistentLocator).isDisplayed());
        // получаем оригинальный TimeoutException
        Assertions.assertThrows(TimeoutException.class, executable);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }
}
