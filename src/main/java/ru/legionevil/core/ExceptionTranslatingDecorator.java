package ru.legionevil.core;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.decorators.Decorated;
import org.openqa.selenium.support.decorators.WebDriverDecorator;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import ru.legionevil.exceptions.ElementNotClickableUiException;
import ru.legionevil.exceptions.ElementNotFoundUiException;
import ru.legionevil.exceptions.InvalidElementStateUiException;
import ru.legionevil.exceptions.SessionNotFoundUiException;
import ru.legionevil.exceptions.UiInteractionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

public class ExceptionTranslatingDecorator extends WebDriverDecorator<WebDriver> {
    private final SoftAssert softAssert;

    public ExceptionTranslatingDecorator(SoftAssert softAssert) {
        this.softAssert = softAssert;
    }

    // Главный метод для перехвата
    @Override
    public @NonNull Object onError(@NonNull Decorated<?> target, @NonNull Method method, Object @NonNull [] args, @NonNull InvocationTargetException e) throws Throwable {
        // 1. Извлекаем оригинальное исключение Selenium
        Throwable originalException = e.getTargetException();

        // 2. Переводим в наше доменное исключение
        UiInteractionException translated = translateException(originalException, args, target.getOriginal(), method);

        // 3. Коллекционируем
        softAssert.fail("Ошибка:", translated);

        // 4. Бросаем переведенное исключение дальше (оно заменит оригинальное для вызывающего кода)?
        // или не бросаем?
        Class<?> returnType = method.getReturnType();

        // Если упал поиск элемента, создаем заглушку и передаем в нее сообщение об ошибке
        if (returnType == WebElement.class) {
            return createNullWebElement(translated.getMessage());
        }

        if (returnType == String.class) return "";
        if (returnType == boolean.class || returnType == Boolean.class) return false;

        return null;
    }

    public @NonNull Object call(@NonNull Decorated<?> target, @NonNull Method method, Object @NonNull [] args) throws Throwable {
        if (method.getName().equals("findElement")) {// дополняем поведение драйвера для метода findElement
            System.out.println("ищу элемент: " + args[0]);
        }
        return super.call(target, method, args);
    }

    @Contract("null, _, _, _ -> new")
    private @NonNull UiInteractionException translateException(Throwable original, Object @NonNull [] args,
                                                               Object targetOriginal, @NonNull Method method) {
        if (original instanceof NoSuchElementException) {
            return new ElementNotFoundUiException("Элемент не найден в DOM, селектор: " + args[0]);
        } else if (original instanceof ElementNotInteractableException) {
            return new ElementNotClickableUiException("Элемент существует, но с ним нельзя взаимодействовать", original);
        } else if (original instanceof TimeoutException) {
            return new UiInteractionException("Превышено время ожидания", original);
        } else if (original instanceof NoSuchSessionException) {
            return new SessionNotFoundUiException("Не найдена сессия драйвера");
        } else if (original instanceof InvalidElementStateException) {
            return new InvalidElementStateUiException(
                    String.format("Не подходящее состояние элемента, для метода \"%s\" селектор: \"%s\"",
                            method.toString().replaceFirst(".*\\.", ""),
                            targetOriginal.toString().replaceFirst(".*-> ", "").replace("]", "")
                    )
            );
        }
        return new UiInteractionException("Неизвестная ошибка UI: " + original.getMessage(), original);
    }

    private WebElement createNullWebElement(String originalError) {
        Set<String> actionMethods = Set.of("click", "sendKeys", "clear", "submit");
        return (WebElement) Proxy.newProxyInstance(
                WebElement.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    // если вызван один из actionMethods на заглушке
                    if (actionMethods.contains(methodName)) {
                        // Используем Hard Assert из TestNG, чтобы мгновенно прервать тест
                        Assert.fail(String.format(
                                "Критическая ошибка: Попытка вызвать метод [%s()] у несуществующего элемента!\n\t" +
                                        "Первоначальная ошибка: %s", methodName, originalError
                        ));
                    }
                    // Этот код обрабатывает ВСЕ вызовы к нашей заглушке (click, sendKeys, getText и т.д.)
                    Class<?> returnType = method.getReturnType();

                    if (returnType == String.class) return "";
                    if (returnType == boolean.class || returnType == Boolean.class) return false;
                    if (returnType == WebElement.class) return proxy; // Для цепочек вида findElement().findElement()

                    return null; // void методы (click(), clear(), sendKeys()) просто безопасно завершатся
                }
        );
    }
}
