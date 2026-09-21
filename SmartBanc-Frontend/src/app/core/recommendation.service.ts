import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../lib/api.config';
import { Movement, Recommendation } from '../lib/models';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private http = inject(HttpClient);

  latest() {
    return this.http.get<Recommendation>(`${API_URL}/recommendations`, { observe: 'response' });
  }

  refresh(movements: Movement[]) {
    return this.http.post<void>(`${API_URL}/recommendations/refresh`, movements.map(movement => ({
      transactionId: movement.transactionId,
      sourceAccountNumber: movement.sourceAccountNumber,
      destinationAccountNumber: movement.destinationAccountNumber,
      amount: movement.amount,
      type: movement.type,
      serviceCode: movement.serviceCode,
      createdAt: movement.createdAt
    })));
  }
}
