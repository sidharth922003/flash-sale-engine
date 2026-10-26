import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from './components/navbar/navbar.component';
import { StressTestModalComponent } from './components/stress-test-modal/stress-test-modal.component';
import { CheckoutModalComponent } from './components/checkout-modal/checkout-modal.component';
import { FlashSaleService } from './services/flash-sale.service';
import { EventItem, TicketTier, ReservationResponse } from './models/flash-sale.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, StressTestModalComponent, CheckoutModalComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  events = signal<EventItem[]>([]);
  isLoading = signal(true);
  isReserving = signal(false);

  showLabModal = signal(false);
  activeReservation = signal<ReservationResponse | null>(null);

  constructor(private flashSaleService: FlashSaleService) {}

  ngOnInit() {
    this.loadEvents();
  }

  loadEvents() {
    this.isLoading.set(true);
    this.flashSaleService.getEvents().subscribe({
      next: (data) => {
        this.events.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load events:', err);
        this.isLoading.set(false);
      }
    });
  }

  openReserve(tier: TicketTier, event: EventItem) {
    this.isReserving.set(true);
    const mockEmail = `guest_${Math.floor(Math.random() * 10000)}@gmail.com`;
    const idempotencyKey = 'RES-' + Math.random().toString(36).substring(2, 15);

    this.flashSaleService.reserveTickets({
      ticketTierId: tier.id,
      quantity: 1,
      userEmail: mockEmail,
      idempotencyKey: idempotencyKey,
      strategy: 'ATOMIC_DB_CONDITIONAL'
    }).subscribe({
      next: (res) => {
        this.activeReservation.set(res);
        this.isReserving.set(false);
        this.loadEvents();
      },
      error: (err) => {
        alert(err.error?.message || 'Could not hold ticket: ' + err.message);
        this.isReserving.set(false);
      }
    });
  }
}
