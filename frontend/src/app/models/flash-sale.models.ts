export type ConcurrencyStrategy = 'ATOMIC_DB_CONDITIONAL' | 'PESSIMISTIC_LOCK' | 'OPTIMISTIC_LOCK' | 'STRIPED_LOCK';

export interface TicketTier {
  id: number;
  name: string;
  price: number;
  totalSeats: number;
  availableSeats: number;
  version?: number;
}

export interface EventItem {
  id: number;
  title: string;
  description: string;
  venue: string;
  city: string;
  eventDate: string;
  saleStartTime: string;
  imageUrl: string;
  ticketTiers: TicketTier[];
}

export interface ReservationRequest {
  ticketTierId: number;
  quantity: number;
  userEmail: string;
  idempotencyKey: string;
  strategy?: ConcurrencyStrategy;
}

export interface ReservationResponse {
  reservationToken: string;
  eventId: number;
  eventTitle: string;
  ticketTierId: number;
  tierName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  status: 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED';
  createdAt: string;
  expiresAt: string;
  remainingSeconds: number;
}

export interface OrderCheckoutRequest {
  reservationToken: string;
  paymentMethod: string;
  paymentReference?: string;
}

export interface OrderResponse {
  orderNumber: string;
  reservationToken: string;
  eventTitle: string;
  tierName: string;
  quantity: number;
  amountPaid: number;
  status: 'PAID' | 'FAILED' | 'REFUNDED';
  paymentReference: string;
  confirmedAt: string;
}

export interface StressTestRequest {
  ticketTierId: number;
  totalConcurrentUsers: number;
  quantityPerUser: number;
  strategy: ConcurrencyStrategy;
}

export interface StressTestResult {
  tierId: number;
  tierName: string;
  initialAvailableSeats: number;
  finalAvailableSeats: number;
  totalRequests: number;
  successfulReservations: number;
  rejectedRequests: number;
  totalSeatsSold: number;
  oversoldCount: number;
  executionDurationMs: number;
  requestsPerSecond: number;
  strategyUsed: ConcurrencyStrategy;
  statusMessage: string;
}
