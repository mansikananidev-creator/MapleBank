import PageContainer from "@/components/PageContainer.tsx";
import {useEffect, useState} from "react";
import type {AccountResponse, LoanResponse, MonthlySummaryResponse, TransactionResponse} from "@/types";
import api from "@/api/axios.ts";
import {Card, CardContent} from "@/components/ui/card.tsx";
import {Button} from "@/components/ui/button.tsx";
import {useNavigate} from "react-router-dom";
import MoneyAmount, {formatCurrency, isInbound, transTypeLabels} from "@/components/MoneyAmount.tsx";
import {ArrowDownLeft, ArrowLeftRight, ArrowUpRight, CreditCard, Plus} from "lucide-react";
import {Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis} from "recharts";

function monthLabel(month: string) {
    const [year, monthNum] = month.split('-')
    const date = new Date(Number(year), Number(monthNum) - 1, 1)
    return date.toLocaleDateString('en-US', { month: 'short' })
}

export default function Dashboard() {
    const [ accounts, setAccounts ] = useState<AccountResponse[]>([])
    const [ loans, setLoans ] = useState<LoanResponse[]>([])
    const [ recentTransactions, setRecentTransactions ] = useState<TransactionResponse[]>([])
    const [ summary, setSummary ] = useState<MonthlySummaryResponse[]>([])
    const [ loading, setLoading ] = useState(true)
    const navigate = useNavigate()

    async function fetchDashboard() {
        try {
            const [accountsRes, loansRes, txRes, summaryRes] = await Promise.all([
                api.get<AccountResponse[]>('/accounts'),
                api.get<LoanResponse[]>('/loans'),
                api.get<TransactionResponse[]>('/transactions/recent'),
                api.get<MonthlySummaryResponse[]>('/transactions/summary'),
            ])
            setAccounts(accountsRes.data)
            setLoans(loansRes.data)
            setRecentTransactions(txRes.data)
            setSummary(summaryRes.data)
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchDashboard()
    }, []);

    const totalBalance = accounts
        .filter(a => a.status === 'ACTIVE')
        .reduce((sum, a) => sum + a.balance, 0)
    const activeLoans = loans.filter(l => l.status === 'ACTIVE')
    const totalOwed = activeLoans.reduce((sum, l) => sum + l.remainingBalance, 0)

    return (
        <PageContainer>
            <h1 className="text-2xl font-bold mb-6">Dashboard</h1>
            {loading ? (
                <p>Loading...</p>
            ) : (
                <div className="space-y-4">
                    <Card className="py-0">
                        <CardContent className="p-5">
                            <div className="flex justify-between items-start">
                                <div>
                                    <p className="text-xs text-muted-foreground mb-1">Total balance</p>
                                    <p className="text-3xl font-semibold tabular-nums">{formatCurrency(totalBalance)}</p>
                                </div>
                                <div className="flex gap-2">
                                    <Button onClick={() => navigate('/transactions')}>
                                        <Plus size={16} /> Deposit
                                    </Button>
                                    <Button variant="outline" onClick={() => navigate('/transactions')}>
                                        <ArrowLeftRight size={16} /> Transfer
                                    </Button>
                                </div>
                            </div>

                            <div className="h-[100px] rounded bg-muted/50 mt-3" />

                            <div className="flex justify-between mt-1.5">
                                <span className="text-[10px] text-muted-foreground">Jul 3</span>
                                <span className="text-[10px] text-muted-foreground">Today</span>
                            </div>
                        </CardContent>
                    </Card>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="bg-muted rounded-[10px] px-4 py-3.5">
                            <p className="text-xs text-muted-foreground">Active loans</p>
                            <p className="text-xl font-semibold mt-0.5">{activeLoans.length}</p>
                        </div>
                        <div className="bg-muted rounded-[10px] px-4 py-3.5">
                            <p className="text-xs text-muted-foreground">Total owed</p>
                            <p className="text-xl font-semibold mt-0.5 tabular-nums">{formatCurrency(totalOwed)}</p>
                        </div>
                    </div>
                    <Card className="py-0">
                        <CardContent className="p-5">
                            <p className="text-xs font-medium text-muted-foreground mb-3">Income vs. spending (last 6 months)</p>
                            {summary.length === 0 ? (
                                <p className="text-sm text-muted-foreground">Not enough activity yet to chart.</p>
                            ) : (
                                <div className="h-[220px]">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={summary.map(s => ({
                                            month: monthLabel(s.month),
                                            Income: s.income,
                                            Spending: s.expense
                                        }))}>
                                            <CartesianGrid strokeDasharray="3 3" vertical={false} className="stroke-muted" />
                                            <XAxis dataKey="month" fontSize={12} tickLine={false} axisLine={false} />
                                            <YAxis fontSize={12} tickLine={false} axisLine={false} width={36} />
                                            <Tooltip formatter={(value: number) => formatCurrency(value)} />
                                            <Legend />
                                            <Bar dataKey="Income" fill="#B45309" radius={[4, 4, 0, 0]} />
                                            <Bar dataKey="Spending" fill="#A1A1AA" radius={[4, 4, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            )}
                        </CardContent>
                    </Card>

                    <div className="grid md:grid-cols-[1.7fr_1fr] gap-4 items-start">
                        <Card className="py-0 gap-0">
                            <p className="text-xs font-medium text-muted-foreground px-4 pt-3.5 pb-2">Recent activity</p>
                            {recentTransactions.length === 0 ? (
                                <p className="text-sm text-muted-foreground px-4 pb-4">No recent activity.</p>
                            ) : (
                                recentTransactions.map((tx) => {
                                    const inbound = isInbound(tx.type)
                                    const Icon = tx.type === 'LOAN_REPAYMENT' ? CreditCard : inbound ? ArrowDownLeft : ArrowUpRight

                                    return (
                                        <div key={tx.id} className="flex items-center gap-2.5 px-4 py-2.5 border-t">
                                            <div className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 ${
                                                inbound ? 'bg-success/10 text-success dark:bg-success/20' : 'bg-destructive/10 text-destructive dark:bg-destructive/20'
                                            }`}>
                                                <Icon size={14} />
                                            </div>
                                            <div className="flex-1">
                                                <p className="text-[13px]">{tx.description || transTypeLabels[tx.type]}</p>
                                                <p className="text-[11px] text-muted-foreground">
                                                    {new Date(tx.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                                                </p>
                                            </div>
                                            <MoneyAmount amount={tx.amount} direction={inbound ? 'in' : 'out'} />
                                        </div>
                                    )
                                })
                            )}
                        </Card>

                        <Card className="py-0 gap-0">
                            <p className="text-xs font-medium text-muted-foreground px-4 pt-3.5 pb-2">Accounts</p>
                            {accounts.filter(a => a.status === 'ACTIVE').map((account) => (
                                <div
                                    key={account.id}
                                    onClick={() => navigate(`/accounts/${account.id}`)}
                                    className="flex items-center gap-2 px-4 py-2.5 border-t cursor-pointer hover:bg-muted/50 transition-colors"
                                >
                                    <div className={`w-2 h-2 rounded-full shrink-0 ${
                                        account.type === 'CHECKING' ? 'bg-[#B45309]' : 'bg-[#27272A] dark:bg-[#A1A1AA]'
                                    }`} />
                                    <p className="text-xs flex-1 truncate">{account.nickname ?? (account.type === 'CHECKING' ? 'Checking' : 'Savings')}</p>
                                    <p className="text-xs font-semibold tabular-nums">{formatCurrency(account.balance, 0)}</p>
                                </div>
                            ))}
                        </Card>
                    </div>
                </div>
            )}
        </PageContainer>
    )
}