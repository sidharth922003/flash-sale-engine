import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EventItem,
  ReservationRequest,
  ReservationResponse,
  OrderCheckoutRequest,
  OrderResponse,
  StressTestRequest,
  StressTestResult,
  ConcurrencyStrategy
} from '../models/flash-sale.models';

import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FlashSaleService {
  private http = inject(HttpClient);
  private baseUrl = (typeof window !== 'undefined' && localStorage.getItem('FLASH_API_URL')) 
    ? localStorage.getItem('FLASH_API_URL')! 
    : environment.apiUrl;

  getEvents(): Observable<EventItem[]> {
    return this.http.get<EventItem[]>(`${this.baseUrl}/events`);
  }

  getEventById(id: number): Observable<EventItem> {
    return this.http.get<EventItem>(`${this.baseUrl}/events/${id}`);
  }

  reserveTickets(request: ReservationRequest): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${this.baseUrl}/reservations/reserve`, request);
  }

  getReservation(token: string): Observable<ReservationResponse> {
    return this.http.get<ReservationResponse>(`${this.baseUrl}/reservations/${token}`);
  }

  cancelReservation(token: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/reservations/${token}`);
  }

  checkout(request: OrderCheckoutRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.baseUrl}/orders/checkout`, request);
  }

  runStressTest(request: StressTestRequest): Observable<StressTestResult> {
    return this.http.post<StressTestResult>(`${this.baseUrl}/simulation/load-test`, request);
  }

  resetInventory(tierId: number, seats: number = 100): Observable<string> {
    return this.http.post(`${this.baseUrl}/simulation/reset/${tierId}?seats=${seats}`, null, {
      responseType: 'text'
    });
  }

  getStrategies(): Observable<ConcurrencyStrategy[]> {
    return this.http.get<ConcurrencyStrategy[]>(`${this.baseUrl}/simulation/strategies`);
  }
}
