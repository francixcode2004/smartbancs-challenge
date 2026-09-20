export interface UserProfile {
  accountNumber: string; name: string; email: string; balance: number; createdAt: string;
}
export interface AccessToken {
  accessToken: string; tokenType: string; expiresIn: number; accountNumber: string;
}
export interface Recipient { accountNumber: string; name: string; }
export interface BasicService { code: string; name: string; accountNumber: string; }
export type Operation = 'deposit' | 'withdraw' | 'transfer' | 'service_payment';
export interface Movement {
  transactionId: string; userId: string; sourceAccountNumber: string | null;
  destinationAccountNumber: string | null; amount: number; type: Operation;
  description: string | null; serviceCode: string | null; customerReference: string | null;
  idempotencyKey: string; createdAt: string;
}
export interface OperationBody {
  amount: string; description?: string; accountNumber?: string;
  destinationAccountNumber?: string; serviceCode?: string; customerReference?: string;
}
export interface PendingOperation {
  key: string; account: string; kind: Operation; body: OperationBody;
}
