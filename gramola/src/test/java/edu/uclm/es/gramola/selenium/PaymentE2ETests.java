package edu.uclm.es.gramola.selenium;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentListParams;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Pruebas E2E Selenium – Sección 4 (cliente del bar)
 */
@SuppressWarnings("unused") // Los métodos @BeforeAll, @BeforeEach, @AfterAll son usados por JUnit
public class PaymentE2ETests {

    private static final String BAR_EMAIL = "algarcimartinez@gmail.com";
    private static final String FRONT_BASE = "http://127.0.0.1:4200";
    private static final String BACK_BASE = "http://localhost:8080";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static WebDriver driver;

    @BeforeAll
    static void setupClass() throws Exception {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

        // Cargar secret de Stripe desde application.properties
        Properties props = new Properties();
        props.load(Files.newInputStream(Paths.get("src/main/resources/application.properties")));
        String stripeSecret = props.getProperty("stripe.secret");
        if (stripeSecret == null || stripeSecret.isBlank()) {
            throw new IllegalStateException("stripe.secret no está configurado en application.properties");
        }
        // Resolver placeholder ${...} si existe
        if (stripeSecret.startsWith("${") && stripeSecret.contains(":")) {
            stripeSecret = stripeSecret.substring(stripeSecret.indexOf(":") + 1, stripeSecret.length() - 1);
        }
        Stripe.apiKey = stripeSecret;
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) driver.quit();
    }

    @BeforeEach
    void clearState() {
        driver.manage().deleteAllCookies();
    }

    @Test
    void flujoExitoso_busca_paga_inserta() throws Exception {
        String trackBuscada = "Estopa"; // cadena de búsqueda
        int playlistSizeBefore = fetchPlaylist().size();

        driver.get(FRONT_BASE + "/jukebox/" + BAR_EMAIL);

        // Buscar canción
        WebElement input = driver.findElement(By.cssSelector("input[placeholder='Busca una canción en Spotify...']"));
        input.sendKeys(trackBuscada);
        input.sendKeys(Keys.RETURN);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        WebElement addBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".song-card .btn-add-footer")));
        String songTitle = driver.findElement(By.cssSelector(".song-card .song-title")).getText();
        addBtn.click();

        // Pantalla de pago: insertar tarjeta de prueba válida
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("card-element")));
        // Stripe inserta un iframe dentro de #card-element
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("#card-element iframe")));
        WebElement cardNumber = wait.until(ExpectedConditions.elementToBeClickable(By.name("cardnumber")));
        cardNumber.sendKeys("4242424242424242");
        driver.switchTo().defaultContent();

        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("#card-element iframe")));
        WebElement exp = wait.until(ExpectedConditions.elementToBeClickable(By.name("exp-date")));
        exp.sendKeys("0428");
        WebElement cvc = wait.until(ExpectedConditions.elementToBeClickable(By.name("cvc")));
        cvc.sendKeys("234");
        driver.switchTo().defaultContent();

        driver.findElement(By.cssSelector(".confirm-btn")).click();

        // Esperar mensaje de éxito
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".status-alert"), "Pago realizado"));

        // Verificar en Stripe: PaymentIntent succeeded con metadata email
        boolean intentOk = StripePaymentVerifier.verifyLatestIntentForEmail(BAR_EMAIL, 50L);
        assertTrue(intentOk, "El PaymentIntent no aparece con estado succeeded y email en metadata");

        // Verificar cola: la canción está en playlist y en la posición esperada (tamaño previo + 1)
        List<Map<String, Object>> playlist = fetchPlaylist();
        int expectedPosition = playlistSizeBefore + 1;
        boolean inQueue = playlist.stream().anyMatch(p -> songTitle.equalsIgnoreCase(String.valueOf(p.get("title")))
            && Integer.parseInt(p.get("queuePosition").toString()) == expectedPosition);
        assertTrue(inQueue, "La canción no aparece en la posición esperada de la cola");
    }

    @Test
    void flujoError_tarjetaInvalida_noInserta() throws Exception {
        String trackBuscada = "Shakira"; // Buscar una canción diferente al test exitoso

        // Se verifica que la canción no se inserta; no se asume tamaño fijo de la cola

        driver.get(FRONT_BASE + "/jukebox/" + BAR_EMAIL);
        WebElement input = driver.findElement(By.cssSelector("input[placeholder='Busca una canción en Spotify...']"));
        input.sendKeys(trackBuscada);
        input.sendKeys(Keys.RETURN);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        WebElement addBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".song-card .btn-add-footer")));
        String songTitle = driver.findElement(By.cssSelector(".song-card .song-title")).getText();
        addBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("card-element")));
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("#card-element iframe")));
        WebElement cardNumber = wait.until(ExpectedConditions.elementToBeClickable(By.name("cardnumber")));
        cardNumber.sendKeys("4000000000000002"); // tarjeta de fallo
        driver.switchTo().defaultContent();

        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("#card-element iframe")));
        WebElement exp = wait.until(ExpectedConditions.elementToBeClickable(By.name("exp-date")));
        exp.sendKeys("0428");
        WebElement cvc = wait.until(ExpectedConditions.elementToBeClickable(By.name("cvc")));
        cvc.sendKeys("234");
        driver.switchTo().defaultContent();

        driver.findElement(By.cssSelector(".confirm-btn")).click();

        // Esperar unos segundos para que el backend procese el pago fallido
        Thread.sleep(5000);

        // Verificar que NO se añade a la cola
        List<Map<String, Object>> playlist = fetchPlaylist();
        boolean inQueue = playlist.stream().anyMatch(p -> songTitle.equalsIgnoreCase(String.valueOf(p.get("title"))));
        assertFalse(inQueue, "La canción no debería insertarse tras fallo de pago");
    }

    /** Obtiene la playlist del backend y la parsea como lista de mapas. */
    private List<Map<String, Object>> fetchPlaylist() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BACK_BASE + "/music/getPlaylist?email=" + BAR_EMAIL))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), "GET /music/getPlaylist devolvió error");
        return MAPPER.readValue(resp.body(), new TypeReference<List<Map<String, Object>>>() {});
    }

    /** Verificador de PaymentIntent en Stripe. */
    static class StripePaymentVerifier {
        static boolean verifyLatestIntentForEmail(String email, long amountCents) throws Exception {
            PaymentIntentListParams params = PaymentIntentListParams.builder()
                    .setLimit(10L)
                    .build();
            List<PaymentIntent> intents = PaymentIntent.list(params).getData();
                return intents.stream()
                    .filter(pi -> "succeeded".equalsIgnoreCase(pi.getStatus()))
                    .filter(pi -> amountCents == pi.getAmount())
                    .filter(pi -> email.equalsIgnoreCase(pi.getMetadata().get("email")))
                    .sorted((a, b) -> Long.compare(b.getCreated(), a.getCreated()))
                    .findFirst()
                    .isPresent();
        }
    }
}
