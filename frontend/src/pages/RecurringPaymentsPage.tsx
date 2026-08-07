import {useEffect, useState} from "react";
import type {AccountResponse, Frequency, RecurringPaymentResponse} from "@/types";
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
import {formatCurrency} from "@/components/MoneyAmount.tsx";
import {toast} from "sonner";
import {RefreshCw} from "lucide-react";

export default function RecurringPaymentsPage() {
    const [ payments, setPayments ] = useState<RecurringPaymentResponse[]>([])
    const [ loading, setLoading ] = useState(true)
    const [ accounts, setAccounts ] = useState<AccountResponse[]>([])
    const [ dialogOpen, setDialogOpen ] = useState(false)
    const [ fromAccountId, setFromAccountId ] = useState('')
    const [ toAccountNumber, setToAccountNumber ] = useState('')
    const [ amount, setAmount ] = useState('')
    const [ description, setDescription ] = useState('')
    const [ frequency, setFrequency ] = useState<Frequency>('MONTHLY')
    const [ submitting, setSubmitting ] = useState(false)
    const [ formError, setFormError ] = useState('')

    async function fetchPayments() {
        try {
            const response = await api.get<RecurringPaymentResponse[]>('/recurring-payments')
            setPayments(response.data)
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    async function fetchAccounts() {
        try {
            const response = await api.get<AccountResponse[]>('/accounts')
            setAccounts(response.data.filter(a => a.status === 'ACTIVE'))
        } catch (err) {
            console.error(err)
        }
    }

    async function handleCreate() {
        if (Number(amount) <= 0) {
            setFormError('Amount must be greater than zero')
            return
        }
        if (!toAccountNumber) {
            setFormError('Recipient account number is required')
            return
        }
        setSubmitting(true)
        setFormError('')
        try {
            const body = {
                fromAccountId: Number(fromAccountId),
                toAccountNumber,
                amount: Number(amount),
                description,
                frequency
            }
            await api.post('/recurring-payments', body)
            setDialogOpen(false)
            setFromAccountId('')
            setToAccountNumber('')
            setAmount('')
            setDescription('')
            setFrequency('MONTHLY')
            await fetchPayments()
            toast.success('Recurring payment scheduled')
        } catch (err: any) {
            setFormError(err.response?.data?.message ?? 'Failed to schedule recurring payment')
        } finally {
            setSubmitting(false)
        }
    }

    async function handleCancel(id: number) {
        try {
            await api.delete(`/recurring-payments/${id}`)
            await fetchPayments()
            toast.success('Recurring payment cancelled')
        } catch (err: any) {
            toast.error(err.response?.data?.message ?? 'Failed to cancel')
        }
    }

    useEffect(() => {
        fetchPayments()
        fetchAccounts()
    }, []);

    return (
        <PageContainer>
            <h1 className="text-2xl font-bold mb-2">Recurring Payments</h1>
            <p className="text-sm text-muted-foreground mb-6">
                Automate bill payments or regular transfers. A background job runs daily and sends any payment that's due.
            </p>
            <div className="mb-6">
                <Dialog open={dialogOpen} onOpenChange={(open) => { setDialogOpen(open); if (!open) setFormError('') }}>
                    <DialogTrigger asChild>
                        <Button><RefreshCw size={16} /> Schedule a payment</Button>
                    </DialogTrigger>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>Schedule a recurring payment</DialogTitle>
                        </DialogHeader>

                        <div className="space-y-4 py-2">
                            <div className="space-y-2">
                                <Label>From account</Label>
                                <Select value={fromAccountId} onValueChange={setFromAccountId}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select an account" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {accounts.map((account) => (
                                            <SelectItem key={account.id} value={String(account.id)}>
                                                {account.nickname} — {formatCurrency(account.balance)}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="to-account-number">To account number</Label>
                                <Input
                                    id="to-account-number"
                                    placeholder="UNI..."
                                    value={toAccountNumber}
                                    onChange={(e) => setToAccountNumber(e.target.value)}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="recurring-amount">Amount ($)</Label>
                                <Input
                                    type="number"
                                    id="recurring-amount"
                                    placeholder="0.00"
                                    value={amount}
                                    onChange={(e) => setAmount(e.target.value)}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label>Frequency</Label>
                                <Select value={frequency} onValueChange={(v) => setFrequency(v as Frequency)}>
                                    <SelectTrigger>
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="WEEKLY">Weekly</SelectItem>
                                        <SelectItem value="MONTHLY">Monthly</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="recurring-description">Description (optional)</Label>
                                <Input
                                    id="recurring-description"
                                    placeholder="e.g. Rent"
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                />
                            </div>
                        </div>

                        {formError && <p className="text-sm text-destructive">{formError}</p>}

                        <DialogFooter>
                            <Button onClick={handleCreate} disabled={submitting}>
                                {submitting ? 'Scheduling...' : 'Schedule payment'}
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
            </div>

            {loading ? (
                <p>Loading...</p>
            ) : payments.length === 0 ? (
                <p className="text-muted-foreground">You have no recurring payments scheduled.</p>
            ) : (
                <div className="grid gap-4">
                    {payments.map((payment) => (
                        <Card key={payment.id} className="max-w-lg">
                            <CardHeader>
                                <CardTitle>{payment.description || 'Recurring payment'}</CardTitle>
                                <Badge variant={payment.active ? 'default' : 'secondary'}>
                                    {payment.active ? 'Active' : 'Cancelled'}
                                </Badge>
                            </CardHeader>
                            <CardContent className="space-y-1 text-sm">
                                <p>From: {payment.fromAccountNumber}</p>
                                <p>To: {payment.toAccountNumber}</p>
                                <p>Amount: {formatCurrency(payment.amount)}</p>
                                <p>Frequency: {payment.frequency === 'WEEKLY' ? 'Weekly' : 'Monthly'}</p>
                                <p>Next payment: {new Date(payment.nextRunDate).toLocaleDateString()}</p>
                                {payment.active && (
                                    <div className="mt-3">
                                        <Button variant="outline" className="text-destructive border-destructive/30 hover:bg-destructive/10" onClick={() => handleCancel(payment.id)}>
                                            Cancel
                                        </Button>
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
