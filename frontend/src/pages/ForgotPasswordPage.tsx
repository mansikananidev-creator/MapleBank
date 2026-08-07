import { useState, type SubmitEvent } from 'react'
import { Link } from 'react-router-dom'
import api from '@/api/axios.ts'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import { Field, FieldDescription, FieldGroup, FieldLabel } from '@/components/ui/field.tsx'
import MapleLeafIcon from '@/components/MapleLeafIcon.tsx'

export default function ForgotPasswordPage() {
    const [email, setEmail] = useState('')
    const [loading, setLoading] = useState(false)
    const [submitted, setSubmitted] = useState(false)

    async function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
        e.preventDefault()
        setLoading(true)
        try {
            await api.post('/auth/forgot-password', { email })
        } catch {
            // Intentionally ignored: we show the same message either way so this
            // endpoint can't be used to figure out which emails are registered.
        } finally {
            setLoading(false)
            setSubmitted(true)
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <Card className="overflow-hidden p-0 w-full max-w-md">
                <CardContent className="p-6 md:p-8">
                    <div className="flex flex-col items-center gap-2 text-center mb-4">
                        <MapleLeafIcon size={28} className="text-primary" />
                        <h1 className="text-2xl font-bold">Reset your password</h1>
                        <p className="text-balance text-muted-foreground">
                            Enter the email on your Maple Bank account and we'll send you a link to reset your password.
                        </p>
                    </div>

                    {submitted ? (
                        <FieldGroup>
                            <p className="text-sm text-center">
                                If an account exists for <span className="font-medium">{email}</span>, a password reset
                                link is on its way. Check your inbox (and spam folder) — the link expires in 30 minutes.
                            </p>
                            <FieldDescription className="text-center">
                                <Link to="/login" className="underline">Back to sign in</Link>
                            </FieldDescription>
                        </FieldGroup>
                    ) : (
                        <form onSubmit={handleSubmit}>
                            <FieldGroup>
                                <Field>
                                    <FieldLabel htmlFor="email">Email</FieldLabel>
                                    <Input
                                        id="email"
                                        type="email"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                    />
                                </Field>

                                <Field>
                                    <Button type="submit" disabled={loading}>
                                        {loading ? 'Sending...' : 'Send reset link'}
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
