import {useEffect, useState} from "react";
import type {ComplianceFlagResponse} from "@/types";
import api from "@/api/axios.ts";
import PageContainer from "@/components/PageContainer.tsx";
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "@/components/ui/table.tsx";
import {Badge} from "@/components/ui/badge.tsx";
import {Button} from "@/components/ui/button.tsx";
import {formatCurrency, transTypeLabels} from "@/components/MoneyAmount.tsx";
import {toast} from "sonner";

export default function AdminCompliancePage() {
    const [ flags, setFlags ] = useState<ComplianceFlagResponse[]>([])
    const [ loading, setLoading ] = useState(true)
    const [ submittingId, setSubmittingId ] = useState<number | null>(null)

    async function fetchFlags() {
        try {
            const response = await api.get<ComplianceFlagResponse[]>('/compliance/flags')
            setFlags(response.data)
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    async function handleReview(id: number) {
        setSubmittingId(id)
        try {
            await api.put(`/compliance/flags/${id}/review`)
            await fetchFlags()
            toast.success('Marked as reviewed')
        } catch (err: any) {
            toast.error(err.response?.data?.message ?? 'Failed to update flag')
        } finally {
            setSubmittingId(null)
        }
    }

    useEffect(() => {
        fetchFlags()
    }, []);

    const pending = flags.filter(f => !f.reviewed)
    const reviewed = flags.filter(f => f.reviewed)

    return (
        <PageContainer>
            <h1 className="text-2xl font-bold mb-2">Compliance Review</h1>
            <p className="text-sm text-muted-foreground mb-6">
                Transactions of $10,000 or more are automatically flagged for review, mirroring FINTRAC's
                large transaction reporting requirement for Canadian financial institutions.
            </p>

            {loading ? (
                <p>Loading...</p>
            ) : flags.length === 0 ? (
                <p className="text-muted-foreground">No large transactions have been flagged yet.</p>
            ) : (
                <>
                    <h2 className="text-lg font-semibold mb-3">Pending review ({pending.length})</h2>
                    {pending.length === 0 ? (
                        <p className="text-sm text-muted-foreground mb-6">Nothing pending — all clear.</p>
                    ) : (
                        <Table className="mb-8">
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Account owner</TableHead>
                                    <TableHead>Account</TableHead>
                                    <TableHead>Type</TableHead>
                                    <TableHead className="text-right">Amount</TableHead>
                                    <TableHead>Date</TableHead>
                                    <TableHead></TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {pending.map((flag) => (
                                    <TableRow key={flag.id}>
                                        <TableCell className="font-medium">{flag.accountOwnerName}</TableCell>
                                        <TableCell>{flag.accountNumber}</TableCell>
                                        <TableCell>{transTypeLabels[flag.transactionType]}</TableCell>
                                        <TableCell className="text-right">{formatCurrency(flag.amount)}</TableCell>
                                        <TableCell>{new Date(flag.createdAt).toLocaleDateString()}</TableCell>
                                        <TableCell>
                                            <Button
                                                size="sm"
                                                disabled={submittingId === flag.id}
                                                onClick={() => handleReview(flag.id)}
                                            >
                                                {submittingId === flag.id ? 'Saving...' : 'Mark reviewed'}
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    )}

                    {reviewed.length > 0 && (
                        <>
                            <h2 className="text-lg font-semibold mb-3">Reviewed ({reviewed.length})</h2>
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Account owner</TableHead>
                                        <TableHead>Type</TableHead>
                                        <TableHead className="text-right">Amount</TableHead>
                                        <TableHead>Reviewed by</TableHead>
                                        <TableHead>Status</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {reviewed.map((flag) => (
                                        <TableRow key={flag.id}>
                                            <TableCell className="font-medium">{flag.accountOwnerName}</TableCell>
                                            <TableCell>{transTypeLabels[flag.transactionType]}</TableCell>
                                            <TableCell className="text-right">{formatCurrency(flag.amount)}</TableCell>
                                            <TableCell>{flag.reviewedByUsername}</TableCell>
                                            <TableCell><Badge variant="secondary">Reviewed</Badge></TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </>
                    )}
                </>
            )}
        </PageContainer>
    )
}
