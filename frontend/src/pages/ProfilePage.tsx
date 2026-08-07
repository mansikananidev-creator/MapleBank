import {useEffect, useState} from "react";
import type {UserProfileResponse} from "@/types";
import api from "@/api/axios.ts";
import PageContainer from "@/components/PageContainer.tsx";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card.tsx";
import {Label} from "@/components/ui/label.tsx";
import {Input} from "@/components/ui/input.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.tsx";
import {toast} from "sonner";

const PROVINCES = [
    { value: "AB", label: "Alberta" },
    { value: "BC", label: "British Columbia" },
    { value: "MB", label: "Manitoba" },
    { value: "NB", label: "New Brunswick" },
    { value: "NL", label: "Newfoundland and Labrador" },
    { value: "NS", label: "Nova Scotia" },
    { value: "NT", label: "Northwest Territories" },
    { value: "NU", label: "Nunavut" },
    { value: "ON", label: "Ontario" },
    { value: "PE", label: "Prince Edward Island" },
    { value: "QC", label: "Quebec" },
    { value: "SK", label: "Saskatchewan" },
    { value: "YT", label: "Yukon" },
]

export default function ProfilePage() {
    const [profile, setProfile] = useState<UserProfileResponse | null>(null)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [profileError, setProfileError] = useState('')

    const [fullName, setFullName] = useState('')
    const [phoneNumber, setPhoneNumber] = useState('')
    const [dateOfBirth, setDateOfBirth] = useState('')
    const [addressLine1, setAddressLine1] = useState('')
    const [addressLine2, setAddressLine2] = useState('')
    const [city, setCity] = useState('')
    const [province, setProvince] = useState('')
    const [postalCode, setPostalCode] = useState('')

    const [currentPassword, setCurrentPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [confirmNewPassword, setConfirmNewPassword] = useState('')
    const [passwordError, setPasswordError] = useState('')
    const [changingPassword, setChangingPassword] = useState(false)

    async function fetchProfile() {
        try {
            const response = await api.get<UserProfileResponse>('/profile')
            const data = response.data
            setProfile(data)
            setFullName(data.fullName ?? '')
            setPhoneNumber(data.phoneNumber ?? '')
            setDateOfBirth(data.dateOfBirth ?? '')
            setAddressLine1(data.addressLine1 ?? '')
            setAddressLine2(data.addressLine2 ?? '')
            setCity(data.city ?? '')
            setProvince(data.province ?? '')
            setPostalCode(data.postalCode ?? '')
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchProfile()
    }, [])

    async function handleSaveProfile() {
        setSaving(true)
        setProfileError('')
        try {
            const body = {
                fullName,
                phoneNumber: phoneNumber || null,
                dateOfBirth: dateOfBirth || null,
                addressLine1: addressLine1 || null,
                addressLine2: addressLine2 || null,
                city: city || null,
                province: province || null,
                postalCode: postalCode || null,
            }
            const response = await api.put<UserProfileResponse>('/profile', body)
            setProfile(response.data)
            toast.success('Profile updated')
        } catch (err: any) {
            setProfileError(err.response?.data?.message ?? 'Failed to update profile')
        } finally {
            setSaving(false)
        }
    }

    async function handleChangePassword() {
        setPasswordError('')

        if (newPassword.length < 8) {
            setPasswordError('New password must be at least 8 characters')
            return
        }
        if (newPassword !== confirmNewPassword) {
            setPasswordError("New passwords don't match")
            return
        }

        setChangingPassword(true)
        try {
            await api.put('/profile/change-password', { currentPassword, newPassword })
            setCurrentPassword('')
            setNewPassword('')
            setConfirmNewPassword('')
            toast.success('Password changed')
        } catch (err: any) {
            setPasswordError(err.response?.data?.message ?? 'Failed to change password')
        } finally {
            setChangingPassword(false)
        }
    }

    if (loading) {
        return (
            <PageContainer>
                <p>Loading...</p>
            </PageContainer>
        )
    }

    return (
        <PageContainer>
            <h1 className="text-2xl font-bold mb-6">Profile</h1>

            <div className="grid gap-6 max-w-xl">
                <Card>
                    <CardHeader>
                        <CardTitle>Account</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-1 text-sm text-muted-foreground">
                        <p>Username: <span className="text-foreground">{profile?.username}</span></p>
                        <p>Email: <span className="text-foreground">{profile?.email}</span></p>
                        <p>Role: <span className="text-foreground">{profile?.role}</span></p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>Personal information</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="full-name">Full name</Label>
                            <Input id="full-name" value={fullName} onChange={(e) => setFullName(e.target.value)} />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="phone">Phone number</Label>
                            <Input
                                id="phone"
                                placeholder="e.g. 416-555-0123"
                                value={phoneNumber}
                                onChange={(e) => setPhoneNumber(e.target.value)}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="dob">Date of birth</Label>
                            <Input
                                id="dob"
                                type="date"
                                value={dateOfBirth}
                                onChange={(e) => setDateOfBirth(e.target.value)}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="address1">Address line 1</Label>
                            <Input
                                id="address1"
                                placeholder="Street address"
                                value={addressLine1}
                                onChange={(e) => setAddressLine1(e.target.value)}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="address2">Address line 2 (optional)</Label>
                            <Input
                                id="address2"
                                placeholder="Apartment, suite, etc."
                                value={addressLine2}
                                onChange={(e) => setAddressLine2(e.target.value)}
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="city">City</Label>
                                <Input id="city" value={city} onChange={(e) => setCity(e.target.value)} />
                            </div>

                            <div className="space-y-2">
                                <Label>Province</Label>
                                <Select value={province} onValueChange={setProvince}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {PROVINCES.map((p) => (
                                            <SelectItem key={p.value} value={p.value}>{p.label}</SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="postal-code">Postal code</Label>
                            <Input
                                id="postal-code"
                                placeholder="e.g. K1A 0B1"
                                value={postalCode}
                                onChange={(e) => setPostalCode(e.target.value)}
                            />
                        </div>

                        {profileError && <p className="text-sm text-destructive">{profileError}</p>}

                        <Button onClick={handleSaveProfile} disabled={saving}>
                            {saving ? 'Saving...' : 'Save changes'}
                        </Button>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>Change password</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="current-password">Current password</Label>
                            <Input
                                id="current-password"
                                type="password"
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="new-password">New password</Label>
                            <Input
                                id="new-password"
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="confirm-new-password">Confirm new password</Label>
                            <Input
                                id="confirm-new-password"
                                type="password"
                                value={confirmNewPassword}
                                onChange={(e) => setConfirmNewPassword(e.target.value)}
                            />
                        </div>

                        {passwordError && <p className="text-sm text-destructive">{passwordError}</p>}

                        <Button onClick={handleChangePassword} disabled={changingPassword}>
                            {changingPassword ? 'Updating...' : 'Change password'}
                        </Button>
                    </CardContent>
                </Card>
            </div>
        </PageContainer>
    )
}
