export type Role = "CUSTOMER" | "ADMIN"
export type AccountType = "CHECKING" | "SAVINGS"
export type AccountStatus = "ACTIVE" | "CLOSED" | "FROZEN"
export type TransType =
    | "DEPOSIT"
    | "WITHDRAWAL"
    | "TRANSFER_IN"
    | "TRANSFER_OUT"
    | "LOAN_DISBURSEMENT"
    | "LOAN_REPAYMENT"
export type LoanStatus = "PENDING" | "APPROVED" | "REJECTED" | "ACTIVE" | "PAID_OFF"
export type Frequency = "WEEKLY" | "MONTHLY"

export interface AuthResponse {
    token: string
}

export interface DecodedToken {
    sub: string
    role: Role
    iat: number
    exp: number
}

export interface AccountResponse {
    id: number
    accountNumber: string
    nickname: string
    type: AccountType
    balance: number
    status: AccountStatus
}

export interface TransactionResponse {
    id: number
    type: TransType
    amount: number
    balanceAfter: number
    description: string
    createdAt: string
}

export interface LoanResponse {
    id: number
    principalAmount: number
    remainingBalance: number
    interestRate: number
    termMonths: number
    monthlyPayment: number
    status: LoanStatus
    purpose: string
    appliedAt: string
}

export interface ErrorResponse {
    timestamp: string
    status: number
    message: string
    path: string
}

export interface Page<T> {
    content: T[]
    totalPages: number
    totalElements: number
    number: number
    size: number
    first: boolean
    last: boolean
}

export interface ComplianceFlagResponse {
    id: number
    transactionId: number
    accountNumber: string
    accountOwnerName: string
    transactionType: TransType
    amount: number
    reason: string
    reviewed: boolean
    reviewedByUsername: string | null
    reviewedAt: string | null
    createdAt: string
}

export interface RecurringPaymentResponse {
    id: number
    fromAccountNumber: string
    toAccountNumber: string
    amount: number
    description: string
    frequency: Frequency
    nextRunDate: string
    active: boolean
}

export interface MonthlySummaryResponse {
    month: string
    income: number
    expense: number
}

export interface UserProfileResponse {
    id: number
    username: string
    email: string
    fullName: string
    role: Role
    phoneNumber: string | null
    dateOfBirth: string | null
    addressLine1: string | null
    addressLine2: string | null
    city: string | null
    province: string | null
    postalCode: string | null
}