package com.flashsale.config;

import com.flashsale.model.Event;
import com.flashsale.model.TicketTier;
import com.flashsale.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final EventRepository eventRepository;

    public DataInitializer(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void run(String... args) {
        if (eventRepository.count() > 0) {
            log.info("Events already initialized.");
            return;
        }

        log.info("Seeding realistic high-demand flash sale events and ticket tiers...");

        // Event 1: Coldplay
        Event coldplay = new Event(
                "Coldplay: Music of the Spheres World Tour",
                "Experience the global phenomenon live in India with groundbreaking stadium production, wristband lights, and an electrifying setlist.",
                "DY Patil Stadium",
                "Mumbai, India",
                LocalDateTime.now().plusDays(45).withHour(19).withMinute(0),
                LocalDateTime.now().minusHours(1), // Sale already live!
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80"
        );
        coldplay.addTicketTier(new TicketTier("VIP Hospitality Lounge", new BigDecimal("12500.00"), 50, 50));
        coldplay.addTicketTier(new TicketTier("Ground Floor Standing (Flash Tier)", new BigDecimal("4500.00"), 120, 120));
        coldplay.addTicketTier(new TicketTier("Level 2 Upper Seating", new BigDecimal("2500.00"), 200, 200));

        // Event 2: Formula 1
        Event f1 = new Event(
                "Formula 1 Grand Prix — Flash Pass Release",
                "High-speed motorsport thrills featuring the world's elite drivers, pit lane access, and live paddock concerts.",
                "Buddh International Circuit",
                "Greater Noida, India",
                LocalDateTime.now().plusDays(60).withHour(15).withMinute(0),
                LocalDateTime.now().minusMinutes(15), // Sale live
                "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=1200&q=80"
        );
        f1.addTicketTier(new TicketTier("Paddock Club VIP", new BigDecimal("35000.00"), 25, 25));
        f1.addTicketTier(new TicketTier("Main Grandstand", new BigDecimal("9500.00"), 100, 100));
        f1.addTicketTier(new TicketTier("General Admission Park", new BigDecimal("3200.00"), 250, 250));

        // Event 3: Diljit Dosanjh
        Event diljit = new Event(
                "Diljit Dosanjh — Dil-Luminati India Tour",
                "The historic record-breaking Punjabi powerhouse tour arrives with unmatched energy, bhangra beats, and stadium-shaking anthems.",
                "Jawaharlal Nehru Stadium",
                "New Delhi, India",
                LocalDateTime.now().plusDays(30).withHour(18).withMinute(30),
                LocalDateTime.now().minusMinutes(5), // Sale live
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80"
        );
        diljit.addTicketTier(new TicketTier("Fan Pit Gold (Front Stage)", new BigDecimal("8500.00"), 60, 60));
        diljit.addTicketTier(new TicketTier("Silver Floor Standing", new BigDecimal("3800.00"), 150, 150));
        diljit.addTicketTier(new TicketTier("North Stand Seated", new BigDecimal("1999.00"), 300, 300));

        eventRepository.save(coldplay);
        eventRepository.save(f1);
        eventRepository.save(diljit);

        log.info("Initialized 3 flash sale events with 9 ticket tiers.");
    }
}
