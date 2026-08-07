import { useState, type SubmitEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import api from '@/api/axios.ts'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import { Field, FieldDescription, FieldGroup, FieldLabel } from '@/components/ui/field.tsx'
import MapleLeafIcon from '@/components/MapleLeafIcon.tsx'
import { toast } from 'sonner'

export default function ResetPasswordPage() {
    const [searchParams] = useSearchParams()
    const token = searchParams.get('token') ?? ''
    const navigate = useNavigate()

    const [newPassword, setNewPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    async function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
        e.preventDefault()
        setError('')

        if (newPassword.length < 8) {
            setError('Password must be at least 8 characters')
            return
        }
        if (newPassword !== confirmPassword) {
            setError("Passwords don't match")
            return
        }

        setLoading(true)
        try {
            await api.post('/auth/reset-password', { token, newPassword })
            toast.success('Password updated — sign in with your new password')
            navigate('/login')
        } catch (err: any) {
            setError(err.response?.data?.message ?? 'This reset link is invalid or has expired')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <Card className="overflow-hidden p-0 w-full max-w-md">
                <CardContent className="p-6 md:p-8">
                    <div className="flex flex-col items-center gap-2 text-center mb-4">
                        <MapleLeafIcon size={28} className="text-primary" />
                        <h1 className="text-2xl font-bold">Choose a new password</h1>
                    </div>

                    {!token ? (
                        <p className="text-sm text-destructive text-center">
                            This link is missing its reset token. Please use the link from your email, or{' '}
                            <Link to="/forgot-password" className="underline">request a new one</Link>.
                        </p>
                    ) : (
                        <form onSubmit={handleSubmit}>
                            <FieldGroup>
                                <Field>
                                    <FieldLabel htmlFor="new-password">New password</FieldLabel>
                                    <Input
                                        id="new-password"
                                        type="password"
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                        required
                                    />
                                </Field>

                                <Field>
                                    <FieldLabel htmlFor="confirm-password">Confirm password</FieldLabel>
                                    <Input
                                        id="confirm-password"
                                        type="password"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        required
                                    />
                                </Field>

                                {error && <p className="text-sm text-destructive">{error}</p>}

                                <Field>
                                    <Button type="submit" disabled={loading}>
                                        {loading ? 'Updating...' : 'Update password'}
                                    </Button>
                                </Field>

                                <FieldDescription className="text-center">
                                    <Link to="/login" className="underline">Back to sign in</Link>
                                </FieldDescription>
                            </FieldGroup>
                        </form>
                    )}
                </CardContent>
            </Card>
        </div>
    )
}
