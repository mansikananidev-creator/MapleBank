import type {TransType} from "@/types";

type MoneyAmountProps = {
    amount: number
    direction: 'in' | 'out'
}

export default function MoneyAmount({ amount, direction }: MoneyAmountProps) {
    return <span className={`${direction === 'in' ? 'text-success' : 'text-destructive'} tabular-nums font-semibold`}>
        {(direction === 'in' ? '+' : '−') + formatCurrency(amount)}
    </span>
}

const inboundTypes: TransType[] = ['DEPOSIT', 'TRANSFER_IN', 'LOAN_DISBURSEMENT']

export const transTypeLabels: Record<TransType, string> = {
    DEPOSIT: 'Deposit',
    WITHDRAWAL: 'Withdrawal',
    TRANSFER_IN: 'Transfer in',
    TRANSFER_OUT: 'Transfer out',
    LOAN_DISBURSEMENT: 'Loan disbursement',
    LOAN_REPAYMENT: 'Loan repayment'
}

export function isInbound(type: TransType) {
    return inboundTypes.includes(type)
}

export function formatCurrency(amount: number, maximumFractionDigits = 2) {
    return new Intl.NumberFormat('el-GR', { style: 'currency', currency: 'CAD', maximumFractionDigits }).format(amount)
}