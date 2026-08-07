import {useEffect, useState} from "react";
import type {AccountResponse, LoanResponse} from "@/types";
import api from "@/api/axios.ts";
import PageContainer from "@/components/PageContainer.tsx";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card.tsx";
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from "@/components/ui/dialog.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Label} from "@/components/ui/label.tsx";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.tsx";
import {Input} from "@/components/ui/input.tsx";
import {Badge} from "@/components/ui/badge.tsx";
import {statusVariant} from "@/lib/statusVariant.ts";
import {formatCurrency} from "@/components/MoneyAmount.tsx";

export default function LoansPage() {
    const [ loans, setLoans ] = useState<LoanResponse[]>([]);
    const [ loading, setLoading ] = useState(true)
    const [ accounts, setAccounts ] = useState<AccountResponse[]>([])
    const [ dialogOpen, setDialogOpen ] = useState(false)
    const [ applyAmount, setApplyAmount ] = useState('')
    const [ applyTermMonths, setApplyTermMonths ] = useState('')
    const [ applyPurpose, setApplyPurpose ] = useState('')
    const [ applyAccountId, setApplyAccountId ] = useState('')
    const [ submitting, setSubmitting ] = useState(false)
    const [ applyError, setApplyError ] = useState('')
    const [ repayAmount, setRepayAmount ] = useState('')
    const [ repayError, setRepayError ] = useState('')
    const [repayingLoanId, setRepayingLoanId] = useState<number | null>(null)

    async function fetchLoans() {
        try {
            const response = await api.get<LoanResponse[]>('/loans')
            setLoans(response.data)
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    async function fetchAccounts() {
        try {
            const response = await api.get<AccountResponse[]>('/accounts')
            setAccounts(response.data)
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    async function handleApply() {
        if (Number(applyAmount) <= 0) {
            setApplyError('Amount must be greater than zero')
            return
        }
        setSubmitting(true)
        setApplyError('')
        try {
            const body = {
                amount: Number(applyAmount),
                termMonths: Number(applyTermMonths),
                purpose: applyPurpose,
                accountId: Number(applyAccountId)
            }
            await api.post<LoanResponse>('/loans', body)
            setDialogOpen(false)
            setApplyAmount('')
            setApplyTermMonths('')
            setApplyPurpose('')
            setApplyAccountId('')
            await fetchLoans()
        } catch (err: any) {
            setApplyError(err.response?.data?.message ?? 'Loan application failed')
        } finally {
            setSubmitting(false)
        }
    }

    async function handleRepay(loanId: number) {
        if (Number(repayAmount) <= 0) {
            setRepayError('Amount must be greater than zero')
            return
        }
        setSubmitting(true)
        setRepayError('')
        try {
            await api.post(`/loans/${loanId}/repay`, { amount: Number(repayAmount) })
            setRepayAmount('')
            setRepayingLoanId(null)   // ← add this
            await fetchLoans()
        } catch (err: any) {
            setRepayError(err.response?.data?.message ?? 'Repayment failed')
        } finally {
            setSubmitting(false)
        }
    }

    useEffect(() => {
        fetchLoans()
        fetchAccounts()
    }, [])

    const activeAccounts = accounts.filter((a) => a.status === 'ACTIVE')

    return (
        <PageContainer>
            <h1 className="text-2xl font-bold mb-6">My Loans</h1>
            <div className="mb-6">
                <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
                    <DialogTrigger asChild>
                        <Button>Loan application</Button>
                    </DialogTrigger>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>Apply for a loan</DialogTitle>
                        </DialogHeader>

                        <div className="space-y-4 py-2">
                            <div className="space-y-2">
                                <Label>Account</Label>
                                <Select value={applyAccountId} onValueChange={setApplyAccountId}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select the account for your loan" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {activeAccounts.map((account) => (
                                            <SelectItem key={account.id} value={String(account.id)}>
                                                {account.nickname} — {formatCurrency(account.balance)}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="loan-amount">Loan amount</Label>
                                <Input
                                    type="number"
                                    id="loan-amount"
                                    placeholder="0.00"
                                    value={applyAmount}
                                    onChange={(e) => setApplyAmount(e.target.value)}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="term-months">Loan term (months)</Label>
                                <Input
                                    type="number"
                                    id="term-months"
                                    placeholder="e.g. 6 months"
                                    value={applyTermMonths}
                                    onChange={(e) => setApplyTermMonths(e.target.value)}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="loan-purpose">Purpose for the loan</Label>
                                <Input
                                    id="loan-purpose"
                                    placeholder="e.g. Buying a house"
                                    value={applyPurpose}
                                    onChange={(e) => setApplyPurpose(e.target.value)}
                                />
                            </div>
                        </div>

                        {applyError && <p className="text-sm text-destructive">{applyError}</p>}

                        <DialogFooter>
                            <Button onClick={handleApply} disabled={submitting}>
                                {submitting ? 'Applying...' : 'Apply'}
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
            </div>
            {loading ? (
                <p>Loading...</p>
            ) : loans.length === 0 ? (
                <p className="text-muted-foreground">You have no loans.</p>
            ) : (
                <div className="grid gap-4">
                    {loans.map((loan) => (
                        <Card key={loan.id} className="max-w-lg">
                            <CardHeader>
                                <CardTitle>{loan.purpose}</CardTitle>
                                <Badge variant={statusVariant(loan.status)}>{loan.status}</Badge>
                            </CardHeader>
                            <CardContent className="space-y-1 text-sm">
                                <p>Principal: {formatCurrency(loan.principalAmount)}</p>
                                <p>Remaining: {formatCurrency(loan.remainingBalance)}</p>
                                <p>Monthly payment: {formatCurrency(loan.monthlyPayment)}</p>
                                <p>Term: {loan.termMonths} months</p>
                                <p>Interest rate: {loan.interestRate}%</p>

                                {loan.status === 'ACTIVE' && (
                                    <div className="mt-4">
                                        <Dialog open={repayingLoanId === loan.id} onOpenChange={(open) => setRepayingLoanId(open ? loan.id : null)}>
                                            <DialogTrigger asChild>
                                                <Button>Repay</Button>
                                            </DialogTrigger>
                                            <DialogContent>
                                                <DialogHeader>
                                                    <DialogTitle>Repay loan</DialogTitle>
                                                </DialogHeader>
                                                <div className="space-y-2 py-2">
                                                    <Label htmlFor="repay-amount">Amount</Label>
                                                    <Input
                                                        type="number"
                                                        id="repay-amount"
                                                        placeholder="0.00"
                                                        value={repayAmount}
                                                        onChange={(e) => setRepayAmount(e.target.value)}
                                                    />
                                                    {repayError && <p className="text-sm text-destructive">{repayError}</p> }
                                                </div>
                                                <DialogFooter>
                                                    <Button onClick={() => handleRepay(loan.id)} disabled={submitting}>
                                                        {submitting ? 'Repaying...' : 'Repay'}
                                                    </Button>
                                                </DialogFooter>
                                            </DialogContent>
                                        </Dialog>
                                    </div>
                                )}
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </PageContainer>
    )
}