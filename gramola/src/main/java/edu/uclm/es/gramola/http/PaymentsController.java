package edu.uclm.es.gramola.http;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.uclm.es.gramola.dao.StripeTransactionDao;
import edu.uclm.es.gramola.model.StripeTransaction;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentsController {

    @Autowired
    private StripeTransactionDao stripeDao;

    @Value("${stripe.secret}")
    private String stripeSecret;

    @PostConstruct
    public void initStripe() {
        // Inicializa la clave de Stripe desde configuración
        Stripe.apiKey = stripeSecret;
    }

    @PostMapping("/prepay") // 1. Cambiamos a POST por seguridad y estándares
    public String prepay(@RequestParam String email) throws Exception { //
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(1000L)
            .setCurrency("eur")
            .setDescription("Suscripción Gramola - " + email) // Personalizamos
            .putMetadata("email", email)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .build();

        PaymentIntent intent = PaymentIntent.create(params);

        StripeTransaction st = new StripeTransaction();
        st.setId(intent.getId());
        st.setData(intent.toJson());
        st.setEmail(email);
        stripeDao.save(st);

        return intent.toJson(); 
    }
}