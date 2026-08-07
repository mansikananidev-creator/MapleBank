import {useEffect, useState} from "react";
import api from "@/api/axios.ts";
import type {AccountResponse, AccountType} from "@/types";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Label} from "@/components/ui/label.tsx";
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from "@/components/ui/dialog.tsx";
import {Input} from "@/components/ui/input.tsx";
import {useNavigate} from "react-router-dom";
import PageContainer from "@/components/PageContainer.tsx";
import {formatCurrency} from "@/components/MoneyAmount.tsx";
import ArcCard from "@/components/ArcCard.tsx";
import {Copy, Landmark, Plus, Wallet} from "lucide-react";
import {toast} from "sonner";
import {statusVariant} from "@/lib/statusVariant.ts";
import {Badge} from "@/components/ui/badge.tsx";

export default function AccountsPage() {
    const [ accounts, setAccounts ] = useState<AccountResponse[]>([])
    const [ loading, setLoading ] = useState(true)
    const [ type, setType ] = useState<AccountType>('CHECKING')
    const [ nickname, setNickname ] = useState('')
    const [ dialogOpen, setDialogOpen ] = useState(false)
    const [ submitting, setSubmitting ] = useState(false)
    const navigate = useNavigate()

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

    async function handleOpenAccount() {
        setSubmitting(true)
        try {
            await api.post<AccountResponse>('/accounts', { type, nickname: nickname.trim() || null })
            setDialogOpen(false)
            setNickname('')
            await fetchAccounts()
        } catch (err) {
            console.error(err)
        } finally {
            setSubmitting(false)
        }
    }

    useEffect(() => {
        fetchAccounts()
    }, [])

    const sortedAccounts = [...accounts].sort((a, b) => {
        if (a.status === 'CLOSED' && b.status !== 'CLOSED') return 1
        if (a.status !== 'CLOSED' && b.status === 'CLOSED') return -1
        return 0
    })

    return (
        <PageContainer>
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold">My Accounts</h1>

                <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
                    <DialogTrigger asChild>
                        <Button>Open account</Button>
                    </DialogTrigger>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>Open a new account</DialogTitle>
                        </DialogHeader>

                        <div className="space-y-4 py-2">
                            <div className="space-y-2">
                                <Label>Account type</Label>
                                <Select value={type} onValueChange={(v) => setType(v as AccountType)}>
                                    <SelectTrigger>
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="CHECKING">Checking</SelectItem>
                                        <SelectItem value="SAVINGS">Savings</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="nickname">Nickname (optional)</Label>
                                <Input
                                    id="nickname"
                                    placeholder="e.g. Rent money"
                                    value={nickname}
                                    onChange={(e) => setNickname(e.target.value)}
                                />
                            </div>
                        </div>

                        <DialogFooter>
                            <Button onClick={handleOpenAccount} disabled={submitting}>
                                {submitting ? 'Loading...' : 'Open account'}
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
            </div>

            {loading ? (
                <p>Loading...</p>
            ) : accounts.length === 0 ? (
                <p className="text-muted-foreground">You don't have any accounts yet.</p>
            ) : (
                <div className="grid gap-[14px] grid-cols-[repeat(auto-fill,minmax(260px,1fr))]">
                    {sortedAccounts.map((account) => {
                        if (account.status === "CLOSED") {
                            const typeLabel = account.type === 'CHECKING' ? 'Checking' : 'Savings'

                            return (
                                <div
                                    key={account.id}
                                    onClick={() => navigate(`/accounts/${account.id}`)}
                                    className="bg-muted rounded-[20px] h-[160px] p-4 flex flex-col justify-between cursor-pointer"
                                >
                                    <div className="flex justify-between items-start">
                                        <div>
                                            <div className="text-muted-foreground text-base font-semibold">
                                                {account.nickname ?? typeLabel}
                                            </div>
                                            <div className="text-muted-foreground/70 text-sm mt-0.5">{typeLabel}</div>
                                        </div>
                                        <Badge variant={statusVariant(account.status)}>{account.status}</Badge>
                                    </div>
                                    <div className="flex justify-between items-end">
                                        <div className="text-muted-foreground/70 text-sm">•••• {account.accountNumber.slice(-4)}</div>
                                        <div className="text-muted-foreground/70 text-xl font-semibold tabular-nums">
                                            {formatCurrency(account.balance)}
                                        </div>
                                    </div>
                                </div>
                            )
                        }

                        const isChecking = account.type === 'CHECKING'
                        const typeLabel = isChecking ? 'Checking' : 'Savings'
                        const secondary = isChecking ? 'text-[#FDE68A]' : 'text-[#A1A1AA]'
                        const Icon = isChecking ? Wallet : Landmark

                        return (
                            <ArcCard
                                key={account.id}
                                variant={isChecking ? 'gold' : 'zinc'}
                                arcs="md"
                                className="h-[160px] cursor-pointer"
                                onClick={() => navigate(`/accounts/${account.id}`)}
                            >
                                <div className="p-4 h-full flex flex-col justify-between">
                                    <div className="flex justify-between items-start">
                                        <div>
                                            <div className="text-white text-base font-semibold">
                                                {account.nickname ?? typeLabel}
                                            </div>
                                            <div className={`text-sm mt-0.5 ${secondary}`}>{typeLabel}</div>
                                        </div>
                                        <Icon size={17} className={secondary} />
                                    </div>
                                    <div className="flex justify-between items-end">
                                        <div className={`flex items-center gap-1.5 ${secondary}`}>
                                            <span className="text-sm">•••• {account.accountNumber.slice(-4)}</span>
                                            <button
                                                className="cursor-pointer transition hover:text-white active:scale-90"
                                                aria-label="Copy account number"
                                                onClick={(e) => {
                                                    e.stopPropagation()
                                                    navigator.clipboard.writeText(account.accountNumber)
                                                    toast.success('Account number copied')
                                                }}
                                            >
                                                <Copy size={14} />
                                            </button>
                                        </div>
                                        <div className="text-white text-xl font-semibold tabular-nums">
                                            {formatCurrency(account.balance)}
                                        </div>
                                    </div>
                                </div>
                            </ArcCard>
                        )
                    })}
                    <button
                        onClick={() => setDialogOpen(true)}
                        className="h-[160px] rounded-[20px] border-2 border-dashed border-muted-foreground/30 flex flex-col items-center justify-center gap-1.5 text-muted-foreground hover:border-muted-foreground/60 hover:text-foreground transition cursor-pointer"
                    >
                        <Plus size={22} />
                        <span className="text-sm font-medium">New account</span>
                    </button>
                </div>
            )}
        </PageContainer>
    )
}