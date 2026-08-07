import { useState, type SubmitEvent } from 'react'
import {useAuth} from "@/context/AuthContext.tsx";
import {Link, useNavigate} from "react-router-dom";
import api from "@/api/axios.ts";
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import {Field, FieldDescription, FieldGroup, FieldLabel} from "@/components/ui/field.tsx";
import {Eye, EyeOff, Wallet} from "lucide-react";
import MapleLeafIcon from "@/components/MapleLeafIcon.tsx";

export default function AuthPage() {
    const [mode, setMode] = useState<'login' | 'register'>('login')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [username, setUsername] = useState('')
    const [fullName, setFullName] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)
    const [showPassword, setShowPassword] = useState(false)

    const { login } = useAuth()
    const navigate = useNavigate()

    async function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
        e.preventDefault()
        setError('')
        setLoading(true)

        try {
            const endpoint = mode === 'login' ? '/auth/login' : '/auth/register'
            const body =
                mode === 'login'
                    ? { email, password }
                    : { username, email, password, fullName }

            const response = await api.post(endpoint, body)
            login(response.data.token)
            navigate('/dashboard')
        } catch (err:any) {
            setError(err.response?.data?.message ?? 'Something went wrong')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <Card className="overflow-hidden p-0 w-full max-w-3xl">
                <CardContent className="grid p-0 md:grid-cols-2">
                    <form className="p-6 md:p-8" onSubmit={handleSubmit}>
                            <FieldGroup>
                                <div className="flex flex-col items-center gap-2 text-center">
                                    <h1 className="text-2xl font-bold">{mode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
                                    <p className="text-balance text-muted-foreground">
                                        {mode === 'login' ? 'Sign in to Maple Bank' : 'Sign up for Maple Bank'}
                                    </p>
                                </div>

                                {mode === 'register' && (
                                    <>
                                        <Field>
                                            <FieldLabel htmlFor="username">Username</FieldLabel>
                                            <Input id="username" value={username} onChange={(e) => setUsername(e.target.value)} required />
                                        </Field>
                                        <Field>
                                            <FieldLabel htmlFor="fullName">Full name</FieldLabel>
                                            <Input id="fullName" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
                                        </Field>
                                    </>
                                )}

                                <Field>
                                    <FieldLabel htmlFor="email">Email</FieldLabel>
                                    <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                                </Field>

                                <Field>
                                    <div className="flex items-center justify-between">
                                        <FieldLabel htmlFor="password">Password</FieldLabel>
                                        {mode === 'login' && (
                                            <Link to="/forgot-password" className="text-sm text-muted-foreground underline">
                                                Forgot password?
                                            </Link>
                                        )}
                                    </div>
                                    <div className="relative">
                                        <Input
                                            id="password"
                                            type={showPassword ? 'text' : 'password'}
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            className="pr-9"
                                            required
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPassword(!showPassword)}
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                                        >
                                            {showPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                                        </button>
                                    </div>
                                </Field>

                                {error && <p className="text-sm text-destructive text-center">{error}</p>}

                                <Field>
                                    <Button type="submit" disabled={loading}>
                                        {loading ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Create account'}
                                    </Button>
                                </Field>

                                <FieldDescription className="text-center">
                                    {mode === 'login' ? 'No account? ' : 'Already have an account? '}
                                    <button type="button" onClick={() => setMode(mode === 'login' ? 'register' : 'login')} className="underline">
                                        {mode === 'login' ? 'Create one' : 'Sign in'}
                                    </button>
                                </FieldDescription>
                            </FieldGroup>
                    </form>
                    <div className="relative hidden md:flex h-full items-center justify-center overflow-hidden bg-[#B45309] p-8">
                        <div className="absolute w-[280px] h-[280px] rounded-full bg-[#9C4508] -top-[140px] -right-[120px]" />
                        <div className="absolute w-[200px] h-[200px] rounded-full bg-[#8B3D07] -bottom-[110px] -left-[70px]" />

                        <div className="absolute top-7 left-7 flex items-center gap-2 z-10">
                            <MapleLeafIcon size={20} className="text-white" />
                            <div className="text-white text-sm font-semibold">Maple Bank</div>
                        </div>

                        <div className="relative bg-[#D97706] rounded-[20px] p-5 w-[240px] h-[140px] box-border flex flex-col justify-between z-10 border border-white/10 shadow-lg shadow-black/30">
                            <div className="flex justify-between items-start">
                                <div>
                                    <div className="text-white text-sm font-semibold">Everyday</div>
                                    <div className="text-[#FDE68A] text-xs mt-0.5">Checking</div>
                                </div>
                                <Wallet size={18} className="text-[#FDE68A]" />
                            </div>
                            <div className="flex justify-between items-end">
                                <div className="text-[#FDE68A] text-xs">•••• 4821</div>
                                <div className="text-white text-base font-semibold tabular-nums">$4,238.12</div>
                            </div>
                        </div>

                        <div className="absolute bottom-7 left-7 right-7 text-[#FEF3C7] text-sm leading-relaxed z-10">
                            Proudly Canadian banking, coast to coast.
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    )
}