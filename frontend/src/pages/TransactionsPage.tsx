import {useEffect, useState} from "react";
import api from "@/api/axios.ts";
import type {AccountResponse, TransactionResponse} from "@/types";
import PageContainer from "@/components/PageContainer.tsx";
import {Tabs, TabsContent, TabsList, TabsTrigger} from "@/components/ui/tabs.tsx";
import {Label} from "@/components/ui/label.tsx";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.tsx";
import {Input} from "@/components/ui/input.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Card, CardContent} from "@/components/ui/card.tsx";
import {formatCurrency} from "@/components/MoneyAmount.tsx";
import {cn} from "@/lib/utils.ts";
import {toast} from "sonner";

export default function TransactionsPage() {
    const [ accounts, setAccounts ] = useState<AccountResponse[]>([])
    const [ accountId, setAccountId ] = useState('');
    const [ loading, setLoading ] = useState(true)
    const [ depositAmount, setDepositAmount ] = useState('')
    const [ depositDescription, setDepositDescription ] = useState('')
    const [depositError, setDepositError] = useState('')
    const [ withdrawAmount, setWithdrawAmount ] = useState('')
    const [ withdrawDescription, setWithdrawDescription ] = useState('')
    const [ withdrawError, setWithdrawError ] = useState('')
    const [ submitting, setSubmitting ] = useState(false)
    const [ transferToNumber, setTransferToNumber ] = useState('')
    const [ transferAmount, setTransferAmount ] = useState('')
    const [ transferDescription, setTransferDescription ] = useState('')
    const [ transferError, setTransferError ] = useState('')

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

    async function handleDeposit() {
        if (Number(depositAmount) <= 0) {
            setDepositError('Amount must be greater than zero')
            return
        }
        setSubmitting(true)
        setDepositError('')
        try {
            const body = {
                accountId: Number(accountId),
                amount: Number(depositAmount),
                description: depositDescription
            }
            await api.post<TransactionResponse>('/transactions/deposit', body)
            setDepositAmount('')
            setDepositDescription('')
            await fetchAccounts()
            toast.success('Deposit successful')
        } catch (err: any) {
            setDepositError(err.response?.data?.message ?? 'Deposit failed')
        } finally {
            setSubmitting(false)
        }
    }

    async function handleWithdraw() {
        if (Number(withdrawAmount) <= 0) {
            setWithdrawError('Amount must be greater than zero')
            return
        }
        setSubmitting(true)
        setWithdrawError('')
        try {
            const body = {
                accountId: Number(accountId),
                amount: Number(withdrawAmount),
                description: withdrawDescription
            }
            await api.post<TransactionResponse>('/transactions/withdraw', body)
            setWithdrawAmount('')
            setWithdrawDescription('')
            await fetchAccounts()
            toast.success('Withdrawal successful')
        } catch (err: any) {
            setWithdrawError(err.response?.data?.message ?? 'Withdrawal failed')
        } finally {
            setSubmitting(false)
        }
    }

    async function handleTransfer() {
        if (Number(transferAmount) <= 0) {
            setTransferError('Amount must be greater than zero')
            return
        }
        setSubmitting(true)
        setTransferError('')
        try {
            const body = {
                fromAccountId: Number(accountId),
                toAccountNumber: transferToNumber,
                amount: Number(transferAmount),
                description: transferDescription
            }
            await api.post<TransactionResponse>('/transactions/transfer', body)
            setTransferToNumber('')
            setTransferAmount('')
            setTransferDescription('')
            await fetchAccounts()
            toast.success('Transfer successful')
        } catch (err: any) {
            setTransferError(err.response?.data?.message ?? 'Transfer failed')
        } finally {
            setSubmitting(false)
        }
    }

    useEffect(() => {
        fetchAccounts()
    }, [])

    const activeAccounts = accounts.filter((a) => a.status === 'ACTIVE')
    const selectedAccount = activeAccounts.find((a) => String(a.id) === accountId)

    return (
        <PageContainer>
            <h1 className="text-2xl font-bold mb-6">Transactions</h1>
            {loading ? (
                <p>Loading...</p>
            ) : (
                <div className="grid md:grid-cols-[1.7fr_1fr] gap-4 items-start max-w-3xl">
                    <Card className="py-0">
                        <CardContent className="p-5">
                            <Tabs defaultValue="deposit">
                                <TabsList>
                                    <TabsTrigger value="deposit">Deposit</TabsTrigger>
                                    <TabsTrigger value="withdraw">Withdraw</TabsTrigger>
                                    <TabsTrigger value="transfer">Transfer</TabsTrigger>
                                </TabsList>

                                <TabsContent value="deposit" className="space-y-4">
                                    <div className="space-y-2">
                                        <Label>Account</Label>
                                        <Select value={accountId} onValueChange={setAccountId}>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select an account" />
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
                                        <Label htmlFor="deposit-amount">Amount ($)</Label>
                                        <Input
                                            type="number"
                                            id="deposit-amount"
                                            placeholder="0.00"
                                            value={depositAmount}
                                            onChange={(e) => setDepositAmount(e.target.value)}
                                        />
                                    </div>

                                    <div className="space-y-2">
                                        <Label htmlFor="deposit-description">Description (optional)</Label>
                                        <Input
                                            id="deposit-description"
                                            placeholder="e.g. Birthday gift"
                                            value={depositDescription}
                                            onChange={(e) => setDepositDescription(e.target.value)}
                                        />
                                    </div>

                                    {depositError && <p className="text-sm text-destructive">{depositError}</p>}

                                    <Button onClick={handleDeposit} disabled={submitting}>
                                        {submitting ? 'Depositing...' : 'Make deposit'}
                                    </Button>
                                </TabsContent>

                                <TabsContent value="withdraw" className="space-y-4">
                                    <div className="space-y-2">
                                        <Label>Account</Label>
                                        <Select value={accountId} onValueChange={setAccountId}>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select an account" />
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
                                        <Label htmlFor="withdraw-amount">Amount ($)</Label>
                                        <Input
                                            type="number"
                                            id="withdraw-amount"
                                            placeholder="0.00"
                                            value={withdrawAmount}
                                            onChange={(e) => setWithdrawAmount(e.target.value)}
                                        />
                                        {selectedAccount && Number(withdrawAmount) > selectedAccount.balance && (
                                            <p className="text-sm text-amber-600 dark:text-amber-500">
                                                Exceeds balance of {formatCurrency(selectedAccount.balance)}
                                            </p>
                                        )}
                                    </div>

                                    <div className="space-y-2">
                                        <Label htmlFor="withdraw-description">Description (optional)</Label>
                                        <Input
                                            id="withdraw-description"
                                            placeholder="e.g. Birthday gift"
                                            value={withdrawDescription}
                                            onChange={(e) => setWithdrawDescription(e.target.value)}
                                        />
                                    </div>

                                    {withdrawError && <p className="text-sm text-destructive">{withdrawError}</p>}

                                    <Button onClick={handleWithdraw} disabled={submitting}>
                                        {submitting ? 'Withdrawing...' : 'Make withdrawal'}
                                    </Button>
                                </TabsContent>
                                <TabsContent value="transfer" className="space-y-4">
                                    <div className="space-y-2">
                                        <Label>From account</Label>
                                        <Select value={accountId} onValueChange={setAccountId}>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select an account" />
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
                                        <Label htmlFor="to-account">To account number</Label>
                                        <Input
                                            id="to-account"
                                            placeholder="UNI..."
                                            value={transferToNumber}
                                            onChange={(e) => setTransferToNumber(e.target.value)}
                                        />
                                        {activeAccounts.length > 1 && (
                                            <div className="flex items-center gap-2 flex-wrap">
                                                <span className="text-xs text-muted-foreground">Yours:</span>
                                                {activeAccounts
                                                    .filter((a) => a.id !== selectedAccount?.id && a.status === 'ACTIVE')
                                                    .map((a) => (
                                                        <button
                                                            key={a.id}
                                                            type="button"
                                                            onClick={() => setTransferToNumber(a.accountNumber)}
                                                            className="border border-[#FDE68A] bg-[#FEF3C7] text-[#B45309]
                                                            dark:border-[#B45309] dark:bg-[#451A03] dark:text-[#FDBA74]
                                                            text-xs font-medium px-2.5 py-1 rounded-full
                                                            hover:bg-[#FDE68A] dark:hover:bg-[#78350F]"
                                                        >
                                                            {a.nickname}
                                                        </button>
                                                    ))}
                                            </div>
                                        )}
                                    </div>

                                    <div className="space-y-2">
                                        <Label htmlFor="transfer-amount">Amount ($)</Label>
                                        <Input
                                            type="number"
                                            id="transfer-amount"
                                            placeholder="0.00"
                                            value={transferAmount}
                                            onChange={(e) => setTransferAmount(e.target.value)}
                                        />
                                        {selectedAccount && Number(transferAmount) > selectedAccount.balance && (
                                            <p className="text-sm text-amber-600 dark:text-amber-500">
                                                Exceeds balance of {formatCurrency(selectedAccount.balance)}
                                            </p>
                                        )}
                                    </div>

                                    <div className="space-y-2">
                                        <Label htmlFor="transfer-description">Description (optional)</Label>
                                        <Input
                                            id="transfer-description"
                                            placeholder="e.g. Ticket money"
                                            value={transferDescription}
                                            onChange={(e) => setTransferDescription(e.target.value)}
                                        />
                                    </div>

                                    {transferError && <p className="text-sm text-destructive">{transferError}</p>}

                                    <Button onClick={handleTransfer} disabled={submitting}>
                                        {submitting ? 'Transferring...' : 'Make transfer'}
                                    </Button>
                                </TabsContent>
                            </Tabs>
                        </CardContent>
                    </Card>
                    <Card className="py-0 gap-0">
                        <p className="text-xs font-medium text-muted-foreground px-4 pt-3.5 pb-2">Your accounts</p>
                        {activeAccounts.map((account) => {
                            const isSelected = account.id === selectedAccount?.id
                            return(
                                <div
                                    key={account.id}
                                    onClick={() => setAccountId(String(account.id))}
                                    className={cn("cursor-pointer flex items-center gap-2.5 px-4 py-2.5 border-t hover:bg-muted/50 transition-colors", isSelected && "bg-[#FEF3C7] dark:bg-[#451A03]")}
                                >
                                    <div className={`w-2 h-2 rounded-full shrink-0 ${
                                        account.type === 'CHECKING' ? 'bg-[#B45309]' : 'bg-[#27272A] dark:bg-[#A1A1AA]'
                                    }`} />
                                    <p className={cn("text-xs flex-1 truncate", isSelected && "text-[#B45309] dark:text-[#FDBA74]")}>
                                        {account.nickname}
                                    </p>
                                    <p className={cn("text-xs font-semibold tabular-nums", isSelected && "text-[#B45309] dark:text-[#FDBA74]")}>
                                        {formatCurrency(account.balance)}
                                    </p>
                                </div>
                            )
                        })}
                    </Card>
                </div>
            )}
        </PageContainer>
    )
}