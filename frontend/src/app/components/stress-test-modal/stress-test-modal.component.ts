import { Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FlashSaleService } from '../../services/flash-sale.service';
import { EventItem, StressTestResult, ConcurrencyStrategy } from '../../models/flash-sale.models';

@Component({
  selector: 'app-stress-test-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stress-test-modal.component.html',
  styleUrl: './stress-test-modal.component.css'
})
export class StressTestModalComponent {
  events = input.required<EventItem[]>();
  close = output<void>();

  selectedTierId = 1;
  concurrentUsers = 250;
  strategy: ConcurrencyStrategy = 'ATOMIC_DB_CONDITIONAL';

  isLoading = signal(false);
  result = signal<StressTestResult | null>(null);

  constructor(private flashSaleService: FlashSaleService) {}

  runTest() {
    this.isLoading.set(true);
    this.result.set(null);

    this.flashSaleService.runStressTest({
      ticketTierId: this.selectedTierId,
      totalConcurrentUsers: this.concurrentUsers,
      quantityPerUser: 1,
      strategy: this.strategy
    }).subscribe({
      next: (res) => {
        this.result.set(res);
        this.isLoading.set(false);
      },
      error: (err) => {
        alert('Simulation error: ' + (err.error?.message || err.message));
        this.isLoading.set(false);
      }
    });
  }

  resetStock() {
    this.flashSaleService.resetInventory(this.selectedTierId, 50).subscribe({
      next: () => {
        alert('Tier inventory reset to 50 seats.');
      }
    });
  }
}
