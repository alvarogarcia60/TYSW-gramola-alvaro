package edu.uclm.es.gramola.http;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.uclm.es.gramola.services.UserService;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("payments")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
public class PaymentsController {

    @Autowired
    private UserService userService;

    @Value("${stripe.secret}")
    private String stripeSecret;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecret;
    }

    @PostMapping("/prepay")
    public String prepay(@RequestBody Map<String, Object> request) throws Exception {
        String email = (String) request.get("email");

        // Acepta montos con decimales y convierte a céntimos para Stripe
        BigDecimal rawAmount = new BigDecimal(request.get("amount").toString());
        long amountInCents = rawAmount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency("eur")
            .setDescription("Gramola Service - " + email)
            .putMetadata("email", email)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
            )
            .build();

        PaymentIntent intent = PaymentIntent.create(params);
        
        // Registramos el intento en nuestra tabla de transacciones a través del servicio
        this.userService.saveStripeTransaction(email, request);

        return intent.toJson(); 
    }
}