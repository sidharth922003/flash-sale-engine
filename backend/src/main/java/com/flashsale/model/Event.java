package com.flashsale.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false)
    private LocalDateTime saleStartTime;

    private String imageUrl;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<TicketTier> ticketTiers = new ArrayList<>();

    public Event() {}

    public Event(String title, String description, String venue, String city, LocalDateTime eventDate, LocalDateTime saleStartTime, String imageUrl) {
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.city = city;
        this.eventDate = eventDate;
        this.saleStartTime = saleStartTime;
        this.imageUrl = imageUrl;
    }

    public void addTicketTier(TicketTier tier) {
        ticketTiers.add(tier);
        tier.setEvent(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public LocalDateTime getSaleStartTime() { return saleStartTime; }
    public void setSaleStartTime(LocalDateTime saleStartTime) { this.saleStartTime = saleStartTime; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<TicketTier> getTicketTiers() { return ticketTiers; }
    public void setTicketTiers(List<TicketTier> ticketTiers) { this.ticketTiers = ticketTiers; }
}
