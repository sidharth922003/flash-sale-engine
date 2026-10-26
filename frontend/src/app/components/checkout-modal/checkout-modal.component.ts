import { Component, input, output, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FlashSaleService } from '../../services/flash-sale.service';
import { ReservationResponse, OrderResponse } from '../../models/flash-sale.models';

@Component({
  selector: 'app-checkout-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout-modal.component.html',
  styleUrl: './checkout-modal.component.css'
})
export class CheckoutModalComponent implements OnInit, OnDestroy {
  reservation = input.required<ReservationResponse>();
  close = output<void>();

  remainingSec = signal(600);
  isProcessing = signal(false);
  confirmedOrder = signal<OrderResponse | null>(null);
  paymentMethod = 'UPI';

  private timerInterval: any;

  constructor(private flashSaleService: FlashSaleService) {}

  ngOnInit() {
    this.remainingSec.set(this.reservation().remainingSeconds || 600);
    this.timerInterval = setInterval(() => {
      const cur = this.remainingSec();
      if (cur <= 1) {
        clearInterval(this.timerInterval);
        this.remainingSec.set(0);
      } else {
        this.remainingSec.set(cur - 1);
      }
    }, 1000);
  }

  ngOnDestroy() {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  formatTime(totalSeconds: number): string {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  confirmPayment() {
    this.isProcessing.set(true);
    this.flashSaleService.checkout({
      reservationToken: this.reservation().reservationToken,
      paymentMethod: this.paymentMethod
    }).subscribe({
      next: (order) => {
        this.confirmedOrder.set(order);
        this.isProcessing.set(false);
        if (this.timerInterval) clearInterval(this.timerInterval);
      },
      error: (err) => {
        alert('Checkout error: ' + (err.error?.message || err.message));
        this.isProcessing.set(false);
      }
    });
  }

  cancelAndClose() {
    this.flashSaleService.cancelReservation(this.reservation().reservationToken).subscribe({
      next: () => this.close.emit(),
      error: () => this.close.emit()
    });
  }
}
